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
    ruleDefinitions = AllReporters.getReporters().stream()
      .map(reporter -> new RuleDefinition(config, reporter))
      .collect(Collectors.toList());
  }

  @Override
  public void define(Context context) {
    ruleDefinitions.forEach(ruleDefinition -> ruleDefinition.define(context));
  }

  private static class RuleDefinition {

    private final String[] rulesFilePaths;
    private final boolean notEnabled;
    private final String repositoryKey;
    private final String repositoryName;
    private final String rulesDefaultPath;
    private final String ruleTag;
    private NewRepository repository;

    protected RuleDefinition(Configuration config, Reporter properties) {
      rulesFilePaths = config.getStringArray(properties.rulesPathsKey());
      notEnabled = !config.getBoolean(properties.enabledKey()).orElse(properties.enableDefaultValue());
      repositoryKey = properties.repositoryKey();
      repositoryName = properties.repositoryName();
      rulesDefaultPath = properties.rulesDefaultPath();
      ruleTag = properties.ruleTag();
    }

    protected void define(Context context) {
      if (notEnabled) {
        return;
      }

      repository = context.createRepository(repositoryKey, BSLLanguage.KEY).setName(repositoryName);
      loadRules();
      repository.done();
    }

    private void loadRules() {
      RulesFileReader.getRulesFromResource(rulesDefaultPath)
        .ifPresent((RulesFile file) -> file.getRules().forEach(this::createRule));

      var loader = new RulesFileReader(rulesFilePaths);

      while (loader.hasMore()) {
        loader.getNext().ifPresent((RulesFile file) -> file.getRules().forEach(this::createRule));
      }
    }

    private void createRule(RulesFile.Rule rule) {
      var foundRule = repository.rule(rule.getCode());

      if (foundRule == null) {
        foundRule = repository.createRule(rule.getCode()).addTags(ruleTag);
      }

      foundRule.setName(rule.getName())
        .setHtmlDescription(rule.getDescription())
        .setType(RuleType.valueOf(rule.getType()))
        .setSeverity(rule.getSeverity());
      foundRule.setDebtRemediationFunction(
        foundRule.debtRemediationFunctions().linear(rule.getEffortMinutes() + "min")
      );
    }
  }

}
