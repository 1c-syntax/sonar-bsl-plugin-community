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

import com.github._1c_syntax.bsl.sonar.ext_issues.AccReporter;
import com.github._1c_syntax.bsl.sonar.ext_issues.EdtReporter;
import com.github._1c_syntax.bsl.sonar.ext_issues.QualityProfilesContainer;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import com.github._1c_syntax.bsl.sonar.language.BSLQualityProfile;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.Version;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BSLPluginTest {

  private static final Version VERSION_8_9 = Version.create(8, 9);
  private final BSLPlugin bslPlugin = new BSLPlugin();

  @Test
  void testGetExtensions() {

    LanguageClientBinding.INSTANCE.start();

    var runtime = SonarRuntimeImpl.forSonarQube(VERSION_8_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    var context = new Plugin.Context(runtime);
    bslPlugin.define(context);
    assertThat((List<?>) context.getExtensions()).hasSize(23);
  }

  @Test
  void testQualityProfile() {
    var profile = new BSLQualityProfile();
    var context = new BuiltInQualityProfilesDefinition.Context();
    profile.define(context);
    assertThat(context.profilesByLanguageAndName().get(BSLLanguage.KEY)).hasSize(1);
  }

  @Test
  void testQualityProfileAll() {
    var runtime = SonarRuntimeImpl.forSonarQube(VERSION_8_9, SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    var config = new MapSettings()
      .setProperty(AccReporter.create().getEnabledKey(), true)
      .setProperty(EdtReporter.create().getEnabledKey(), true)
      .asConfig();
    var context = new PluginContextImpl.Builder()
      .setSonarRuntime(runtime)
      .setBootConfiguration(config)
      .build();

    bslPlugin.define(context);

    var profile = new QualityProfilesContainer(config);
    var contextProfile = new BuiltInQualityProfilesDefinition.Context();
    profile.define(contextProfile);
    assertThat(contextProfile.profilesByLanguageAndName().get(BSLLanguage.KEY)).hasSize(5);
  }

}
