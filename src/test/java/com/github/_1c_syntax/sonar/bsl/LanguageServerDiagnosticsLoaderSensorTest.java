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
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class LanguageServerDiagnosticsLoaderSensorTest {

    final String BASE_PATH = "src/test/files/src";

    @Test
    public void test_describe() {

        File baseDir = new File(BASE_PATH);
        SensorContextTester context = SensorContextTester.create(baseDir);
        LanguageServerDiagnosticsLoaderSensor diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
        DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
        diagnosticsLoaderSensor.describe(sensorDescriptor);

        assertThat(sensorDescriptor.name()).containsIgnoringCase("BSL Language Server diagnostics loader");

    }

    @Test
    public void test_execute() {

        File baseDir = new File(BASE_PATH);

        SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarLint(Version.create(7, 9));
        SensorContextTester context = SensorContextTester.create(baseDir);
        context.setRuntime(sonarRuntime);


        context.settings().setProperty("sonar.bsl.languageserver.reportPaths", "bsl-json.json, empty.json, empty2.json");
       // context.settings().setProperty("sonar.bsl.languageserver.reportPaths", "empty.json, empty2.json");

        InputFile inputFile = inputFile("Test.bsl", baseDir);
        context.fileSystem().add(inputFile);

        ActiveRules activeRules = new ActiveRulesBuilder()
                .addRule(new NewActiveRule.Builder()
                        .setRuleKey(RuleKey.of(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, "OneStatementPerLine"))
                        .setName("OneStatementPerLine")
                        .build())
                .build();
        context.setActiveRules(activeRules);

        LanguageServerDiagnosticsLoaderSensor diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
        diagnosticsLoaderSensor.execute(context);

    }

    private InputFile inputFile(String name, File baseDir) {

        File file = new File(baseDir.getPath(), name);
        String content;
        try {
            content = readFile(file.toPath().toString());
        } catch (IOException e) {
            content = "Значение = 1; Значение2 = 1;";
        }


        DefaultInputFile inputFile = TestInputFileBuilder.create("moduleKey", name)
                .setModuleBaseDir(baseDir.toPath())
                .setCharset(StandardCharsets.UTF_8)
                .setType(InputFile.Type.MAIN)
                .setLanguage(BSLLanguage.KEY)
                .initMetadata(content)
                .build();
        return inputFile;
    }

    private String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

}
