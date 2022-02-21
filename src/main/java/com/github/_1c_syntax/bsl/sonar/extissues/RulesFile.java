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
package com.github._1c_syntax.bsl.sonar.extissues;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.List;

/**
 * DTO для чтения списка описаний диагностик из файла json
 */
@Value
@AllArgsConstructor
public class RulesFile {

  List<Rule> rules;

  /**
   * DTO для описания самих описаний диагностик в файле
   */
  @Value
  public static class Rule {
    /**
     * Код диагностики (обязательный)
     */
    String code;

    /**
     * Имя диагностики (обязательный)
     */
    String name;

    /**
     * Описание диагностики
     */
    String description;

    /**
     * Тип диагностики
     */
    String type;

    /**
     * Важность диагностики
     */
    String severity;

    /**
     * Признак активизации по умолчанию
     */
    boolean active;

    /**
     * Признак вхождения диагностики в профиль обязательных для сертификации
     */
    boolean needForCertificate;

    /**
     * Время на исправление (в минутах)
     */
    int effortMinutes;
  }
}
