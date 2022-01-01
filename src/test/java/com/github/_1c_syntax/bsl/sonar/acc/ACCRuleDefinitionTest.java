/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2022
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
package com.github._1c_syntax.bsl.sonar.acc;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.rule.RulesDefinition;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ACCRuleDefinitionTest {

  @Test
  void testDefine() {
    Configuration config = new MapSettings()
      .setProperty(ACCProperties.ACC_ENABLED, true)
      .asConfig();
    ACCRuleDefinition ruleDefinition = new ACCRuleDefinition(config);
    RulesDefinition.Context context = new RulesDefinition.Context();
    ruleDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository(ACCRuleDefinition.REPOSITORY_KEY);
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(467);
  }

  @Test
  void testEmptyExternalFilePath() {
    Configuration config = new MapSettings()
      .setProperty(ACCProperties.ACC_ENABLED, true)
      .setProperty(ACCProperties.ACC_RULES_PATHS, "")
      .asConfig();
    ACCRuleDefinition ruleDefinition = new ACCRuleDefinition(config);
    RulesDefinition.Context context = new RulesDefinition.Context();
    ruleDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository(ACCRuleDefinition.REPOSITORY_KEY);
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(467);
  }

  @Test
  void testExternalFile() {
    File baseDir = new File("src/test/resources").getAbsoluteFile();
    File fileRules = new File(baseDir, "acc-test.json");
    File fileRulesSecond = new File(baseDir, "acc-test-second.json");
    Configuration config = new MapSettings()
      .setProperty(ACCProperties.ACC_ENABLED, true)
      .setProperty(ACCProperties.ACC_RULES_PATHS, fileRules.getAbsolutePath() + "," + fileRulesSecond.getAbsolutePath())
      .asConfig();
    ACCRuleDefinition ruleDefinition = new ACCRuleDefinition(config);
    RulesDefinition.Context context = new RulesDefinition.Context();
    ruleDefinition.define(context);

    assertThat(context.repositories()).hasSize(1);
    RulesDefinition.Repository repository = context.repository(ACCRuleDefinition.REPOSITORY_KEY);
    assertThat(repository).isNotNull();
    assertThat(repository.rules()).hasSize(469);
  }

}
