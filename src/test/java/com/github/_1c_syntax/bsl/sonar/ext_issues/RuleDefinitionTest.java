/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2024
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
package com.github._1c_syntax.bsl.sonar.ext_issues;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.rule.RulesDefinition;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class RuleDefinitionTest {

  private final Reporter reporter = EdtReporter.create();

  @Test
  void testDefine() {

    var config = new MapSettings()
      .setProperty(reporter.getEnabledKey(), true)
      .asConfig();
    var ruleDefinition = new RuleDefinitionsContainer(config);
    var context = new RulesDefinition.Context();
    ruleDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    var repository = context.repository(reporter.getRepositoryKey());
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(179);
  }

  @Test
  void testEmptyExternalFilePath() {
    var config = new MapSettings()
      .setProperty(reporter.getEnabledKey(), true)
      .setProperty(reporter.getRulesPathsKey(), "")
      .asConfig();
    var ruleDefinition = new RuleDefinitionsContainer(config);
    var context = new RulesDefinition.Context();
    ruleDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    var repository = context.repository(reporter.getRepositoryKey());
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(179);
  }

  @Test
  void testExternalFile() {
    var baseDir = new File("src/test/resources").getAbsoluteFile();
    var fileRules = new File(baseDir, "examples/acc-test.json");
    var fileRulesSecond = new File(baseDir, "examples/acc-test-second.json");
    var fileRulesThird = new File(baseDir, "examples/edt-test.json");
    var config = new MapSettings()
      .setProperty(reporter.getEnabledKey(), true)
      .setProperty(reporter.getRulesPathsKey(), fileRules.getAbsolutePath()
        + "," + fileRulesSecond.getAbsolutePath()
        + "," + fileRulesThird.getAbsolutePath())
      .asConfig();
    var ruleDefinition = new RuleDefinitionsContainer(config);
    var context = new RulesDefinition.Context();
    ruleDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    var repository = context.repository(reporter.getRepositoryKey());
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(183);
  }

}
