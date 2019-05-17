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
import com.github._1c_syntax.sonar.bsl.language.BSLLanguageServerRuleDefinition;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.github._1c_syntax.bsl.languageserver.configuration.DiagnosticLanguage;
import org.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import org.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import org.github._1c_syntax.bsl.languageserver.context.MetricStorage;
import org.github._1c_syntax.bsl.languageserver.context.ServerContext;
import org.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameter;
import org.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import org.github._1c_syntax.bsl.parser.BSLLexer;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

public class BSLCoreSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(BSLCoreSensor.class);
  private final SensorContext context;
  private Map<InputFile, DocumentContext> inputFilesMap;
  private Map<InputFile, List<Diagnostic>> inputFileDiagnostics;

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
    ServerContext bslServerContext = new ServerContext();

    long inputFleSize = StreamSupport.stream(inputFiles.spliterator(), false).count();
    inputFilesMap = new HashMap<>();

    try (ProgressBar pb = new ProgressBar("", inputFleSize, ProgressBarStyle.ASCII)) {
      StreamSupport.stream(inputFiles.spliterator(), true)
        .forEach((InputFile inputFile) -> {
          URI uri = inputFile.uri();
          LOGGER.debug(uri.toString());
          pb.step();

          String content;
          try {
            content = IOUtils.toString(inputFile.inputStream(), StandardCharsets.UTF_8);
          } catch (IOException e) {
            LOGGER.warn("Can't read content of file " + uri, e);
            content = "";
          }
          inputFilesMap.put(inputFile, bslServerContext.addDocument(uri.toString(), content));
        });
    }

    Boolean langServerEnabled = context.config().getBoolean(BSLCommunityProperties.LANG_SERVER_ENABLED_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_ENABLED_DEFAULT_VALUE);
    if (langServerEnabled) {
      runLangServerAnalyze();
    } else {
      LOGGER.info("Internal analysis with BSL Language server is disabled. Skipping...");
    }

    LOGGER.info("Saving measures...");
    saveMeasures();

    LOGGER.info("Saving CPD info...");
    saveCpd();

    LOGGER.info("Saving highlighting...");
    saveHighlighting();

  }

  private void runLangServerAnalyze() {
    LOGGER.info("Analyze files...");
    inputFileDiagnostics = new HashMap<>();

    DiagnosticProvider diagnosticProvider = new DiagnosticProvider(getLanguageServerConfiguration());
    try (ProgressBar pb = new ProgressBar("", inputFilesMap.size(), ProgressBarStyle.ASCII)) {
      inputFilesMap.entrySet().parallelStream()
        .forEach((Map.Entry<InputFile, DocumentContext> entry) -> {
          URI uri = entry.getKey().uri();
          LOGGER.debug(uri.toString());
          pb.step();

          List<Diagnostic> diagnostics = diagnosticProvider.computeDiagnostics(entry.getValue());
          inputFileDiagnostics.put(entry.getKey(), diagnostics);
        });
    }

    LOGGER.info("Saving issues...");
    saveIssues();
  }

  private void saveIssues() {
    IssuesLoader issuesLoader = new IssuesLoader(context);

    inputFileDiagnostics.forEach((InputFile inputFile, List<Diagnostic> diagnostics) ->
      diagnostics.forEach(diagnostic -> issuesLoader.createIssue(inputFile, diagnostic)));
  }

  private void saveMeasures() {

    inputFilesMap.forEach((InputFile inputFile, DocumentContext documentContext) -> {

      MetricStorage metrics = documentContext.getMetrics();

      context.<Integer>newMeasure().on(inputFile)
        .forMetric(CoreMetrics.NCLOC)
        .withValue(metrics.getNcloc())
        .save();

      context.<Integer>newMeasure().on(inputFile)
        .forMetric(CoreMetrics.STATEMENTS)
        .withValue(metrics.getStatements())
        .save();

      context.<Integer>newMeasure()
        .on(inputFile)
        .forMetric(CoreMetrics.FUNCTIONS)
        .withValue(metrics.getProcedures() + metrics.getFunctions())
        .save();

    });

  }

  private void saveCpd() {

    inputFilesMap.forEach((InputFile inputFile, DocumentContext documentContext) -> {

      NewCpdTokens cpdTokens = context.newCpdTokens();
      cpdTokens.onFile(inputFile);

      documentContext.getTokensFromDefaultChannel()
        .forEach((Token token) -> {
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

    inputFilesMap.forEach((InputFile inputFile, DocumentContext documentContext) -> {

      NewHighlighting highlighting = context.newHighlighting();
      highlighting.onFile(inputFile);

      documentContext.getTokens().forEach((Token token) -> {
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

  private LanguageServerConfiguration getLanguageServerConfiguration() {
    LanguageServerConfiguration languageServerConfiguration = LanguageServerConfiguration.create();
    String diagnosticLanguageCode = context.config()
      .get(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE);

    languageServerConfiguration.setDiagnosticLanguage(
      DiagnosticLanguage.valueOf(diagnosticLanguageCode.toUpperCase(Locale.ENGLISH))
    );

    List<Class<? extends BSLDiagnostic>> diagnosticClasses = DiagnosticProvider.getDiagnosticClasses();
    ActiveRules activeRules = context.activeRules();

    Map<String, Either<Boolean, Map<String, Object>>> diagnostics = new HashMap<>();
    for (Class<? extends BSLDiagnostic> diagnosticClass : diagnosticClasses) {
      ActiveRule activeRule = activeRules.find(
        RuleKey.of(
          BSLLanguageServerRuleDefinition.REPOSITORY_KEY,
          DiagnosticProvider.getDiagnosticCode(diagnosticClass)
        )
      );
      if (activeRule == null) {
        diagnostics.put(DiagnosticProvider.getDiagnosticCode(diagnosticClass), Either.forLeft(false));
      } else {
        Map<String, String> params = activeRule.params();

        Map<String, DiagnosticParameter> diagnosticParameters =
          DiagnosticProvider.getDiagnosticParameters(diagnosticClass);
        Map<String, Object> diagnosticConfiguration = new HashMap<>(diagnosticParameters.size());

        params.entrySet().forEach((Map.Entry<String, String> param) -> {
            DiagnosticParameter diagnosticParameter = diagnosticParameters.get(param.getKey());
            diagnosticConfiguration.put(
              param.getKey(),
              DiagnosticProvider.castDiagnosticParameterValue(param.getValue(), diagnosticParameter.type())
            );
          });
        diagnostics.put(
          DiagnosticProvider.getDiagnosticCode(diagnosticClass),
          Either.forRight(diagnosticConfiguration)
        );
      }
    }

    languageServerConfiguration.setDiagnostics(diagnostics);

    return languageServerConfiguration;
  }

  @Nullable
  private static TypeOfText getTypeOfText(int tokenType) {

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
      case BSLLexer.IN_KEYWORD:
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

}
