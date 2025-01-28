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

import lombok.experimental.UtilityClass;
import org.sonar.api.config.PropertyDefinition;

import java.util.ResourceBundle;

/**
 * Вспомогательный класс для создания параметров плагина с выводом в UI
 */
@UtilityClass
public class PropertyDefinitionUtils {
  private static final String PREFIX_KEY = "communitybsl";
  private static final ResourceBundle L10N_BUNDLE = ResourceBundle.getBundle("org.sonar.l10n.communitybsl");
  private static final String EXTERNAL_ANALYZERS_CATEGORY = "External Analyzers";

  public static PropertyDefinition.Builder newPropertyBuilderBSL(int index,
                                                                 String key,
                                                                 String l10nKey,
                                                                 String defaultValue) {
    return newPropertyBuilder(index, key, l10nKey, defaultValue, BSLCommunityProperties.BSL_CATEGORY);
  }

  public static PropertyDefinition.Builder newPropertyBuilderExternal(int index,
                                                                      String key,
                                                                      String l10nKey,
                                                                      String defaultValue) {
    return newPropertyBuilder(index, key, l10nKey, defaultValue, EXTERNAL_ANALYZERS_CATEGORY);
  }

  public static PropertyDefinition.Builder newPropertyBuilderReport(int index,
                                                                    String key,
                                                                    String l10nKey,
                                                                    String addString,
                                                                    String subCategory) {
    return PropertyDefinition.builder(key)
      .name(getResourceString("report." + l10nKey + ".name", addString))
      .description(getResourceString("report." + l10nKey + ".description", addString))
      .category(BSLCommunityProperties.BSL_CATEGORY)
      .subCategory(subCategory)
      .index(index);
  }

  private static PropertyDefinition.Builder newPropertyBuilder(int index,
                                                               String key,
                                                               String l10nKey,
                                                               String defaultValue,
                                                               String category) {
    return PropertyDefinition.builder(key)
      .name(getResourceString(l10nKey + ".name"))
      .description(getResourceString(l10nKey + ".description"))
      .defaultValue(defaultValue)
      .category(category)
      .index(index);
  }

  private static String getResourceString(String key) {
    return L10N_BUNDLE.getString(PREFIX_KEY + "." + key);
  }

  private static String getResourceString(String key, Object... args) {
    return getResourceString(key).formatted(args);
  }
}
