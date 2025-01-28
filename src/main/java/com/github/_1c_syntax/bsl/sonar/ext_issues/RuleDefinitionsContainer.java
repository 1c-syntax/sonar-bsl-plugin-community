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

import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контейнер диагностик внешних репортеров
 */
public class RuleDefinitionsContainer implements RulesDefinition {

  private final List<RuleDefinition> ruleDefinitions;

  public RuleDefinitionsContainer(Configuration config) {
    ruleDefinitions = ExternalReporters.REPORTERS.stream()
      .map(reporter -> new RuleDefinition(config, reporter))
      .collect(Collectors.toList());
  }

  @Override
  public void define(Context context) {
    ruleDefinitions.forEach(ruleDefinition -> ruleDefinition.define(context));
  }

  private static class RuleDefinition {

    private final String[] rulesFilePaths;
    private final boolean enabled;
    private final String repositoryKey;
    private final String repositoryName;
    private final String rulesDefaultPath;
    private final String ruleTag;
    private NewRepository repository;

    protected RuleDefinition(Configuration config, Reporter properties) {
      rulesFilePaths = config.getStringArray(properties.getRulesPathsKey());
      enabled = config.getBoolean(properties.getEnabledKey())
        .orElse(properties.isEnableDefaultValue());
      repositoryKey = properties.getRepositoryKey();
      repositoryName = properties.getRepositoryName();
      rulesDefaultPath = properties.getRulesDefaultPath();
      ruleTag = properties.getRuleTag();
    }

    protected void define(Context context) {
      if (enabled) {
        repository = context.createRepository(repositoryKey, BSLLanguage.KEY).setName(repositoryName);
        loadRules();
        repository.done();
      }
    }

    private void loadRules() {
      RulesFileReader.getRulesFiles(rulesDefaultPath, rulesFilePaths)
        .forEach(file -> file.rules().forEach(this::createRule));
    }

    private void createRule(RulesFile.Rule rule) {
      var foundRule = repository.rule(rule.code());

      if (foundRule == null) {
        foundRule = repository.createRule(rule.code()).addTags(ruleTag);
      }

      foundRule.setName(rule.name())
        .setHtmlDescription(rule.description())
        .setType(RuleType.valueOf(rule.type()))
        .setSeverity(rule.severity());
      foundRule.setDebtRemediationFunction(
        foundRule.debtRemediationFunctions().linear(rule.effortMinutes() + "min")
      );
    }
  }
}
