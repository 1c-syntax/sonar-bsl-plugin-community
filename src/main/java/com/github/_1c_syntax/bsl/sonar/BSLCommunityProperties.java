/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2025
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

import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import com.github._1c_syntax.bsl.languageserver.configuration.diagnostics.SkipSupport;
import lombok.experimental.UtilityClass;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@UtilityClass
public class BSLCommunityProperties {

  public static final String LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY = "sonar.bsl.languageserver.diagnosticLanguage";
  public static final String LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_KEY = "sonar.bsl.languageserver.skipSupport";
  public static final String LANG_SERVER_ENABLED_KEY = "sonar.bsl.languageserver.enabled";
  public static final String LANG_SERVER_CONFIGURATION_PATH_KEY = "sonar.bsl.languageserver.configurationPath";
  public static final String LANG_SERVER_OVERRIDE_CONFIGURATION_KEY = "sonar.bsl.languageserver.overrideConfiguration";
  public static final String LANG_SERVER_REPORT_PATH_KEY = "sonar.bsl.languageserver.reportPaths";
  public static final String LANG_SERVER_SUBSYSTEM_FILTER_INCLUDE_KEY
    = "sonar.bsl.languageserver.subsystemsFilter.include";
  public static final String LANG_SERVER_SUBSYSTEM_FILTER_EXCLUDE_KEY
    = "sonar.bsl.languageserver.subsystemsFilter.exclude";
  public static final String BSL_FILE_EXTENSIONS_KEY = "sonar.bsl.file.suffixes";

  public static final Boolean LANG_SERVER_ENABLED_DEFAULT_VALUE = Boolean.TRUE;
  public static final String LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE = Language.RU.getLanguageCode();
  public static final String LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_DEFAULT_VALUE
    = SkipSupport.NEVER.name().toLowerCase(Locale.ENGLISH);

  public static final String LANG_SERVER_CONFIGURATION_PATH_DEFAULT_VALUE = ".bsl-language-server.json";
  public static final Boolean LANG_SERVER_OVERRIDE_CONFIGURATION_DEFAULT_VALUE = Boolean.FALSE;
  public static final String BSL_FILE_EXTENSIONS_DEFAULT_VALUE = ".bsl,.os";

  public static final String BSL_CATEGORY = "1C (BSL)";

  private static final String BSL_SUBCATEGORY = "1C (BSL) Community";

  public static List<PropertyDefinition> getProperties() {
    return Arrays.asList(
      PropertyDefinitionUtils.newPropertyBuilderBSL(0,
          LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY,
          "diagnosticLanguage",
          LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE)
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Language.RU.getLanguageCode(), Language.EN.getLanguageCode())
        .onQualifiers(Qualifiers.APP, Qualifiers.PROJECT)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(1,
          LANG_SERVER_ENABLED_KEY,
          "enabled",
          LANG_SERVER_ENABLED_DEFAULT_VALUE.toString())
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(2,
          LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_KEY,
          "skipSupport",
          LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_DEFAULT_VALUE)
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Stream.of(SkipSupport.values())
          .map(value -> value.name().toLowerCase(Locale.ENGLISH).replace("_", " "))
          .toList()
        )
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(3,
          LANG_SERVER_OVERRIDE_CONFIGURATION_KEY,
          "overrideConfiguration",
          LANG_SERVER_OVERRIDE_CONFIGURATION_DEFAULT_VALUE.toString())
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(4,
          LANG_SERVER_CONFIGURATION_PATH_KEY,
          "enabled.configurationPath",
          LANG_SERVER_CONFIGURATION_PATH_DEFAULT_VALUE)
        .type(PropertyType.STRING)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(5,
          LANG_SERVER_SUBSYSTEM_FILTER_INCLUDE_KEY,
          "subsystemfilter.include",
          "")
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(6,
          LANG_SERVER_SUBSYSTEM_FILTER_EXCLUDE_KEY,
          "subsystemfilter.exclude",
          "")
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderBSL(7,
          BSL_FILE_EXTENSIONS_KEY,
          "file.suffixes",
          BSL_FILE_EXTENSIONS_DEFAULT_VALUE)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderExternal(0,
          LANG_SERVER_REPORT_PATH_KEY,
          "reportPaths",
          "")
        .subCategory(BSL_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build()
    );
  }
}
