/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2021
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Gryzlov <nixel2007@gmail.com>
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 *
 * SonarQube 1C (BSL) Community Plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * SonarQube 1C (BSL) Community Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with SonarQube 1C (BSL) Community Plugin.
 */
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.utils.Ranges;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.parser.SDBLLexer;
import com.github._1c_syntax.bsl.parser.Tokenizer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.Token;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class BSLHighlighter {

  private static final Set<Integer> BSL_KEYWORDS = createBslKeywords();
  private static final Set<Integer> BSL_SEPARATORS = createBslSeparators();
  private static final Set<Integer> BSL_LITERALS = createBslLiterals();
  private static final Set<Integer> BSL_STRINGS = createBslStrings();
  private static final Set<Integer> BSL_COMMENTS = createBslComments();
  private static final Set<Integer> BSL_PREPROCESSOR = createBslPreprocessor();
  private static final Set<Integer> BSL_ANNOTATIONS = createBslAnnotations();

  private static final Set<Integer> SDBL_KEYWORDS = createSdblKeywords();
  private static final Set<Integer> SDBL_FUNCTIONS = createSdblFunctions();
  private static final Set<Integer> SDBL_METADATA_TYPES = createSdblMetadataTypes();
  private static final Set<Integer> SDBL_VIRTUAL_TABLES = createSdblVirtualTables();
  private static final Set<Integer> SDBL_LITERALS = createSdblLiterals();
  private static final Set<Integer> SDBL_SEPARATORS = createSdblSeparators();
  private static final Set<Integer> SDBL_STRINGS = createSdblStrings();
  private static final Set<Integer> SDBL_COMMENTS = createSdblComments();
  private static final Set<Integer> SDBL_PARAMETERS = createSdblParameters();

  private final SensorContext context;

  public void saveHighlighting(InputFile inputFile, DocumentContext documentContext) {
    Set<HighlightingData> highlightingData = new HashSet<>(documentContext.getTokens().size());

    // populate bsl highlight data
    documentContext.getTokens().forEach(token ->
      highlightToken(token, highlightingData, getTypeOfTextBSL(token.getType()))
    );

    // compute and populate sdbl highlight data
    Map<Integer, List<Token>> queryTokens = documentContext.getQueries().stream()
      .map(Tokenizer::getTokens)
      .flatMap(Collection::stream)
      .collect(Collectors.groupingBy(Token::getLine));
    Map<Integer, Set<HighlightingData>> highlightingDataSDBL = new HashMap<>(queryTokens.size());

    queryTokens.values().stream()
      .flatMap(Collection::stream)
      .forEach(token -> highlightToken(
        token,
        highlightingDataSDBL.computeIfAbsent(token.getLine(), BSLHighlighter::newHashSet),
        getTypeOfTextSDBL(token.getType()))
      );

    // find bsl strings to check overlap with sdbl tokens
    Set<HighlightingData> strings = highlightingData.stream()
      .filter(data -> data.getType() == TypeOfText.STRING)
      .collect(Collectors.toSet());

    strings.forEach((HighlightingData string) -> {
      Range stringRange = string.getRange();

      // find overlapping tokens
      Set<HighlightingData> dataOfCurrentLine = highlightingDataSDBL.get(stringRange.getStart().getLine());
      if (Objects.isNull(dataOfCurrentLine)) {
        return;
      }

      List<HighlightingData> currentTokens = dataOfCurrentLine.stream()
        .filter(sdblData -> Ranges.containsRange(stringRange, sdblData.getRange()))
        .sorted(Comparator.comparing(data -> data.getRange().getStart().getCharacter()))
        .collect(Collectors.toList());

      if (currentTokens.isEmpty()) {
        return;
      }

      // disable current bsl token
      string.setActive(false);

      // split current bsl token to parts excluding sdbl tokens
      Position start = stringRange.getStart();
      int line = start.getLine();
      int startChar;
      int endChar = start.getCharacter();
      for (HighlightingData currentToken : currentTokens) {
        startChar = endChar;
        endChar = currentToken.getRange().getStart().getCharacter();
        TypeOfText typeOfText = string.getType();

        if (startChar < endChar) {
          // add string part
          highlightingData.add(new HighlightingData(
            line,
            startChar,
            endChar,
            typeOfText
          ));
        }

        endChar = currentToken.getRange().getEnd().getCharacter();
      }

      // add final string part
      startChar = endChar;
      endChar = string.getRange().getEnd().getCharacter();
      TypeOfText typeOfText = string.getType();

      if (startChar < endChar) {
        highlightingData.add(new HighlightingData(
          line,
          startChar,
          endChar,
          typeOfText
        ));
      }
    });

    // merge collected bsl tokens with sdbl tokens
    highlightingDataSDBL.values().forEach(highlightingData::addAll);

    if (highlightingData.stream()
            .filter(HighlightingData::isActive)
            .findAny()
            .isEmpty()) {
      return;
    }

    // save only active tokens
    NewHighlighting highlighting = context.newHighlighting().onFile(inputFile);

    highlightingData.stream()
      .filter(HighlightingData::isActive)
      .forEach(data ->
        highlighting.highlight(
          data.getRange().getStart().getLine(),
          data.getRange().getStart().getCharacter(),
          data.getRange().getEnd().getLine(),
          data.getRange().getEnd().getCharacter(),
          data.getType()
        )
      );

    highlighting.save();
  }

  public void highlightToken(
    Token token,
    Collection<HighlightingData> highlightingData,
    @Nullable TypeOfText typeOfText
  ) {
    if (typeOfText == null) {
      return;
    }

    int line = token.getLine();
    int charPositionInLine = token.getCharPositionInLine();
    String tokenText = token.getText();
    // todo: remove with bsl-parser 0.20.0
    var newLineIndex = tokenText.indexOf('\n');

    int tokenLength;
    if (newLineIndex >= 0) {
      tokenLength = newLineIndex - 1;
    } else {
      tokenLength = tokenText.length();
    }

    var range = Ranges.create(
      line,
      charPositionInLine,
      line,
      charPositionInLine + tokenLength
    );

    var data = new HighlightingData(
      range,
      typeOfText
    );

    highlightingData.add(data);
  }

  @Nullable
  private static TypeOfText getTypeOfTextBSL(int tokenType) {
    TypeOfText typeOfText;

    if (BSL_KEYWORDS.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD;
    } else if (BSL_SEPARATORS.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD_LIGHT;
    } else if (BSL_LITERALS.contains(tokenType)) {
      typeOfText = TypeOfText.CONSTANT;
    } else if (BSL_STRINGS.contains(tokenType)) {
      typeOfText = TypeOfText.STRING;
    } else if (BSL_COMMENTS.contains(tokenType)) {
      typeOfText = TypeOfText.COMMENT;
    } else if (BSL_PREPROCESSOR.contains(tokenType)) {
      typeOfText = TypeOfText.PREPROCESS_DIRECTIVE;
    } else if (BSL_ANNOTATIONS.contains(tokenType)) {
      typeOfText = TypeOfText.ANNOTATION;
    } else {
      typeOfText = null;
    }

    return typeOfText;
  }

  @Nullable
  private static TypeOfText getTypeOfTextSDBL(int tokenType) {
    TypeOfText typeOfText;

    if (SDBL_KEYWORDS.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD;
    } else if (SDBL_FUNCTIONS.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD_LIGHT;
    } else if (SDBL_METADATA_TYPES.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD_LIGHT;
    } else if (SDBL_VIRTUAL_TABLES.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD_LIGHT;
    } else if (SDBL_LITERALS.contains(tokenType)) {
      typeOfText = TypeOfText.CONSTANT;
    } else if (SDBL_SEPARATORS.contains(tokenType)) {
      typeOfText = TypeOfText.KEYWORD_LIGHT;
    } else if (SDBL_STRINGS.contains(tokenType)) {
      typeOfText = TypeOfText.STRING;
    } else if (SDBL_COMMENTS.contains(tokenType)) {
      typeOfText = TypeOfText.COMMENT;
    } else if (SDBL_PARAMETERS.contains(tokenType)) {
      typeOfText = TypeOfText.ANNOTATION;
    } else {
      typeOfText = null;
    }

    return typeOfText;
  }

  private static Set<HighlightingData> newHashSet(Integer line) {
    return new HashSet<>();
  }

  private static Set<Integer> createBslAnnotations() {
    return Set.of(
      BSLLexer.AMPERSAND,
      BSLLexer.ANNOTATION_AFTER_SYMBOL,
      BSLLexer.ANNOTATION_AROUND_SYMBOL,
      BSLLexer.ANNOTATION_ATCLIENT_SYMBOL,
      BSLLexer.ANNOTATION_ATCLIENTATSERVER_SYMBOL,
      BSLLexer.ANNOTATION_ATCLIENTATSERVERNOCONTEXT_SYMBOL,
      BSLLexer.ANNOTATION_ATSERVER_SYMBOL,
      BSLLexer.ANNOTATION_ATSERVERNOCONTEXT_SYMBOL,
      BSLLexer.ANNOTATION_BEFORE_SYMBOL,
      BSLLexer.ANNOTATION_CHANGEANDVALIDATE_SYMBOL,
      BSLLexer.ANNOTATION_CUSTOM_SYMBOL,
      BSLLexer.ANNOTATION_UNKNOWN
    );
  }

  private static Set<Integer> createBslPreprocessor() {
    return Set.of(
      BSLLexer.HASH,
      BSLLexer.PREPROC_USE_KEYWORD,
      BSLLexer.PREPROC_REGION,
      BSLLexer.PREPROC_END_REGION,
      BSLLexer.PREPROC_AND_KEYWORD,
      BSLLexer.PREPROC_OR_KEYWORD,
      BSLLexer.PREPROC_NOT_KEYWORD,
      BSLLexer.PREPROC_IF_KEYWORD,
      BSLLexer.PREPROC_THEN_KEYWORD,
      BSLLexer.PREPROC_ELSIF_KEYWORD,
      BSLLexer.PREPROC_ELSE_KEYWORD,
      BSLLexer.PREPROC_ENDIF_KEYWORD,
      BSLLexer.PREPROC_EXCLAMATION_MARK,
      BSLLexer.PREPROC_LPAREN,
      BSLLexer.PREPROC_RPAREN,
      BSLLexer.PREPROC_MOBILEAPPCLIENT_SYMBOL,
      BSLLexer.PREPROC_MOBILEAPPSERVER_SYMBOL,
      BSLLexer.PREPROC_MOBILECLIENT_SYMBOL,
      BSLLexer.PREPROC_THICKCLIENTORDINARYAPPLICATION_SYMBOL,
      BSLLexer.PREPROC_THICKCLIENTMANAGEDAPPLICATION_SYMBOL,
      BSLLexer.PREPROC_EXTERNALCONNECTION_SYMBOL,
      BSLLexer.PREPROC_THINCLIENT_SYMBOL,
      BSLLexer.PREPROC_WEBCLIENT_SYMBOL,
      BSLLexer.PREPROC_ATCLIENT_SYMBOL,
      BSLLexer.PREPROC_CLIENT_SYMBOL,
      BSLLexer.PREPROC_ATSERVER_SYMBOL,
      BSLLexer.PREPROC_SERVER_SYMBOL,
      BSLLexer.PREPROC_INSERT,
      BSLLexer.PREPROC_ENDINSERT,
      BSLLexer.PREPROC_DELETE,
      BSLLexer.PREPROC_DELETE_ANY,
      BSLLexer.PREPROC_ENDDELETE,
      BSLLexer.PREPROC_IDENTIFIER,
      BSLLexer.PREPROC_LINUX,
      BSLLexer.PREPROC_WINDOWS,
      BSLLexer.PREPROC_MACOS,
      BSLLexer.PREPROC_ANY
    );
  }

  private static Set<Integer> createBslComments() {
    return Set.of(
      BSLLexer.LINE_COMMENT,
      BSLLexer.PREPROC_LINE_COMMENT
    );
  }

  private static Set<Integer> createBslStrings() {
    return Set.of(
      BSLLexer.STRING,
      BSLLexer.STRINGSTART,
      BSLLexer.STRINGPART,
      BSLLexer.STRINGTAIL,
      BSLLexer.PREPROC_STRING
    );
  }

  private static Set<Integer> createBslLiterals() {
    return Set.of(
      BSLLexer.TRUE,
      BSLLexer.FALSE,
      BSLLexer.UNDEFINED,
      BSLLexer.NULL,
      BSLLexer.DATETIME,
      BSLLexer.DECIMAL,
      BSLLexer.FLOAT
    );
  }

  private static Set<Integer> createBslSeparators() {
    return Set.of(
      BSLLexer.SEMICOLON,
      BSLLexer.QUESTION,
      BSLLexer.PLUS,
      BSLLexer.MINUS,
      BSLLexer.MUL,
      BSLLexer.QUOTIENT,
      BSLLexer.MODULO,
      BSLLexer.ASSIGN,
      BSLLexer.LESS_OR_EQUAL,
      BSLLexer.LESS,
      BSLLexer.NOT_EQUAL,
      BSLLexer.GREATER_OR_EQUAL,
      BSLLexer.GREATER,
      BSLLexer.COMMA,
      BSLLexer.COLON,
      BSLLexer.TILDA
    );
  }

  private static Set<Integer> createBslKeywords() {
    return Set.of(
      BSLLexer.PROCEDURE_KEYWORD,
      BSLLexer.FUNCTION_KEYWORD,
      BSLLexer.ENDPROCEDURE_KEYWORD,
      BSLLexer.ENDFUNCTION_KEYWORD,
      BSLLexer.EXPORT_KEYWORD,
      BSLLexer.VAL_KEYWORD,
      BSLLexer.ENDIF_KEYWORD,
      BSLLexer.ENDDO_KEYWORD,
      BSLLexer.IF_KEYWORD,
      BSLLexer.ELSIF_KEYWORD,
      BSLLexer.ELSE_KEYWORD,
      BSLLexer.THEN_KEYWORD,
      BSLLexer.WHILE_KEYWORD,
      BSLLexer.DO_KEYWORD,
      BSLLexer.FOR_KEYWORD,
      BSLLexer.TO_KEYWORD,
      BSLLexer.EACH_KEYWORD,
      BSLLexer.IN_KEYWORD,
      BSLLexer.TRY_KEYWORD,
      BSLLexer.EXCEPT_KEYWORD,
      BSLLexer.ENDTRY_KEYWORD,
      BSLLexer.RETURN_KEYWORD,
      BSLLexer.CONTINUE_KEYWORD,
      BSLLexer.RAISE_KEYWORD,
      BSLLexer.VAR_KEYWORD,
      BSLLexer.NOT_KEYWORD,
      BSLLexer.OR_KEYWORD,
      BSLLexer.AND_KEYWORD,
      BSLLexer.NEW_KEYWORD,
      BSLLexer.GOTO_KEYWORD,
      BSLLexer.BREAK_KEYWORD,
      BSLLexer.EXECUTE_KEYWORD,
      BSLLexer.ADDHANDLER_KEYWORD,
      BSLLexer.REMOVEHANDLER_KEYWORD,
      BSLLexer.ASYNC_KEYWORD,
      BSLLexer.WAIT_KEYWORD
    );
  }

  private static Set<Integer> createSdblSeparators() {
    return Set.of(
      SDBLLexer.SEMICOLON,
      SDBLLexer.PLUS,
      SDBLLexer.MINUS,
      SDBLLexer.MUL,
      SDBLLexer.QUOTIENT,
      SDBLLexer.ASSIGN,
      SDBLLexer.LESS_OR_EQUAL,
      SDBLLexer.LESS,
      SDBLLexer.NOT_EQUAL,
      SDBLLexer.GREATER_OR_EQUAL,
      SDBLLexer.GREATER,
      SDBLLexer.COMMA,
      SDBLLexer.BRACE,
      SDBLLexer.BRACE_START
    );
  }

  private static Set<Integer> createSdblLiterals() {
    return Set.of(
      SDBLLexer.TRUE,
      SDBLLexer.FALSE,
      SDBLLexer.UNDEFINED,
      SDBLLexer.NULL,
      SDBLLexer.DECIMAL,
      SDBLLexer.FLOAT
    );
  }

  private static Set<Integer> createSdblVirtualTables() {
    return Set.of(
      SDBLLexer.ACTUAL_ACTION_PERIOD_VT,
      SDBLLexer.BALANCE_VT,
      SDBLLexer.BALANCE_AND_TURNOVERS_VT,
      SDBLLexer.BOUNDARIES_VT,
      SDBLLexer.DR_CR_TURNOVERS_VT,
      SDBLLexer.EXT_DIMENSIONS_VT,
      SDBLLexer.RECORDS_WITH_EXT_DIMENSIONS_VT,
      SDBLLexer.SCHEDULE_DATA_VT,
      SDBLLexer.SLICEFIRST_VT,
      SDBLLexer.SLICELAST_VT,
      SDBLLexer.TASK_BY_PERFORMER_VT,
      SDBLLexer.TURNOVERS_VT
    );
  }

  private static Set<Integer> createSdblMetadataTypes() {
    return Set.of(
      SDBLLexer.ACCOUNTING_REGISTER_TYPE,
      SDBLLexer.ACCUMULATION_REGISTER_TYPE,
      SDBLLexer.BUSINESS_PROCESS_TYPE,
      SDBLLexer.CALCULATION_REGISTER_TYPE,
      SDBLLexer.CATALOG_TYPE,
      SDBLLexer.CHART_OF_ACCOUNTS_TYPE,
      SDBLLexer.CHART_OF_CALCULATION_TYPES_TYPE,
      SDBLLexer.CHART_OF_CHARACTERISTIC_TYPES_TYPE,
      SDBLLexer.CONSTANT_TYPE,
      SDBLLexer.DOCUMENT_TYPE,
      SDBLLexer.DOCUMENT_JOURNAL_TYPE,
      SDBLLexer.ENUM_TYPE,
      SDBLLexer.EXCHANGE_PLAN_TYPE,
      SDBLLexer.EXTERNAL_DATA_SOURCE_TYPE,
      SDBLLexer.FILTER_CRITERION_TYPE,
      SDBLLexer.INFORMATION_REGISTER_TYPE,
      SDBLLexer.SEQUENCE_TYPE,
      SDBLLexer.TASK_TYPE
    );
  }

  private static Set<Integer> createSdblFunctions() {
    return Set.of(
      SDBLLexer.AVG,
      SDBLLexer.BEGINOFPERIOD,
      SDBLLexer.BOOLEAN,
      SDBLLexer.COUNT,
      SDBLLexer.DATE,
      SDBLLexer.DATEADD,
      SDBLLexer.DATEDIFF,
      SDBLLexer.DATETIME,
      SDBLLexer.DAY,
      SDBLLexer.DAYOFYEAR,
      SDBLLexer.EMPTYTABLE,
      SDBLLexer.ENDOFPERIOD,
      SDBLLexer.HALFYEAR,
      SDBLLexer.HOUR,
      SDBLLexer.MAX,
      SDBLLexer.MIN,
      SDBLLexer.MINUTE,
      SDBLLexer.MONTH,
      SDBLLexer.NUMBER,
      SDBLLexer.QUARTER,
      SDBLLexer.PRESENTATION,
      SDBLLexer.RECORDAUTONUMBER,
      SDBLLexer.REFPRESENTATION,
      SDBLLexer.SECOND,
      SDBLLexer.STRING,
      SDBLLexer.SUBSTRING,
      SDBLLexer.SUM,
      SDBLLexer.TENDAYS,
      SDBLLexer.TYPE,
      SDBLLexer.VALUE,
      SDBLLexer.VALUETYPE,
      SDBLLexer.WEEK,
      SDBLLexer.WEEKDAY,
      SDBLLexer.YEAR
    );
  }

  private static Set<Integer> createSdblKeywords() {
    return Set.of(
      SDBLLexer.ALLOWED,
      SDBLLexer.AND,
      SDBLLexer.AS,
      SDBLLexer.ASC,
      SDBLLexer.AUTOORDER,
      SDBLLexer.BETWEEN,
      SDBLLexer.BY_EN,
      SDBLLexer.CASE,
      SDBLLexer.CAST,
      SDBLLexer.DESC,
      SDBLLexer.DISTINCT,
      SDBLLexer.DROP,
      SDBLLexer.ELSE,
      SDBLLexer.END,
      SDBLLexer.ESCAPE,
      SDBLLexer.FALSE,
      SDBLLexer.FOR_UPDATE,
      SDBLLexer.FROM,
      SDBLLexer.FULL_JOIN,
      SDBLLexer.GROUP_BY,
      SDBLLexer.HAVING,
      SDBLLexer.HIERARCHY,
      SDBLLexer.IN,
      SDBLLexer.INDEX_BY,
      SDBLLexer.INNER_JOIN,
      SDBLLexer.INTO,
      SDBLLexer.IN_HIERARCHY,
      SDBLLexer.IS,
      SDBLLexer.ISNULL,
      SDBLLexer.JOIN,
      SDBLLexer.LEFT_JOIN,
      SDBLLexer.LIKE,
      SDBLLexer.NOT,
      SDBLLexer.OF,
      SDBLLexer.ONLY,
      SDBLLexer.ON_EN,
      SDBLLexer.OR,
      SDBLLexer.ORDER_BY,
      SDBLLexer.OVERALL,
      SDBLLexer.PERIODS,
      SDBLLexer.PO_RU,
      SDBLLexer.REFS,
      SDBLLexer.RIGHT_JOIN,
      SDBLLexer.SELECT,
      SDBLLexer.THEN,
      SDBLLexer.TOP,
      SDBLLexer.TOTALS,
      SDBLLexer.UNION,
      SDBLLexer.UNION_ALL,
      SDBLLexer.WHEN,
      SDBLLexer.WHERE
    );
  }

  private static Set<Integer> createSdblStrings() {
    return Set.of(
      SDBLLexer.STR
    );
  }

  private static Set<Integer> createSdblComments() {
    return Set.of(
      SDBLLexer.LINE_COMMENT
    );
  }

  private static Set<Integer> createSdblParameters() {
    return Set.of(
      SDBLLexer.AMPERSAND,
      SDBLLexer.PARAMETER_IDENTIFIER
    );
  }

  @Data
  @RequiredArgsConstructor
  @EqualsAndHashCode(exclude = "active")
  private static class HighlightingData {
    private final Range range;
    private final TypeOfText type;
    private boolean active = true;

    public HighlightingData(int line, int startChar, int endChar, TypeOfText type) {
      this(Ranges.create(line, startChar, endChar), type);
    }
  }
}
