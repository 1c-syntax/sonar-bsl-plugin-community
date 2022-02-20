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
package com.github._1c_syntax.bsl.sonar.acc;

import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import lombok.experimental.UtilityClass;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;

import static com.github._1c_syntax.bsl.sonar.BSLCommunityProperties.BSL_CATEGORY;

@UtilityClass
public class ACCProperties {

  private final String ACC_SUBCATEGORY = "ACC";

  public final String ACC_ENABLED = "sonar.bsl.acc.enabled";
  public final boolean ENABLE_ACC_DEFAULT_VALUE = false;
  public final String CREATE_EXTERNAL_ISSUES = "sonar.bsl.acc.createExternalIssues";
  public final boolean CREATE_EXTERNAL_ISSUES_DEFAULT_VALUE = true;
  public final String ACC_RULES_PATHS = "sonar.bsl.acc.accRulesPaths";

  public List<PropertyDefinition> getProperties() {
    return Arrays.asList(
      PropertyDefinition.builder(ACC_ENABLED)
        .name("Enable 1C:ACC rules")
        .description(
          "Enable 1C:ACC (1С:АПК) rules. Need restart server"
        )
        .defaultValue(Boolean.toString(ENABLE_ACC_DEFAULT_VALUE))
        .type(PropertyType.BOOLEAN)
        .options(Language.RU.getLanguageCode(), Language.EN.getLanguageCode())
        .category(BSL_CATEGORY)
        .subCategory(ACC_SUBCATEGORY)
        .onQualifiers(Qualifiers.APP)
        .index(31)
        .build(),
      PropertyDefinition.builder(CREATE_EXTERNAL_ISSUES)
        .name("Create external issues with acc sources")
        .description(
          "Create external issue if no active rule was found"
        )
        .defaultValue(Boolean.toString(CREATE_EXTERNAL_ISSUES_DEFAULT_VALUE))
        .type(PropertyType.BOOLEAN)
        .options(Language.RU.getLanguageCode(), Language.EN.getLanguageCode())
        .category(BSL_CATEGORY)
        .subCategory(ACC_SUBCATEGORY)
        .onQualifiers(Qualifiers.APP, Qualifiers.PROJECT)
        .index(32)
        .build(),
      PropertyDefinition.builder(ACC_RULES_PATHS)
        .name("1C:ACC rules path")
        .description(
          "Path (absolute or relative) to json file with 1C:ACC rules"
        )
        .defaultValue("")
        .type(PropertyType.STRING)
        .category(BSL_CATEGORY)
        .subCategory(ACC_SUBCATEGORY)
        .onQualifiers(Qualifiers.APP)
        .multiValues(true)
        .index(33)
        .build()
    );
  }
}
