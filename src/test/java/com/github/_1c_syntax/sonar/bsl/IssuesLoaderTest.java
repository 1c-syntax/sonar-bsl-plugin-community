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

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuesLoaderTest {

    private final String BASE_PATH = "src/test/resources/src";
    private final File BASE_DIR = new File(BASE_PATH).getAbsoluteFile();
    private final String FILE_NAME = "test.bsl";

    @Test
    public void test_createIssue() {

        final String issueCode = "Test";
        final DiagnosticSeverity issueSeverity = DiagnosticSeverity.Information;

        SensorContextTester context = SensorContextTester.create(BASE_DIR);
        InputFile inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR);
        IssuesLoader issuesLoader = new IssuesLoader(context);

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setCode(issueCode);
        diagnostic.setSeverity(issueSeverity);
        diagnostic.setMessage("Check message");
        diagnostic.setRange(new Range(new Position(2, 9), new Position(2, 19)));
        diagnostic.setRelatedInformation(null);

        issuesLoader.createIssue(inputFile, diagnostic);

        assertThat(context.allExternalIssues()).hasSize(1);
        DefaultExternalIssue issue = (DefaultExternalIssue) context.allExternalIssues().toArray()[0];
        assertThat(issue.ruleId()).isEqualTo(issueCode);

    }

}
