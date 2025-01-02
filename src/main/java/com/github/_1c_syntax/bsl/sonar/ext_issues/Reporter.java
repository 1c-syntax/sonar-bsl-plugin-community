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
package com.github._1c_syntax.bsl.sonar.ext_issues;

import com.github._1c_syntax.bsl.sonar.PropertyDefinitionUtils;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;

/**
 * Общий интерфейс для настроек импортеров
 */
public interface Reporter {

  /**
   * Пользовательское имя
   */
  String getName();

  /**
   * Подкатегория настроек
   */
  String getSubcategory();

  /**
   * Ключ настроек для активизации импортера
   */
  String getEnabledKey();

  /**
   * Значение ключа для активизации импортера по умолчанию
   */
  boolean isEnableDefaultValue();

  /**
   * Ключ настроек для создания "внешние замечаний"
   */
  String getCreateExternalIssuesKey();

  /**
   * Значение по умолчанию для создания "внешние замечаний"
   */
  boolean isCreateExternalIssuesDefaultValue();

  /**
   * Ключ настройки путей файлов с описанием диагностик
   */
  String getRulesPathsKey();

  /**
   * Путь к поставляемому файлу описания диагностик
   */
  String getRulesDefaultPath();

  /**
   * Ключ репозитория диагностик
   */
  String getRepositoryKey();

  /**
   * Ключ-идентификатор репортера
   */
  String getSource();

  /**
   * Пользовательское представление репозитория диагностик
   */
  String getRepositoryName();

  /**
   * Имя тега, указываемого диагностикам при загрузке
   */
  String getRuleTag();

  /**
   * Стартовый индекс расположения настроек в UI SQ
   */
  int getStartIndex();

  /**
   * Признак необходимости создания профиля с диагностиками для 1С:Совместимо
   */
  boolean isInclude1CCertifiedProfile();

  /**
   * Добавляет в контекст плагина параметры репортера для отображения в UI SQ
   */
  default void addExtension(Plugin.Context context) {
    var index = getStartIndex();
    Arrays.asList(
      PropertyDefinitionUtils.newPropertyBuilderReport(++index,
          getEnabledKey(),
          "enabled",
          getName(),
          getSubcategory()
        )
        .defaultValue(Boolean.toString(isEnableDefaultValue()))
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.APP)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderReport(++index,
          getCreateExternalIssuesKey(),
          "createExternalIssues",
          getName(),
          getSubcategory()
        )
        .defaultValue(Boolean.toString(isCreateExternalIssuesDefaultValue()))
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.APP, Qualifiers.PROJECT)
        .build(),
      PropertyDefinitionUtils.newPropertyBuilderReport(++index,
          getRulesPathsKey(),
          "rulePath",
          getName(),
          getSubcategory()
        )
        .defaultValue("")
        .type(PropertyType.STRING)
        .onQualifiers(Qualifiers.APP)
        .multiValues(true)
        .build()
    ).forEach(context::addExtension);
  }
}
