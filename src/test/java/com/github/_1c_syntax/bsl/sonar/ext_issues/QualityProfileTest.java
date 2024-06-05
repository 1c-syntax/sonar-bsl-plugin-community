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

import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class QualityProfileTest {

  private static final File BASE_DIR = new File("src/test/resources").getAbsoluteFile();
  private static final String ACC_TEST_RULES_FILE
    = new File(BASE_DIR, "examples/acc-test.json").getAbsolutePath();
  private static final String ACC_TEST2_RULES_FILE
    = new File(BASE_DIR, "examples/acc-test-second.json").getAbsolutePath();

  @Test
  void testQualityProfile() {
    var config = new MapSettings()
      .setProperty(AccReporter.create().getRulesPathsKey(), ACC_TEST_RULES_FILE + "," + ACC_TEST2_RULES_FILE)
      .asConfig();
    var profile = new QualityProfilesContainer(config);
    var context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);
    assertThat(context.profilesByLanguageAndName().get(BSLLanguage.KEY)).isNull();
  }

  @Test
  void testQualityProfileEnabled() {
    var properties = AccReporter.create();
    var config = new MapSettings()
      .setProperty(properties.getEnabledKey(), true)
      .setProperty(properties.getRulesPathsKey(), ACC_TEST_RULES_FILE + "," + ACC_TEST2_RULES_FILE)
      .asConfig();
    var profile = new QualityProfilesContainer(config);
    var context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);
    assertThat(context.profilesByLanguageAndName().get(BSLLanguage.KEY)).hasSize(3);
  }

  @Test
  void testQualityProfileEnabledWithoutFiles() {
    var properties = AccReporter.create();
    var config = new MapSettings()
      .setProperty(properties.getEnabledKey(), true)
      .asConfig();
    var profile = new QualityProfilesContainer(config);
    var context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);
    assertThat(context.profilesByLanguageAndName().get(BSLLanguage.KEY)).hasSize(3);
  }
}
