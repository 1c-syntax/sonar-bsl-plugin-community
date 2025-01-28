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

import lombok.Value;

/**
 * Настройки внешнего анализатора 1С:АПК
 */
@Value(staticConstructor = "create")
public class AccReporter implements Reporter {
  String name = "1C:ACC (1С:АПК)";
  String subcategory = "ACC";
  String enabledKey = "sonar.bsl.acc.enabled";
  boolean enableDefaultValue = false;
  String createExternalIssuesKey = "sonar.bsl.acc.createExternalIssues";
  boolean createExternalIssuesDefaultValue = true;
  String rulesPathsKey = "sonar.bsl.acc.accRulesPaths";
  String rulesDefaultPath = "acc.json";
  String repositoryKey = "acc-rules";
  String source = "acc";
  String repositoryName = "ACC rules";
  String ruleTag = "acc";
  int startIndex = 30;
  boolean include1CCertifiedProfile = true;
}
