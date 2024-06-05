/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2024
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
public class EdtReporter implements Reporter {
  String name = "1C:EDT";
  String subcategory = "EDT";
  String enabledKey = "sonar.bsl.edt.enabled";
  boolean enableDefaultValue = false;
  String createExternalIssuesKey = "sonar.bsl.edt.createExternalIssues";
  boolean createExternalIssuesDefaultValue = true;
  String rulesPathsKey = "sonar.bsl.edt.rulesPaths";
  String rulesDefaultPath = "edt.json";
  String repositoryKey = "edt-rules";
  String source = "edt";
  String repositoryName = "EDT rules";
  String ruleTag = "edt";
  int startIndex = 35;
  boolean include1CCertifiedProfile = true;
}
