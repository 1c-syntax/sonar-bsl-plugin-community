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

import lombok.Value;

/**
 * Настройки внешнего анализатора 1С:EDT
 */
@Value(staticConstructor = "create")
public class UniversalReporter implements Reporter {

  private static final String NAME = "BSLLS Universal";

  private static final String SUBCATEGORY = "Universal";
  private static final String ENABLED_KEY = "sonar.bsl.universal.enabled";
  private static final boolean ENABLE_DEFAULT_VALUE = false;
  private static final String CREATE_EXTERNAL_ISSUES_KEY = "sonar.bsl.universal.createExternalIssues";
  private static final boolean CREATE_EXTERNAL_ISSUES_DEFAULT_VALUE = false;
  private static final String RULES_PATHS_KEY = "sonar.bsl.universal.rulesPaths";
  private static final String RULES_DEFAULT_PATH = "universal.json";
  private static final String REPOSITORY_KEY = "universal-rules";
  private static final String SOURCE = "universal";
  private static final String REPOSITORY_NAME = "Universal rules";
  private static final String RULE_TAG = "bslls-universal";

  private static final int START_INDEX = 45;

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public String subcategory() {
    return SUBCATEGORY;
  }

  @Override
  public String enabledKey() {
    return ENABLED_KEY;
  }

  @Override
  public boolean enableDefaultValue() {
    return ENABLE_DEFAULT_VALUE;
  }

  @Override
  public String createExternalIssuesKey() {
    return CREATE_EXTERNAL_ISSUES_KEY;
  }

  @Override
  public boolean createExternalIssuesDefaultValue() {
    return CREATE_EXTERNAL_ISSUES_DEFAULT_VALUE;
  }

  @Override
  public String rulesPathsKey() {
    return RULES_PATHS_KEY;
  }

  @Override
  public String rulesDefaultPath() {
    return RULES_DEFAULT_PATH;
  }

  @Override
  public String repositoryKey() {
    return REPOSITORY_KEY;
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public String repositoryName() {
    return REPOSITORY_NAME;
  }

  @Override
  public String ruleTag() {
    return RULE_TAG;
  }

  @Override
  public int startIndex() {
    return START_INDEX;
  }

  @Override
  public boolean include1CCertifiedProfile() {
    return false;
  }
}
