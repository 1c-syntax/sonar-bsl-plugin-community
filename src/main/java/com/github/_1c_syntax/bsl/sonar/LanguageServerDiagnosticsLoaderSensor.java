/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2025
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

import com.github._1c_syntax.bsl.languageserver.reporters.data.AnalysisInfo;
import com.github._1c_syntax.bsl.languageserver.reporters.data.FileInfo;
import com.github._1c_syntax.bsl.languageserver.reporters.databind.AnalysisInfoObjectMapper;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarsource.analyzer.commons.ExternalReportProvider;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static com.github._1c_syntax.bsl.sonar.BSLCommunityProperties.LANG_SERVER_REPORT_PATH_KEY;

public class LanguageServerDiagnosticsLoaderSensor implements Sensor {

  private static final Logger LOGGER = Loggers.get(LanguageServerDiagnosticsLoaderSensor.class);

  private final SensorContext context;
  private final IssuesLoader issueLoader;
  private FileSystem fileSystem;
  private FilePredicates predicates;

  public LanguageServerDiagnosticsLoaderSensor(final SensorContext context) {
    this.context = context;
    this.issueLoader = new IssuesLoader(context);
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(BSLLanguage.KEY);
    descriptor.name("BSL Language Server diagnostics loader");
  }

  @Override
  public void execute(SensorContext context) {
    var reportFiles = ExternalReportProvider.getReportFiles(context, LANG_SERVER_REPORT_PATH_KEY);
    reportFiles.forEach(this::parseAndSaveResults);
  }

  private void parseAndSaveResults(File analysisResultsFile) {
    LOGGER.info("Parsing 'BSL Language Server' analysis results:");
    LOGGER.info(analysisResultsFile.getAbsolutePath());

    var analysisInfo = getAnalysisInfo(analysisResultsFile);
    if (analysisInfo == null) {
      return;
    }

    var fileinfos = analysisInfo.getFileinfos();
    for (var fileInfo : fileinfos) {
      processFileInfo(fileInfo);
    }
  }

  private void processFileInfo(FileInfo fileInfo) {
    fileSystem = context.fileSystem();
    var path = fileInfo.getPath();
    predicates = fileSystem.predicates();

    var inputFile = getInputFile(path);
    if (inputFile == null) {
      LOGGER.warn("Can't find inputFile for absolute path {}", path);
      return;
    }

    var diagnostics = fileInfo.getDiagnostics();
    diagnostics.forEach((Diagnostic diagnostic) -> processDiagnostic(inputFile, diagnostic));
  }

  private void processDiagnostic(InputFile inputFile, Diagnostic diagnostic) {
    issueLoader.createIssue(inputFile, diagnostic);
  }

  @CheckForNull
  private InputFile getInputFile(Path path) {
    return fileSystem.inputFile(
      predicates.and(
        predicates.hasLanguage(BSLLanguage.KEY),
        predicates.hasAbsolutePath(path.toAbsolutePath().toString())
      )
    );
  }

  @Nullable
  private static AnalysisInfo getAnalysisInfo(File analysisResultsFile) {

    String json;
    try {
      json = FileUtils.readFileToString(analysisResultsFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Can't read analysis report file", e);
      return null;
    }

    var objectMapper = new AnalysisInfoObjectMapper();

    try {
      return objectMapper.readValue(json, AnalysisInfo.class);
    } catch (IOException e) {
      LOGGER.error("Can't parse analysis report file", e);
      return null;
    }
  }
}
