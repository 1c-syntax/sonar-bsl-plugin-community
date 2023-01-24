/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2023
 * Alexey Sosnoviy <labotamy@gmail.com>, Nikita Fedkin <nixel2007@gmail.com>
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

import com.github._1c_syntax.bsl.languageserver.BSLLSBinding;
import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.parser.SDBLLexer;
import com.github._1c_syntax.bsl.parser.SDBLTokenizer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BSLHighlighterTest {

  private final String BASE_PATH = "src/test/resources/src";
  private final File BASE_DIR = new File(BASE_PATH).getAbsoluteFile();
  private final String FILE_NAME = "src/test.bsl";

  private SensorContextTester context;
  private BSLHighlighter highlighter;
  private DocumentContext documentContext;
  private InputFile inputFile;

  @Test
  void testHighlightingBSL() {

    // given
    var vocabulary = BSLLexer.VOCABULARY;
    var highlightingMap = getHighlightingMapBSL(vocabulary);

    // then
    testHighlighting(vocabulary, highlightingMap);
  }

  @Test
  void testHighlightingSDBL() {
    // given
    var vocabulary = SDBLLexer.VOCABULARY;
    var highlightingMap = getHighlightingMapSDBL(vocabulary);

    // then
    testHighlighting(vocabulary, highlightingMap);
  }

  @Test
  void testMergeHighlightingTokens() {
    // given
    context = SensorContextTester.create(Path.of("."));
    highlighter = new BSLHighlighter(context);
    String content = "А = \"ВЫБРАТЬ РАЗРЕШЕННЫЕ Поле.Один \n" +
      "|КАК \n" +
      "|  Один, 2 \n" +
      " |  КАК Два ИЗ Справочник.Поле\n" +
      "|АВТОУПОРЯДОЧИВАНИЕ;\";";
    documentContext = BSLLSBinding.getServerContext().addDocument(URI.create("file:///fake.bsl"), content, 1);

    inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR, content);

    // when
    highlighter.saveHighlighting(inputFile, documentContext);

    // then
    var componentKey = "moduleKey:" + FILE_NAME;

    checkTokenTypeAtPosition(componentKey, 1, 4, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 1, 5, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 1, 6, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 1, 12, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 1, 13, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 1, 25, TypeOfText.STRING);

    checkTokenTypeAtPosition(componentKey, 2, 0, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 2, 1, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 2, 2, TypeOfText.KEYWORD);

    checkTokenTypeAtPosition(componentKey, 3, 0, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 3, 1, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 3, 5, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 3, 9, TypeOfText.CONSTANT);

    checkTokenTypeAtPosition(componentKey, 4, 1, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 4, 2, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 4, 6, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 4, 10, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 4, 13, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 4, 16, TypeOfText.KEYWORD_LIGHT);

    checkTokenTypeAtPosition(componentKey, 5, 0, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 5, 1, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 5, 18, TypeOfText.KEYWORD);
    checkTokenTypeAtPosition(componentKey, 5, 19, TypeOfText.KEYWORD_LIGHT);
    checkTokenTypeAtPosition(componentKey, 5, 20, TypeOfText.STRING);
    checkTokenTypeAtPosition(componentKey, 5, 21, TypeOfText.KEYWORD_LIGHT);

  }

  private void testHighlighting(Vocabulary vocabulary, Map<String, TypeOfText> highlightingMap) {
    // given
    initContext(vocabulary);

    // when
    highlighter.saveHighlighting(inputFile, documentContext);

    // then
    checkHighlighting(vocabulary, context, highlightingMap);
  }

  private void checkTokenTypeAtPosition(String componentKey, int line, int character, TypeOfText typeOfText) {
    var typeOfTexts = context.highlightingTypeAt(componentKey, line, character);
    assertThat(typeOfTexts)
      .as("Position %d:%d should have typeOfText %s", line, character, typeOfText)
      .contains(typeOfText);
  }

  private void initContext(Vocabulary vocabulary) {
    context = SensorContextTester.create(Path.of("."));
    highlighter = new BSLHighlighter(context);
    documentContext = mock(DocumentContext.class);
    List<Token> tokens = new ArrayList<>();

    int maxTokenType = vocabulary.getMaxTokenType();
    for (var tokenType = 1; tokenType <= maxTokenType; tokenType++) {
      var token = new CommonToken(tokenType, "a");
      token.setLine(1);
      token.setCharPositionInLine(tokenType - 1);
      tokens.add(token);
    }

    var content = tokens.stream()
      .map(Token::getText)
      .collect(Collectors.joining());

    if (vocabulary.equals(SDBLLexer.VOCABULARY)) {
      var sdblTokenizer = mock(SDBLTokenizer.class);
      when(sdblTokenizer.getTokens()).thenReturn(tokens);
      when(documentContext.getQueries()).thenReturn(List.of(sdblTokenizer));
    } else {
      when(documentContext.getTokens()).thenReturn(tokens);
    }
    inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR, content);
  }

  private void checkHighlighting(Vocabulary vocabulary,
                                 SensorContextTester context,
                                 Map<String, TypeOfText> highlightingMap) {
    var maxTokenType = vocabulary.getMaxTokenType();
    var componentKey = "moduleKey:" + FILE_NAME;

    assertThat(IntStream.range(1, maxTokenType))
      .isNotEmpty()
      .allSatisfy(tokenType -> {
        String symbolicTokenName = vocabulary.getSymbolicName(tokenType);
        // no need to check lexer fragments or invisible names.
        if (symbolicTokenName == null) {
          return;
        }
        var typeOfText = highlightingMap.get(symbolicTokenName);
        if (typeOfText == null) {
          return;
        }

        var typeOfTexts = context.highlightingTypeAt(componentKey, 1, tokenType - 1);
        assertThat(typeOfTexts)
          .as("Symbolic token name %s should maps to typeOfText %s", symbolicTokenName, typeOfText)
          .contains(typeOfText);
      });
  }

  private Map<String, TypeOfText> getHighlightingMapBSL(Vocabulary vocabulary) {

    Set<String> literals = Set.of(
      "TRUE",
      "FALSE",
      "UNDEFINED",
      "NULL",
      "DATETIME",
      "DECIMAL",
      "FLOAT"
    );

    Set<String> punctuators = Set.of(
      "SEMICOLON",
      "QUESTION",
      "PLUS",
      "MINUS",
      "MUL",
      "QUOTIENT",
      "MODULO",
      "ASSIGN",
      "LESS_OR_EQUAL",
      "LESS",
      "NOT_EQUAL",
      "GREATER_OR_EQUAL",
      "GREATER",
      "COMMA",
      "COLON",
      "TILDA"
    );

    Set<String> noOpTypes = Set.of(
      "WHITE_SPACE",
      "DOT",
      "LBRACK",
      "RBRACK",
      "LPAREN",
      "RPAREN",
      "SQUOTE",
      "IDENTIFIER",
      "UNKNOWN",
      "ANNOTATION_UNKNOWN",
      "PREPROC_NEWLINE",
      "BAR"
    );

    var maxTokenType = vocabulary.getMaxTokenType();

    Map<String, TypeOfText> highlightingMap = new HashMap<>();
    for (int tokenType = 1; tokenType <= maxTokenType; tokenType++) {
      var ruleName = vocabulary.getSymbolicName(tokenType);
      // no need to check lexer fragments or invisible names.
      if (ruleName == null) {
        continue;
      }

      TypeOfText typeOfText = null;
      if (noOpTypes.contains(ruleName)) {
        continue;
      } else if (ruleName.endsWith("_KEYWORD") && !ruleName.startsWith("PREPROC_")) {
        typeOfText = TypeOfText.KEYWORD;
      } else if (literals.contains(ruleName)) {
        typeOfText = TypeOfText.CONSTANT;
      } else if (punctuators.contains(ruleName)) {
        typeOfText = TypeOfText.KEYWORD_LIGHT;
      } else if (ruleName.contains("STRING")) {
        typeOfText = TypeOfText.STRING;
      } else if (ruleName.contains("LINE_COMMENT")) {
        typeOfText = TypeOfText.COMMENT;
      } else if (ruleName.equals("AMPERSAND") || ruleName.contains("ANNOTATION_")) {
        typeOfText = TypeOfText.ANNOTATION;
      } else if (ruleName.equals("HASH") || ruleName.contains("PREPROC_")) {
        typeOfText = TypeOfText.PREPROCESS_DIRECTIVE;
      }

      if (typeOfText == null) {
        throw new IllegalArgumentException("Unknown type of text for lexer rule name: " + ruleName);
      }

      highlightingMap.put(ruleName, typeOfText);
    }
    return highlightingMap;
  }

  private Map<String, TypeOfText> getHighlightingMapSDBL(Vocabulary vocabulary) {

    Set<String> keywords = Set.of(
      "ALL",
      "ALLOWED",
      "AND",
      "AS",
      "ASC",
      "AUTOORDER",
      "BETWEEN",
      "BY_EN",
      "CASE",
      "CAST",
      "DESC",
      "DISTINCT",
      "DROP",
      "ELSE",
      "END",
      "ESCAPE",
      "EMPTYREF",
      "FALSE",
      "FOR",
      "FROM",
      "FULL",
      "GROUP",
      "GROUPEDBY",
      "GROUPING",
      "HAVING",
      "HIERARCHY",
      "HIERARCHY_FOR_IN",
      "IN",
      "INDEX",
      "INNER",
      "INTO",
      "IS",
      "ISNULL",
      "JOIN",
      "LEFT",
      "LIKE",
      "NOT",
      "OF",
      "ON_EN",
      "OR",
      "ORDER",
      "OVERALL",
      "OUTER",
      "PO_RU",
      "RIGHT",
      "SELECT",
      "SET",
      "THEN",
      "TOP",
      "TOTALS",
      "UNION",
      "WHEN",
      "WHERE",
      "ONLY",
      "PERIODS",
      "REFS",
      "UPDATE"
    );

    Set<String> functions = Set.of(
      "AVG",
      "BEGINOFPERIOD",
      "BOOLEAN",
      "COUNT",
      "DATE",
      "DATEADD",
      "DATEDIFF",
      "DATETIME",
      "DAY",
      "DAYOFYEAR",
      "EMPTYTABLE",
      "ENDOFPERIOD",
      "HALFYEAR",
      "HOUR",
      "MAX",
      "MIN",
      "MINUTE",
      "MONTH",
      "NUMBER",
      "QUARTER",
      "PRESENTATION",
      "RECORDAUTONUMBER",
      "REFPRESENTATION",
      "SECOND",
      "STRING",
      "SUBSTRING",
      "SUM",
      "TENDAYS",
      "TYPE",
      "VALUE",
      "VALUETYPE",
      "WEEK",
      "WEEKDAY",
      "YEAR"
    );

    Set<String> metadataTypes = Set.of(
      "ACCOUNTING_REGISTER_TYPE",
      "ACCUMULATION_REGISTER_TYPE",
      "BUSINESS_PROCESS_TYPE",
      "CALCULATION_REGISTER_TYPE",
      "CATALOG_TYPE",
      "CHART_OF_ACCOUNTS_TYPE",
      "CHART_OF_CALCULATION_TYPES_TYPE",
      "CHART_OF_CHARACTERISTIC_TYPES_TYPE",
      "CONSTANT_TYPE",
      "DOCUMENT_TYPE",
      "DOCUMENT_JOURNAL_TYPE",
      "ENUM_TYPE",
      "EXCHANGE_PLAN_TYPE",
      "EXTERNAL_DATA_SOURCE_TYPE",
      "FILTER_CRITERION_TYPE",
      "INFORMATION_REGISTER_TYPE",
      "SEQUENCE_TYPE",
      "TASK_TYPE"
    );

    Set<String> virtualTables = Set.of(
      "ACTUAL_ACTION_PERIOD_VT",
      "BALANCE_VT",
      "BALANCE_AND_TURNOVERS_VT",
      "BOUNDARIES_VT",
      "DR_CR_TURNOVERS_VT",
      "EXT_DIMENSIONS_VT",
      "RECORDS_WITH_EXT_DIMENSIONS_VT",
      "SCHEDULE_DATA_VT",
      "SLICEFIRST_VT",
      "SLICELAST_VT",
      "TASK_BY_PERFORMER_VT",
      "TURNOVERS_VT"
    );

    Set<String> literals = Set.of(
      "TRUE",
      "FALSE",
      "UNDEFINED",
      "NULL",
      "DECIMAL",
      "FLOAT"
    );

    Set<String> separators = Set.of(
      "SEMICOLON",
      "PLUS",
      "MINUS",
      "MUL",
      "QUOTIENT",
      "ASSIGN",
      "LESS_OR_EQUAL",
      "LESS",
      "NOT_EQUAL",
      "GREATER_OR_EQUAL",
      "GREATER",
      "COMMA",
      "BRACE",
      "BRACE_START"
    );

    Set<String> noOpTypes = Set.of(
      "WHITE_SPACE",
      "DOT",
      "LPAREN",
      "RPAREN",
      "ROUTEPOINT_FIELD",
      "IDENTIFIER",
      "INCORRECT_IDENTIFIER",
      "BRACE_IDENTIFIER",
      "UNKNOWN",
      "BAR" // TODO: Убрать из лексера
    );

    var maxTokenType = vocabulary.getMaxTokenType();

    Map<String, TypeOfText> highlightingMap = new HashMap<>();
    for (int tokenType = 1; tokenType <= maxTokenType; tokenType++) {
      String ruleName = vocabulary.getSymbolicName(tokenType);
      // no need to check lexer fragments or invisible names.
      if (ruleName == null) {
        continue;
      }

      TypeOfText typeOfText = null;
      if (noOpTypes.contains(ruleName)) {
        continue;
      } else if (keywords.contains(ruleName)) {
        typeOfText = TypeOfText.KEYWORD;
      } else if (literals.contains(ruleName)) {
        typeOfText = TypeOfText.CONSTANT;
      } else if (separators.contains(ruleName)) {
        typeOfText = TypeOfText.KEYWORD_LIGHT;
      } else if (functions.contains(ruleName)) {
        typeOfText = TypeOfText.KEYWORD_LIGHT;
      } else if (metadataTypes.contains(ruleName)) {
        typeOfText = TypeOfText.KEYWORD_LIGHT;
      } else if (virtualTables.contains(ruleName)) {
        typeOfText = TypeOfText.KEYWORD_LIGHT;
      } else if (ruleName.equals("STR")) {
        typeOfText = TypeOfText.STRING;
      } else if (ruleName.contains("LINE_COMMENT")) {
        typeOfText = TypeOfText.COMMENT;
      } else if (ruleName.equals("AMPERSAND") || ruleName.equals("PARAMETER_IDENTIFIER")) {
        typeOfText = TypeOfText.ANNOTATION;
      }

      if (typeOfText == null) {
        throw new IllegalArgumentException("Unknown type of text for lexer rule name: " + ruleName);
      }

      highlightingMap.put(ruleName, typeOfText);
    }
    return highlightingMap;
  }

}
