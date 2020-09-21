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
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import java.io.File;
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

  @Test
  void testHighlighting() {

    // given
    SensorContextTester context = SensorContextTester.create(Path.of("."));
    var highlighter = new BSLHighlighter(context);
    var documentContext = mock(DocumentContext.class);
    Vocabulary vocabulary = BSLLexer.VOCABULARY;
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

    String fileName = "test.bsl";
    InputFile inputFile = Tools.inputFileBSL(fileName, BASE_DIR, content);

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

    // when
    highlighter.saveHighlighting(inputFile, documentContext);

    // then
    String componentKey = "moduleKey:" + fileName;

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
}
