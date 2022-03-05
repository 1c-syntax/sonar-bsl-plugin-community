/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2022
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
import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.languageserver.configuration.diagnostics.SkipSupport;
import com.github._1c_syntax.bsl.languageserver.context.DocumentContext;
import com.github._1c_syntax.bsl.languageserver.context.MetricStorage;
import com.github._1c_syntax.bsl.languageserver.context.ServerContext;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameterInfo;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import com.github._1c_syntax.utils.Absolute;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
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
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
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
  private final IssuesLoader issuesLoader;
  private final BSLHighlighter highlighter;

  public BSLCoreSensor(SensorContext context, FileLinesContextFactory fileLinesContextFactory) {
    this.context = context;
    this.fileLinesContextFactory = fileLinesContextFactory;

    langServerEnabled = context.config().getBoolean(BSLCommunityProperties.LANG_SERVER_ENABLED_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_ENABLED_DEFAULT_VALUE);

    sourcesList = context.config().get("sonar.sources")
      .map(sources ->
        Arrays.stream(StringUtils.split(sources, ","))
          .map(String::strip)
          .collect(Collectors.toList()))
      .orElse(Collections.singletonList("."));

    issuesLoader = new IssuesLoader(context);
    highlighter = new BSLHighlighter(context);
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
        Path sourcePath = Path.of(sourceDir.trim());
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

    LanguageServerConfiguration languageServerConfiguration = getLanguageServerConfiguration();

    inputFilesByPath.forEach((Path sourceDir, List<InputFile> inputFilesList) -> {
      LOGGER.info("Source dir: {}", sourceDir);

      Path configurationRoot = LanguageServerConfiguration.getCustomConfigurationRoot(
        languageServerConfiguration,
        sourceDir
      );

      var bslServerContext = BSLLSBinding.getServerContext();
      bslServerContext.setConfigurationRoot(configurationRoot);
      bslServerContext.populateContext();

      try (ProgressBar pb = new ProgressBarBuilder()
        .setTaskName("")
        .setInitialMax(inputFilesList.size())
        .setStyle(ProgressBarStyle.ASCII)
        .build()) {
        inputFilesList.parallelStream().forEach((InputFile inputFile) -> {
          URI uri = inputFile.uri();
          LOGGER.debug(uri.toString());
          pb.step();

          processFile(inputFile, bslServerContext);
        });
      }

      bslServerContext.clear();
    });

    BSLLSBinding.getApplicationContext().close();
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
    DocumentContext documentContext = bslServerContext.addDocument(uri, content, 1);

    if (langServerEnabled) {
      documentContext.getDiagnostics()
        .forEach(diagnostic -> issuesLoader.createIssue(inputFile, diagnostic));
    }

    saveCpd(inputFile, documentContext);
    highlighter.saveHighlighting(inputFile, documentContext);
    saveMeasures(inputFile, documentContext);

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

  private LanguageServerConfiguration getLanguageServerConfiguration() {

    boolean overrideConfiguration = context.config()
      .get(BSLCommunityProperties.LANG_SERVER_OVERRIDE_CONFIGURATION_KEY)
      .map(Boolean::parseBoolean)
      .orElse(BSLCommunityProperties.LANG_SERVER_OVERRIDE_CONFIGURATION_DEFAULT_VALUE);

    var configuration = BSLLSBinding.getLanguageServerConfiguration();
    if (overrideConfiguration) {
      String configurationPath = context.config()
        .get(BSLCommunityProperties.LANG_SERVER_CONFIGURATION_PATH_KEY)
        .orElse(BSLCommunityProperties.LANG_SERVER_CONFIGURATION_PATH_DEFAULT_VALUE);

      File configurationFile = new File(configurationPath);
      if (configurationFile.exists()) {
        LOGGER.info("BSL LS configuration file exists. Overriding SonarQube rules' settings...");
        configuration.update(configurationFile);
        return configuration;
      } else {
        LOGGER.error("Can't find bsl configuration file {}. Using SonarQube config instead.", configurationPath);
      }
    }

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
    Collection<DiagnosticInfo> diagnosticInfos = BSLLSBinding.getDiagnosticInfos();

    for (DiagnosticInfo diagnosticInfo : diagnosticInfos) {
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

  private static Object castDiagnosticParameterValue(String valueToCast, Class<?> type) {
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
