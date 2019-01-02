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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._1c_syntax.sonar.bsl.language.BSLLanguage;
import org.apache.commons.io.FileUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.github._1c_syntax.intellij.bsl.lsp.server.diagnostics.FileInfo;
import org.github._1c_syntax.intellij.bsl.lsp.server.diagnostics.reporter.AnalysisInfo;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LanguageServerDiagnosticsLoaderSensor implements Sensor {

    private final SensorContext context;
    private final Map<DiagnosticSeverity, Severity> severityMap;

    private static final Logger LOGGER = Loggers.get(LanguageServerDiagnosticsLoaderSensor.class);

    public LanguageServerDiagnosticsLoaderSensor(final SensorContext context) {
        this.context = context;
        this.severityMap = createDiagnosticSeverityMap();
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.onlyOnLanguage(BSLLanguage.KEY);
        descriptor.name("BSL Language Server diagnostics loader");
    }

    @Override
    public void execute(SensorContext context) {
        String reportPath = getReportPath();
        if (reportPath.equals("")) {
            return;
        }

        File analysisResultsFile = new File(reportPath);
        parseAndSaveResults(analysisResultsFile);
    }

    private void parseAndSaveResults(File analysisResultsFile) {
        LOGGER.info("Parsing 'BSL Language Server' analysis results");

        AnalysisInfo analysisInfo = getAnalysisInfo(analysisResultsFile);
        if (analysisInfo == null) {
            return;
        }

        List<FileInfo> fileinfos = analysisInfo.getFileinfos();
        for (FileInfo fileInfo : fileinfos) {
            processFileInfo(fileInfo);
        }
    }

    private void processFileInfo(FileInfo fileInfo) {
        FileSystem fileSystem = context.fileSystem();
        Path path = fileInfo.getPath();
        InputFile inputFile = fileSystem.inputFile(
                fileSystem.predicates().and(
                        fileSystem.predicates().hasLanguage(BSLLanguage.KEY),
                        fileSystem.predicates().hasAbsolutePath(path.toAbsolutePath().toString())
                )
        );

        if (inputFile == null) {
            LOGGER.warn("Can't find inputFile for absolute path", path);
            return;
        }

        Diagnostic[] diagnostics = fileInfo.getDiagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            processDiagnostic(inputFile, diagnostic);
        }
    }

    private void processDiagnostic(InputFile inputFile, Diagnostic diagnostic) {
        NewExternalIssue issue = context.newExternalIssue();

        Range range = diagnostic.getRange();
        Position start = range.getStart();
        Position end = range.getEnd();
        TextRange textRange = inputFile.newRange(
                start.getLine() + 1,
                start.getCharacter(),
                end.getLine() + 1,
                end.getCharacter()
        );

        NewIssueLocation location = issue.newLocation();
        location.on(inputFile);
        location.at(textRange);
        location.message(diagnostic.getMessage());

        issue.engineId("bsl-language-server");
        issue.ruleId(diagnostic.getCode());
        issue.type(RuleType.CODE_SMELL);
        issue.severity(severityMap.get(diagnostic.getSeverity()));
        issue.at(location);

        issue.save();
    }


    private String getReportPath() {
        Optional<String> reportPath = context.config().get(BSLCommunityProperties.LANG_SERVER_REPORT_PATH_KEY);
        return reportPath.orElse("");
    }

    @Nullable
    private AnalysisInfo getAnalysisInfo(File analysisResultsFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(df);

        String json;
        try {
            json = FileUtils.readFileToString(analysisResultsFile, Charset.forName("UTF-8"));
        } catch (IOException e) {
            LOGGER.error("Can't read analysis report file", e);
            return null;
        }

        try {
            return objectMapper.readValue(json, AnalysisInfo.class);
        } catch (IOException e) {
            LOGGER.error("Can't parse analysis report file", e);
            return null;
        }
    }

    private Map<DiagnosticSeverity, Severity> createDiagnosticSeverityMap() {
        Map<DiagnosticSeverity, Severity> map = new EnumMap<>(DiagnosticSeverity.class);
        map.put(DiagnosticSeverity.Warning, Severity.MAJOR);
        map.put(DiagnosticSeverity.Information, Severity.MINOR);
        map.put(DiagnosticSeverity.Hint, Severity.INFO);
        map.put(DiagnosticSeverity.Error, Severity.CRITICAL);

        return map;
    }
}
