/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2020
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
import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.parser.SDBLLexer;
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
  private final String FILE_NAME = "test.bsl";

  private SensorContextTester context;
  private BSLHighlighter highlighter;
  private DocumentContext documentContext;
  private InputFile inputFile;

  @Test
  void testHighlightingBSL() {

    // given
    Vocabulary vocabulary = BSLLexer.VOCABULARY;
    Map<String, TypeOfText> highlightingMap = getHighlightingMapBSL(vocabulary);

    // then
    testHighlighting(vocabulary, highlightingMap);
  }

  @Test
  void testHighlightingSDBL() {
    // given
    Vocabulary vocabulary = SDBLLexer.VOCABULARY;
    Map<String, TypeOfText> highlightingMap = getHighlightingMapSDBL(vocabulary);

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
    documentContext = new DocumentContext(URI.create("file:///fake.bsl"), content, null, null);
    documentContext.rebuild(content);

    inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR, content);

    // when
    highlighter.saveHighlighting(inputFile, documentContext);

    // then
    String componentKey = "moduleKey:" + FILE_NAME;

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
    checkTokenTypeAtPosition(componentKey, 4, 16, TypeOfText.STRING);

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
    List<TypeOfText> typeOfTexts = context.highlightingTypeAt(componentKey, line, character);
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
      token.setCharPositionInLine(tokenType);
      tokens.add(token);
    }

    String content = tokens.stream()
      .map(Token::getText)
      .collect(Collectors.joining());

    when(documentContext.getTokens()).thenReturn(tokens);

    inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR, content);
  }

  private void checkHighlighting(Vocabulary vocabulary, SensorContextTester context, Map<String, TypeOfText> highlightingMap) {
    int maxTokenType = vocabulary.getMaxTokenType();
    String componentKey = "moduleKey:" + FILE_NAME;

    assertThat(IntStream.range(1, maxTokenType))
      .isNotEmpty()
      .allSatisfy(tokenType -> {
        String symbolicTokenName = vocabulary.getSymbolicName(tokenType);
        // no need to check lexer fragments or invisible names.
        if (symbolicTokenName == null) {
          return;
        }
        TypeOfText typeOfText = highlightingMap.get(symbolicTokenName);
        if (typeOfText == null) {
          return;
        }

        List<TypeOfText> typeOfTexts = context.highlightingTypeAt(componentKey, 1, tokenType);
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
      "PREPROC_NEWLINE",
      "BAR"
    );

    int maxTokenType = vocabulary.getMaxTokenType();

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

    Set<String> literals = Set.of(
      "TRUE",
      "FALSE",
      "UNDEFINED",
//      "NULL",
//      "DATETIME",
      "DECIMAL",
      "FLOAT"
    );

    Set<String> separators = Set.of(
//      "SEMICOLON",
//      "QUESTION",
//      "PLUS",
//      "MINUS",
//      "MUL",
//      "QUOTIENT",
//      "MODULO",
//      "ASSIGN",
//      "LESS_OR_EQUAL",
//      "LESS",
//      "NOT_EQUAL",
//      "GREATER_OR_EQUAL",
//      "GREATER",
//      "COMMA",
//      "COLON",
//      "TILDA"
    );

    Set<String> noOpTypes = Set.of(
//      "WHITE_SPACE",
//      "DOT",
//      "LBRACK",
//      "RBRACK",
//      "LPAREN",
//      "RPAREN",
//      "SQUOTE",
//      "IDENTIFIER",
//      "UNKNOWN",
//      "PREPROC_NEWLINE",
//      "BAR"
    );

    int maxTokenType = vocabulary.getMaxTokenType();

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
      } else if (ruleName.endsWith("_KEYWORD") && !ruleName.startsWith("PREPROC_")) {
        typeOfText = TypeOfText.KEYWORD;
      } else if (literals.contains(ruleName)) {
        typeOfText = TypeOfText.CONSTANT;
      } else if (separators.contains(ruleName)) {
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

}
