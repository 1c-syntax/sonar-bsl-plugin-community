/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2019
 * Nikita Gryzlov <nixel2007@gmail.com>
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
package com.github._1c_syntax.sonar.bsl;

import com.github._1c_syntax.sonar.bsl.language.BSLLanguage;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.github._1c_syntax.bsl.parser.BSLLexer;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public class BSLCoreSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(BSLCoreSensor.class);
  private final SensorContext context;

  private Map<InputFile, List<? extends Token>> fileTokens = Collections.synchronizedMap(new HashMap<>());

  public BSLCoreSensor(SensorContext context) {
    this.context = context;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("BSL Core Sensor");
    descriptor.onlyOnLanguage(BSLLanguage.KEY);
  }

  @Override
  public void execute(SensorContext context) {

    FileSystem fileSystem = context.fileSystem();
    FilePredicates predicates = fileSystem.predicates();
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(
      predicates.and(
        predicates.hasLanguage(BSLLanguage.KEY)
      )
    );

    LOGGER.info("Parsing files...");
    StreamSupport.stream(inputFiles.spliterator(), true)
      .forEach(inputFile -> fileTokens.put(inputFile, getTokens(inputFile)));

    LOGGER.info("Saving measures...");
    saveMeasures();

    LOGGER.info("Saving CPD info...");
    saveCpd();

    LOGGER.info("Saving highlighting...");
    saveHighlighting();

  }

  private void saveMeasures() {

    fileTokens.forEach((inputFile, tokens) -> {

      int ncloc = (int) tokens.stream()
        .filter(token -> token.getChannel() == Token.DEFAULT_CHANNEL)
        .map(Token::getLine)
        .distinct()
        .count();

      NewMeasure<Integer> measure = context.newMeasure();
      measure.on(inputFile)
        .forMetric(CoreMetrics.NCLOC)
        .withValue(ncloc)
        .save();
    });

  }

  private void saveCpd() {

    fileTokens.forEach((inputFile, tokens) -> {

      NewCpdTokens cpdTokens = context.newCpdTokens();
      cpdTokens.onFile(inputFile);

      tokens.stream()
        .filter(token -> token.getChannel() == Token.DEFAULT_CHANNEL && token.getType() != Token.EOF)
        .forEach(token -> {
            int line = token.getLine();
            int charPositionInLine = token.getCharPositionInLine();
            String tokenText = token.getText();
            cpdTokens.addToken(
              line,
              charPositionInLine,
              line,
              charPositionInLine + tokenText.length(),
              tokenText
            );
          }
        );

      cpdTokens.save();
    });
  }

  private void saveHighlighting() {

    fileTokens.forEach((inputFile, tokens) -> {

      NewHighlighting highlighting = context.newHighlighting();
      highlighting.onFile(inputFile);

      tokens.forEach(token -> {
          TypeOfText typeOfText = getTypeOfText(token.getType());
          if (typeOfText == null) {
            return;
          }
          int line = token.getLine();
          int charPositionInLine = token.getCharPositionInLine();
          String tokenText = token.getText();
          highlighting.highlight(
            line,
            charPositionInLine,
            line,
            charPositionInLine + tokenText.length(),
            typeOfText
          );
        }
      );

      highlighting.save();
    });

  }

  @Nullable
  private TypeOfText getTypeOfText(int tokenType) {

    TypeOfText typeOfText = null;

    switch (tokenType) {
      case BSLLexer.PROCEDURE_KEYWORD:
      case BSLLexer.FUNCTION_KEYWORD:
      case BSLLexer.ENDPROCEDURE_KEYWORD:
      case BSLLexer.ENDFUNCTION_KEYWORD:
      case BSLLexer.EXPORT_KEYWORD:
      case BSLLexer.VAL_KEYWORD:
      case BSLLexer.ENDIF_KEYWORD:
      case BSLLexer.ENDDO_KEYWORD:
      case BSLLexer.IF_KEYWORD:
      case BSLLexer.ELSIF_KEYWORD:
      case BSLLexer.ELSE_KEYWORD:
      case BSLLexer.THEN_KEYWORD:
      case BSLLexer.WHILE_KEYWORD:
      case BSLLexer.DO_KEYWORD:
      case BSLLexer.FOR_KEYWORD:
      case BSLLexer.TO_KEYWORD:
      case BSLLexer.EACH_KEYWORD:
      case BSLLexer.FROM_KEYWORD:
      case BSLLexer.TRY_KEYWORD:
      case BSLLexer.EXCEPT_KEYWORD:
      case BSLLexer.ENDTRY_KEYWORD:
      case BSLLexer.RETURN_KEYWORD:
      case BSLLexer.CONTINUE_KEYWORD:
      case BSLLexer.RAISE_KEYWORD:
      case BSLLexer.VAR_KEYWORD:
      case BSLLexer.NOT_KEYWORD:
      case BSLLexer.OR_KEYWORD:
      case BSLLexer.AND_KEYWORD:
      case BSLLexer.NEW_KEYWORD:
      case BSLLexer.GOTO_KEYWORD:
      case BSLLexer.BREAK_KEYWORD:
      case BSLLexer.EXECUTE_KEYWORD:
        typeOfText = TypeOfText.KEYWORD;
        break;
      case BSLLexer.TRUE:
      case BSLLexer.FALSE:
      case BSLLexer.UNDEFINED:
      case BSLLexer.NULL:
        typeOfText = TypeOfText.CONSTANT;
        break;
      case BSLLexer.DECIMAL:
      case BSLLexer.FLOAT:
        typeOfText = TypeOfText.CONSTANT;
        break;
      case BSLLexer.STRING:
      case BSLLexer.STRINGSTART:
      case BSLLexer.STRINGPART:
      case BSLLexer.STRINGTAIL:
      case BSLLexer.PREPROC_STRING:
        typeOfText = TypeOfText.STRING;
        break;
      case BSLLexer.DATETIME:
        typeOfText = TypeOfText.CONSTANT;
        break;
      case BSLLexer.LINE_COMMENT:
        typeOfText = TypeOfText.COMMENT;
        break;
      case BSLLexer.HASH:
      case BSLLexer.PREPROC_USE_KEYWORD:
      case BSLLexer.PREPROC_REGION:
      case BSLLexer.PREPROC_END_REGION:
      case BSLLexer.PREPROC_AND_KEYWORD:
      case BSLLexer.PREPROC_OR_KEYWORD:
      case BSLLexer.PREPROC_NOT_KEYWORD:
      case BSLLexer.PREPROC_IF_KEYWORD:
      case BSLLexer.PREPROC_THEN_KEYWORD:
      case BSLLexer.PREPROC_ELSIF_KEYWORD:
      case BSLLexer.PREPROC_ELSE_KEYWORD:
      case BSLLexer.PREPROC_ENDIF_KEYWORD:
        typeOfText = TypeOfText.PREPROCESS_DIRECTIVE;
        break;
      case BSLLexer.AMPERSAND:
      case BSLLexer.ANNOTATION_ATCLIENT_SYMBOL:
      case BSLLexer.ANNOTATION_ATCLIENTATSERVER_SYMBOL:
      case BSLLexer.ANNOTATION_ATCLIENTATSERVERNOCONTEXT_SYMBOL:
      case BSLLexer.ANNOTATION_ATSERVER_SYMBOL:
      case BSLLexer.ANNOTATION_ATSERVERNOCONTEXT_SYMBOL:
      case BSLLexer.ANNOTATION_CUSTOM_SYMBOL:
        typeOfText = TypeOfText.ANNOTATION;
        break;
      default:
        // no-op
    }

    return typeOfText;

  }

  private List<? extends Token> getTokens(InputFile inputFile) {

    InputStream inputStream;
    try {
      inputStream = inputFile.inputStream();
    } catch (IOException e) {
      LOGGER.warn("Can't get content of file " + inputFile.filename());
      return Collections.emptyList();
    }
    CharStream input;
    try {
      input = CharStreams.fromStream(inputStream, inputFile.charset());
    } catch (IOException e) {
      LOGGER.warn("Can't create char stream from file " + inputFile.filename());
      return Collections.emptyList();
    }

    BSLLexer lexer = new BSLLexer(input);
    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    tokenStream.fill();

    return tokenStream.getTokens();
  }


}
