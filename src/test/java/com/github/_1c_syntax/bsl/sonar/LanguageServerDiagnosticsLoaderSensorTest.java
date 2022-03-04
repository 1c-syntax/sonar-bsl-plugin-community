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

import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageServerDiagnosticsLoaderSensorTest {

  private final String BASE_PATH = "src/test/resources/examples";
  private final File BASE_DIR = new File(BASE_PATH);

  @Test
  void test_describe() {

    var context = SensorContextTester.create(BASE_DIR);
    var diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
    var sensorDescriptor = new DefaultSensorDescriptor();
    diagnosticsLoaderSensor.describe(sensorDescriptor);

    assertThat(sensorDescriptor.name()).containsIgnoringCase("BSL Language Server diagnostics loader");
  }

  @Test
  void test_execute() {

    var FILE_NAME = "test.bsl";
    var inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR);

    var sonarRuntime = SonarRuntimeImpl.forSonarLint(Version.create(7, 9));
    var context = SensorContextTester.create(BASE_DIR);
    context.setRuntime(sonarRuntime);
    context.settings().setProperty(
      "sonar.bsl.languageserver.reportPaths",
      "bsl-json.json, bsl-json2.json, empty.json, empty2.json");
    context.fileSystem().add(inputFile);

    var activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, "OneStatementPerLine"))
        .setName("OneStatementPerLine")
        .build())
      .build();
    context.setActiveRules(activeRules);

    var diagnosticsLoaderSensor = new LanguageServerDiagnosticsLoaderSensor(context);
    diagnosticsLoaderSensor.execute(context);

    assertThat(context.isCancelled()).isFalse();

  }
}
