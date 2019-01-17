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
import org.github._1c_syntax.parser.BSLLexer;
import org.github._1c_syntax.parser.BSLParser;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BSLCoreSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(BSLCoreSensor.class);

  private Map<InputFile, BSLParser.FileContext> fileTrees = new HashMap<>();
  private final BSLLexer lexer = new BSLLexer(null);
  private final BSLParser parser = new BSLParser(null);

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


    Stream<InputFile> stream = StreamSupport.stream(inputFiles.spliterator(), true);
    stream.forEach(inputFile -> fileTrees.put(inputFile, parseInputFile(inputFile)));

    fileTrees.entrySet().stream()
      .filter(Objects::nonNull)
      .forEach(entry -> {

        InputFile inputFile = entry.getKey();
        BSLParser.FileContext fileContext = entry.getValue();

        NewCpdTokens cpdTokens = context.newCpdTokens();
        cpdTokens.onFile(inputFile);

        List<Token> tokens = fileContext.getTokens();
        tokens.forEach(token -> {
            int line = token.getLine();
            int charPositionInLine = token.getCharPositionInLine();
            String tokenText = token.getText();
            if (tokenText == null) {
              tokenText = "";
            }
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

  // TODO: To separate class. BSL Parser itself?
  private BSLParser.FileContext parseInputFile(InputFile inputFile) {
    InputStream inputStream;
    try {
      inputStream = inputFile.inputStream();
    } catch (IOException e) {
      LOGGER.warn("Can't get content of file " + inputFile.filename());
      return null;
    }
    CharStream input;
    try {
      input = CharStreams.fromStream(inputStream, inputFile.charset());
    } catch (IOException e) {
      LOGGER.warn("Can't create char stream from file " + inputFile.filename());
      return null;
    }

    lexer.setInputStream(input);

    CommonTokenStream tokenStream = new CommonTokenStream(lexer);
    parser.setTokenStream(tokenStream);

    return parser.file();
  }
}
