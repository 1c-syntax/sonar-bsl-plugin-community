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
import com.github._1c_syntax.sonar.bsl.language.BSLQualityProfile;
import org.sonar.api.Plugin;

public class BSLPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtension(BSLLanguage.class);
    context.addExtension(BSLQualityProfile.class);

    context.addExtension(BSLLanguageServerRuleDefinition.class);

    context.addExtension(BSLCoreSensor.class);
    context.addExtension(LanguageServerDiagnosticsLoaderSensor.class);

    context.addExtensions(BSLCommunityProperties.getProperties());
  }
}
