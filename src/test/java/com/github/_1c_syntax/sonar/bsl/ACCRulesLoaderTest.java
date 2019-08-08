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

import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.rule.RulesDefinition;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ACCRulesLoaderTest {

  private final String BASE_PATH = "src/test/resources/src";
  private final File BASE_DIR = new File(BASE_PATH).getAbsoluteFile();
  private final String FILE_NAME = "acc.json";

  @Test
  public void test_define() {

    File fileRules = new File(BASE_DIR, FILE_NAME);
    Configuration config = new MapSettings()
      .setProperty(BSLCommunityProperties.LANG_SERVER_ACCRULES_PATH, fileRules.getAbsolutePath())
      .asConfig();
    ACCRulesLoader ruleDefinition = new ACCRulesLoader(config);
    RulesDefinition.Context context = new RulesDefinition.Context();
    ruleDefinition.define(context);
    assertThat(context.repository(ACCRulesLoader.REPOSITORY_KEY)).isNotNull();
  }
}
