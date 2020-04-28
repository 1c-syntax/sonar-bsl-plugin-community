/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2020
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
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.languageserver.configuration.diagnostics.SkipSupport;
import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.MetricStorage;
import com.github._1c_syntax.bsl.languageserver.context.ServerContext;
import com.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.DiagnosticSupplier;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameterInfo;
import com.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import com.github._1c_syntax.bsl.parser.BSLLexer;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import com.github._1c_syntax.utils.Absolute;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BSLCoreSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(BSLCoreSensor.class);
  private final SensorContext context;
  private final FileLinesContextFactory fileLinesContextFactory;

  private final boolean langServerEnabled;
  private final List<String> sourcesList;
  private final LanguageServerConfiguration languageServerConfiguration;
  private final DiagnosticProvider diagnosticProvider;
  private final IssuesLoader issuesLoader;

  private boolean calculateCoverLoc;

  public BSLCoreSensor(SensorContext context, FileLinesContextFactory fileLinesContextFactory) {
    this.context = context;
    this.fileLinesContextFactory = fileLinesContextFactory;

    langServerEnabled = context.config().getBoolean(BSLCommunityProperties.LANG_SERVER_ENABLED_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_ENABLED_DEFAULT_VALUE);

    calculateCoverLoc = context.config().getBoolean(BSLCommunityProperties.BSL_CALCULATE_LINE_TO_COVER_KEY)
      .orElse(BSLCommunityProperties.BSL_CALCULATE_LINE_TO_COVER_VALUE);

    sourcesList = context.config().get("sonar.sources")
      .map(sources ->
        Arrays.stream(StringUtils.split(sources, ","))
          .map(String::strip)
          .collect(Collectors.toList()))
      .orElse(Collections.singletonList("."));

    languageServerConfiguration = getLanguageServerConfiguration();
    DiagnosticSupplier diagnosticSupplier = new DiagnosticSupplier(languageServerConfiguration);
    diagnosticProvider = new DiagnosticProvider(diagnosticSupplier);
    issuesLoader = new IssuesLoader(context);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("BSL Core Sensor");
    descriptor.onlyOnLanguage(BSLLanguage.KEY);
  }

  @Override
  public void execute(SensorContext context) {
    LOGGER.info("Parsing files...");

    FileSystem fileSystem = context.fileSystem();
    File baseDir = fileSystem.baseDir();

    var absoluteSourceDirs = sourcesList.stream()
      .map((String sourceDir) -> {
        Path sourcePath = Path.of(sourceDir);
        if (sourcePath.isAbsolute()) {
          return sourcePath;
        } else {
          return Path.of(baseDir.toString(), sourceDir);
        }
      })
      .map(Absolute::path)
      .collect(Collectors.toList());

    FilePredicates predicates = fileSystem.predicates();
    Iterable<InputFile> inputFiles = fileSystem.inputFiles(
      predicates.hasLanguage(BSLLanguage.KEY)
    );

    Map<Path, List<InputFile>> inputFilesByPath = StreamSupport.stream(inputFiles.spliterator(), true)
      .collect(Collectors.groupingBy((InputFile inputFile) -> {
        var filePath = Absolute.path(inputFile.uri());
        return absoluteSourceDirs.stream()
          .filter(filePath::startsWith)
          .findAny()
          .orElse(baseDir.toPath());
      }));

    inputFilesByPath.forEach((Path sourceDir, List<InputFile> inputFilesList) -> {
      LOGGER.info("Source dir: {}", sourceDir);

      Path configurationRoot = LanguageServerConfiguration.getCustomConfigurationRoot(
        languageServerConfiguration,
        sourceDir
      );

      var bslServerContext = new ServerContext(configurationRoot);

      try (ProgressBar pb = new ProgressBar("", inputFilesList.size(), ProgressBarStyle.ASCII)) {
        inputFilesList.parallelStream().forEach((InputFile inputFile) -> {
          URI uri = inputFile.uri();
          LOGGER.debug(uri.toString());
          pb.step();

          processFile(inputFile, bslServerContext);
        });
      }

      bslServerContext.clear();
    });

  }


  private void processFile(InputFile inputFile, ServerContext bslServerContext) {
    URI uri = inputFile.uri();

    String content;
    try {
      content = IOUtils.toString(inputFile.inputStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.warn("Can't read content of file " + uri, e);
      content = "";
    }
    DocumentContext documentContext = bslServerContext.addDocument(uri, content);

    if (langServerEnabled) {
      diagnosticProvider.computeDiagnostics(documentContext)
        .forEach(diagnostic -> issuesLoader.createIssue(inputFile, diagnostic));
      diagnosticProvider.clearComputedDiagnostics(documentContext);
    }

    saveCpd(inputFile, documentContext);
    saveHighlighting(inputFile, documentContext);
    saveMeasures(inputFile, documentContext);

    saveCoverageLoc(inputFile, documentContext);

    documentContext.clearSecondaryData();
  }


  private void saveCpd(InputFile inputFile, DocumentContext documentContext) {

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

    synchronized (this) {
      cpdTokens.save();
    }

  }

  private void saveHighlighting(InputFile inputFile, DocumentContext documentContext) {

    NewHighlighting highlighting = context.newHighlighting().onFile(inputFile);

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
    });

    highlighting.save();

  }

  private void saveMeasures(InputFile inputFile, DocumentContext documentContext) {

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

    context.<Integer>newMeasure()
      .on(inputFile)
      .forMetric(CoreMetrics.COGNITIVE_COMPLEXITY)
      .withValue(metrics.getCognitiveComplexity())
      .save();

    context.<Integer>newMeasure()
      .on(inputFile)
      .forMetric(CoreMetrics.COMPLEXITY)
      .withValue(metrics.getCyclomaticComplexity())
      .save();

    context.<Integer>newMeasure()
      .on(inputFile)
      .forMetric(CoreMetrics.COMMENT_LINES)
      .withValue(metrics.getComments())
      .save();

    FileLinesContext fileLinesContext = fileLinesContextFactory.createFor(inputFile);
    for (int line : metrics.getNclocData()) {
      fileLinesContext.setIntValue(CoreMetrics.NCLOC_DATA_KEY, line, 1);
    }
    fileLinesContext.save();

  }

  private void saveCoverageLoc(InputFile inputFile, DocumentContext documentContext) {

    if (!calculateCoverLoc) {
      return;
    }

    NewCoverage coverage = context.newCoverage().onFile(inputFile);

    Arrays.stream(documentContext.getMetrics().getCovlocData())
      .forEach(loc -> coverage.lineHits(loc, 0));

    coverage.save();

  }

  private LanguageServerConfiguration getLanguageServerConfiguration() {

    boolean overrideConfiguration = context.config()
      .get(BSLCommunityProperties.LANG_SERVER_OVERRIDE_CONFIGURATION_KEY)
      .map(Boolean::parseBoolean)
      .orElse(BSLCommunityProperties.LANG_SERVER_OVERRIDE_CONFIGURATION_DEFAULT_VALUE);

    if (overrideConfiguration) {
      String configurationPath = context.config()
        .get(BSLCommunityProperties.LANG_SERVER_CONFIGURATION_PATH_KEY)
        .orElse(BSLCommunityProperties.LANG_SERVER_CONFIGURATION_PATH_DEFAULT_VALUE);

      File configurationFile = new File(configurationPath);
      if (configurationFile.exists()) {
        LOGGER.info("BSL LS configuration file exists. Overriding SonarQube rules' settings...");
        return LanguageServerConfiguration.create(configurationFile);
      } else {
        LOGGER.error("Can't find bsl configuration file {}. Using SonarQube config instead.", configurationPath);
      }
    }

    LanguageServerConfiguration configuration = LanguageServerConfiguration.create();
    String diagnosticLanguageCode = context.config()
      .get(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE);

    configuration.setLanguage(
      Language.valueOf(diagnosticLanguageCode.toUpperCase(Locale.ENGLISH))
    );

    SkipSupport skipSupport = context.config()
      .get(BSLCommunityProperties.LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_KEY)
      .map(value -> value.toUpperCase(Locale.ENGLISH).replace(" ", "_"))
      .map(SkipSupport::valueOf)
      .orElse(SkipSupport.valueOf(
        BSLCommunityProperties.LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_DEFAULT_VALUE.toUpperCase(Locale.ENGLISH)
      ));

    configuration.getDiagnosticsOptions().setSkipSupport(skipSupport);

    ActiveRules activeRules = context.activeRules();

    Map<String, Either<Boolean, Map<String, Object>>> diagnostics = new HashMap<>();
    List<Class<? extends BSLDiagnostic>> diagnosticClasses = DiagnosticSupplier.getDiagnosticClasses();

    for (Class<? extends BSLDiagnostic> diagnosticClass : diagnosticClasses) {
      DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnosticClass);
      String diagnosticCode = diagnosticInfo.getCode().getStringValue();
      ActiveRule activeRule = activeRules.find(
        RuleKey.of(
          BSLLanguageServerRuleDefinition.REPOSITORY_KEY,
          diagnosticCode
        )
      );
      if (activeRule == null) {
        diagnostics.put(diagnosticCode, Either.forLeft(false));
      } else {
        Map<String, String> params = activeRule.params();

        List<DiagnosticParameterInfo> diagnosticParameters = diagnosticInfo.getParameters();
        Map<String, Object> diagnosticConfiguration = new HashMap<>(diagnosticParameters.size());

        params.forEach((String key, String value) ->
          diagnosticInfo.getParameter(key).ifPresent(diagnosticParameterInfo ->
            diagnosticConfiguration.put(
              key,
              castDiagnosticParameterValue(value, diagnosticParameterInfo.getType())
            )
          )
        );
        diagnostics.put(
          diagnosticCode,
          Either.forRight(diagnosticConfiguration)
        );
      }
    }

    configuration.getDiagnosticsOptions().setParameters(diagnostics);

    return configuration;
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
      case BSLLexer.DATETIME:
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

  private static Object castDiagnosticParameterValue(String valueToCast, Class type) {
    Object value;
    if (type == Integer.class) {
      value = Integer.parseInt(valueToCast);
    } else if (type == Boolean.class) {
      value = Boolean.parseBoolean(valueToCast);
    } else if (type == Float.class) {
      value = Float.parseFloat(valueToCast);
    } else if (type == String.class) {
      value = valueToCast;
    } else {
      throw new IllegalArgumentException("Unsupported diagnostic parameter type " + type);
    }

    return value;
  }

}
