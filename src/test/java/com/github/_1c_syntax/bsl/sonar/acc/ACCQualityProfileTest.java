/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2021
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
package com.github._1c_syntax.bsl.sonar.acc;

import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ACCQualityProfileTest {

  @Test
  void testQualityProfile() {
    File baseDir = new File("src/test/resources").getAbsoluteFile();
    File fileRules = new File(baseDir, "acc-test.json");
    File fileRulesSecond = new File(baseDir, "acc-test-second.json");
    Configuration config = new MapSettings()
      .setProperty(ACCProperties.ACC_RULES_PATHS, fileRules.getAbsolutePath() + "," + fileRulesSecond.getAbsolutePath())
      .asConfig();
    ACCQualityProfile profile = new ACCQualityProfile(config);
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);
    assertThat(context.profilesByLanguageAndName().get(BSLLanguage.KEY)).isNull();
  }

  @Test
  void testQualityProfileEnabled() {
    File baseDir = new File("src/test/resources").getAbsoluteFile();
    File fileRules = new File(baseDir, "acc-test.json");
    File fileRulesSecond = new File(baseDir, "acc-test-second.json");
    Configuration config = new MapSettings()
      .setProperty(ACCProperties.ACC_ENABLED, true)
      .setProperty(ACCProperties.ACC_RULES_PATHS, fileRules.getAbsolutePath() + "," + fileRulesSecond.getAbsolutePath())
      .asConfig();
    ACCQualityProfile profile = new ACCQualityProfile(config);
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);
    assertThat(context.profilesByLanguageAndName().get(BSLLanguage.KEY)).hasSize(3);
  }

}
