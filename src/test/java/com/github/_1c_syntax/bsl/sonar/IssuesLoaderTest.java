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

import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class IssuesLoaderTest {

    private final String BASE_PATH = "src/test/resources/src";
    private final File BASE_DIR = new File(BASE_PATH).getAbsoluteFile();
    private final String FILE_NAME = "test.bsl";

    @Test
    void test_createExtIssue() {

        final String issueCode = "Test";
        final DiagnosticSeverity issueSeverity = DiagnosticSeverity.Information;

        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        InputFile inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR);
        IssuesLoader issuesLoader = new IssuesLoader(context);

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode(issueCode);
        diagnostic.setSeverity(issueSeverity);
        diagnostic.setMessage("Check message");
        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
        diagnostic.setRelatedInformation(null);

        issuesLoader.createIssue(inputFile, diagnostic);

        assertThat(context.allExternalIssues()).hasSize(1);
        DefaultExternalIssue issue = (DefaultExternalIssue) context.allExternalIssues().toArray()[0];
        assertThat(issue.ruleId()).isEqualTo(issueCode);

    }

    @Test
    void test_createIssue() {

        final DiagnosticSeverity issueSeverity = DiagnosticSeverity.Information;
        final String diagnosticName = "OneStatementPerLine";
        final RuleKey ruleKey = RuleKey.of(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, diagnosticName);

        SensorContextTester context = SensorContextTester.create(BASE_DIR);

        ActiveRules activeRules = new ActiveRulesBuilder()
                .addRule(new NewActiveRule.Builder()
                        .setRuleKey(ruleKey)
                        .setName(diagnosticName)
                        .build())
                .build();
        context.setActiveRules(activeRules);

        InputFile inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR);
        context.fileSystem().add(inputFile);

        IssuesLoader issuesLoader = new IssuesLoader(context);

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode(diagnosticName);
        diagnostic.setSeverity(issueSeverity);
        diagnostic.setMessage("Check message OneStatementPerLine");
        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));

        issuesLoader.createIssue(inputFile, diagnostic);

        assertThat(context.allIssues()).hasSize(1);
        DefaultIssue issue = (DefaultIssue) context.allIssues().toArray()[0];
        assertThat(issue.ruleKey()).isEqualTo(ruleKey);

    }
}
