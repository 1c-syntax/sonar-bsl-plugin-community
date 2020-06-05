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
package com.github._1c_syntax.bsl.sonar.acc;

import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class ACCRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "acc-rules";
  public static final String SOURCE = "acc";
  private static final String REPOSITORY_NAME = "ACC rules";
  private static final Logger LOGGER = Loggers.get(ACCRuleDefinition.class);

  private NewRepository repository;
  private final String[] rulesFilePaths;
  private final boolean notEnabled;

  public ACCRuleDefinition(Configuration config) {
    rulesFilePaths = config.getStringArray(ACCProperties.ACC_RULES_PATHS);
    notEnabled = !config.getBoolean(ACCProperties.ACC_ENABLED).orElse(ACCProperties.ENABLE_ACC_DEFAULT_VALUE);
  }

  @Override
  public void define(Context context) {
    if (notEnabled) {
      return;
    }

    repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);
    loadRules();
    repository.done();
  }

  private void loadRules() {
    ACCRulesFileReader
      .getRulesFromResource()
      .ifPresent((ACCRulesFile file) -> file.getRules().forEach(this::createRule));

    ACCRulesFileReader loader = new ACCRulesFileReader(rulesFilePaths);

    while (loader.hasMore()) {
      loader.getNext().ifPresent((ACCRulesFile file) -> file.getRules().forEach(this::createRule));
    }
  }

  private void createRule(ACCRulesFile.ACCRule rule) {

    NewRule foundRule = repository.rule(rule.getCode());

    if (foundRule != null) {
      foundRule.setName(rule.getName())
        .setHtmlDescription(rule.getDescription())
        .setType(RuleType.valueOf(rule.getType()))
        .setSeverity(rule.getSeverity());
      foundRule.setDebtRemediationFunction(
        foundRule.debtRemediationFunctions().linear(
          rule.getEffortMinutes() + "min"
        )
      );
      return;
    }

    NewRule newRule = repository.createRule(rule.getCode())
      .setName(rule.getName())
      .setHtmlDescription(rule.getDescription())
      .setType(RuleType.valueOf(rule.getType()))
      .setSeverity(rule.getSeverity())
      .addTags("acc");
    newRule.setDebtRemediationFunction(
      newRule.debtRemediationFunctions().linear(
        rule.getEffortMinutes() + "min"
      )
    );
  }

}
