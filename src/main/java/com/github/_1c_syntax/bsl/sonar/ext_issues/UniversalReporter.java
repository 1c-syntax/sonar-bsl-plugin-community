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
package com.github._1c_syntax.bsl.sonar.ext_issues;

import lombok.Value;

/**
 * Настройки внешнего анализатора 1С:EDT
 */
@Value(staticConstructor = "create")
public class UniversalReporter implements Reporter {
  String name = "BSLLS Universal";
  String subcategory = "Universal";
  String enabledKey = "sonar.bsl.universal.enabled";
  boolean enableDefaultValue = false;
  String createExternalIssuesKey = "sonar.bsl.universal.createExternalIssues";
  boolean createExternalIssuesDefaultValue = false;
  String rulesPathsKey = "sonar.bsl.universal.rulesPaths";
  String rulesDefaultPath = "universal.json";
  String repositoryKey = "universal-rules";
  String source = "universal";
  String repositoryName = "Universal rules";
  String ruleTag = "bslls-universal";
  int startIndex = 45;
  boolean include1CCertifiedProfile = false;
}
