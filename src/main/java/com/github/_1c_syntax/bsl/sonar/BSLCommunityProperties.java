/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2020
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
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import com.github._1c_syntax.bsl.languageserver.configuration.diagnostics.ComputeTrigger;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public final class BSLCommunityProperties {

  public static final String LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY = "sonar.bsl.languageserver.diagnosticLanguage";
  public static final String LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_KEY = "sonar.bsl.languageserver.skipSupport";
  public static final String LANG_SERVER_ENABLED_KEY = "sonar.bsl.languageserver.enabled";
  public static final String LANG_SERVER_CONFIGURATION_PATH_KEY = "sonar.bsl.languageserver.configurationPath";
  public static final String LANG_SERVER_OVERRIDE_CONFIGURATION_KEY = "sonar.bsl.languageserver.overrideConfiguration";
  public static final String LANG_SERVER_REPORT_PATH_KEY = "sonar.bsl.languageserver.reportPaths";
  public static final String BSL_FILE_EXTENSIONS_KEY = "sonar.bsl.file.suffixes";
  public static final String BSL_CALCULATE_LINE_TO_COVER_KEY = "sonar.bsl.calculateLineCover";

  public static final Boolean LANG_SERVER_ENABLED_DEFAULT_VALUE = Boolean.TRUE;
  public static final String LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE = Language.RU.getLanguageCode();
  public static final String LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_DEFAULT_VALUE
    = ComputeTrigger.NEVER.name().toLowerCase(Locale.ENGLISH);

  public static final String LANG_SERVER_CONFIGURATION_PATH_DEFAULT_VALUE = ".bsl-language-server.json";
  public static final Boolean LANG_SERVER_OVERRIDE_CONFIGURATION_DEFAULT_VALUE = Boolean.FALSE;
  public static final String BSL_FILE_EXTENSIONS_DEFAULT_VALUE = ".bsl,.os";
  public static final Boolean BSL_CALCULATE_LINE_TO_COVER_VALUE = Boolean.FALSE;

  private static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";
  private static final String BSL_CATEGORY = "1C (BSL)";
  private static final String BSL_SUBCATEGORY = "1C (BSL) Community";


  private BSLCommunityProperties() {
    // only statics
  }

  public static List<PropertyDefinition> getProperties() {
    return Arrays.asList(
      PropertyDefinition.builder(LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY)
        .name("BSL Language server rule names and messages language")
        .description(
          "Defines the language of the rules names (on APP level) and language of rules message (on PROJECT level)"
        )
        .defaultValue(LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE)
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Language.RU.getLanguageCode(), Language.EN.getLanguageCode())
        .category(BSL_CATEGORY)
        .onQualifiers(Qualifiers.APP, Qualifiers.PROJECT)
        .index(0)
        .build(),
      PropertyDefinition.builder(LANG_SERVER_ENABLED_KEY)
        .name("BSL Language Server enabled")
        .description("Run internal BSL Language Server Diagnostic Provider")
        .defaultValue(LANG_SERVER_ENABLED_DEFAULT_VALUE.toString())
        .type(PropertyType.BOOLEAN)
        .category(BSL_CATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .index(1)
        .build(),
      PropertyDefinition.builder(LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_KEY)
        .name("BSL Language Server - Skip computing diagnostics on modules with parent configurations")
        .description("Skip computing diagnostics according to module's support mode " +
          "(if there is a parent configuration).")
        .category(BSL_CATEGORY)
        .defaultValue(LANG_SERVER_COMPUTE_DIAGNOSTICS_SKIP_SUPPORT_DEFAULT_VALUE)
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Stream.of(ComputeTrigger.values())
          .map(value -> value.name().toLowerCase(Locale.ENGLISH).replace("_", " "))
          .collect(Collectors.toList())
        )
        .onQualifiers(Qualifiers.PROJECT)
        .index(2)
        .build(),
      PropertyDefinition.builder(LANG_SERVER_OVERRIDE_CONFIGURATION_KEY)
        .name("BSL Language Server - Use configuration file")
        .description("Override SonarQube settings with BSL LS configuration file.")
        .category(BSL_CATEGORY)
        .defaultValue(LANG_SERVER_OVERRIDE_CONFIGURATION_DEFAULT_VALUE.toString())
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .index(3)
        .build(),
      PropertyDefinition.builder(LANG_SERVER_CONFIGURATION_PATH_KEY)
        .name("BSL Language Server - Configuration file")
        .description("Path to BSL LS configuration file.")
        .category(BSL_CATEGORY)
        .defaultValue(LANG_SERVER_CONFIGURATION_PATH_DEFAULT_VALUE)
        .type(PropertyType.STRING)
        .onQualifiers(Qualifiers.PROJECT)
        .index(4)
        .build(),
      PropertyDefinition.builder(BSL_FILE_EXTENSIONS_KEY)
        .name("BSL File suffixes")
        .description("List of file suffixes that will be scanned.")
        .category(BSL_CATEGORY)
        .defaultValue(BSL_FILE_EXTENSIONS_DEFAULT_VALUE)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .index(5)
        .build(),
      PropertyDefinition.builder(BSL_CALCULATE_LINE_TO_COVER_KEY)
        .name("BSL calculate lines to cover")
        .description("Calculate lines to cover on analise")
        .defaultValue(BSL_CALCULATE_LINE_TO_COVER_VALUE.toString())
        .type(PropertyType.BOOLEAN)
        .category(BSL_CATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .index(6)
        .build(),
      PropertyDefinition.builder(LANG_SERVER_REPORT_PATH_KEY)
        .name("BSL Language Server Report Files")
        .description("Paths (absolute or relative) to xml files with BSL Language Server diagnostics")
        .defaultValue("")
        .category(EXTERNAL_ANALYZERS_CATEGORY)
        .subCategory(BSL_SUBCATEGORY)
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .index(0)
        .build()
    );
  }

}
