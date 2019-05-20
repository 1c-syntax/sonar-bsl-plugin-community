/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2019
 * Nikita Gryzlov <nixel2007@gmail.com>
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
package com.github._1c_syntax.sonar.bsl.language;

import org.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameter;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import org.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import org.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BSLLanguageServerRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "bsl-language-server";
  private static final String REPOSITORY_NAME = "BSL Language Server";
  private static final Logger LOGGER = Loggers.get(BSLLanguageServerRuleDefinition.class);

  private final Configuration config;

  public BSLLanguageServerRuleDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);
    Map<DiagnosticSeverity, String> severityMap = createDiagnosticSeverityMap();
    Map<DiagnosticType, RuleType> ruleTypeMap = createRuleTypeMap();

    List<Class<? extends BSLDiagnostic>> diagnosticClasses = DiagnosticProvider.getDiagnosticClasses();
    diagnosticClasses.forEach((Class<? extends BSLDiagnostic> diagnostic) -> {
      NewRule newRule = repository.createRule(DiagnosticProvider.getDiagnosticCode(diagnostic))
        // todo: get localized name
        .setName(DiagnosticProvider.getDiagnosticName(diagnostic))
        .setMarkdownDescription(convertToSonarqubeMarkdown(DiagnosticProvider.getDiagnosticDescription(diagnostic)))
        .setType(ruleTypeMap.get(DiagnosticProvider.getDiagnosticType(diagnostic)))
        .setSeverity(severityMap.get(DiagnosticProvider.getDiagnosticSeverity(diagnostic)));

      newRule.setDebtRemediationFunction(
              newRule.debtRemediationFunctions()
                      .linear(DiagnosticProvider.getMinutesToFix(diagnostic) + "min"));

      Map<String, DiagnosticParameter> diagnosticParameters = DiagnosticProvider.getDiagnosticParameters(diagnostic);
      diagnosticParameters.forEach((String paramKey, DiagnosticParameter diagnosticParameter) -> {
        RuleParamType ruleParamType = getRuleParamType(diagnosticParameter.type());
        if (ruleParamType == null) {
          LOGGER.error(
            String.format(
              "Can't cast rule param type %s for rule %s",
              diagnosticParameter.type(),
              newRule.key()
            )
          );
          return;
        }

        NewParam newParam = newRule.createParam(paramKey);
        newParam.setType(ruleParamType);
        newParam.setDescription(diagnosticParameter.description());
        newParam.setDefaultValue(DiagnosticProvider.getDefaultValue(diagnosticParameter).toString());
      });

    });

    repository.done();
  }

  public static List<String> getRuleKeys() {
    return DiagnosticProvider.getDiagnosticClasses().stream()
      .map(DiagnosticProvider::getDiagnosticCode)
      .collect(Collectors.toList());
  }

  private static String convertToSonarqubeMarkdown(String input) {
    return input
      .replaceAll("(?!.+\\[)#(?!.+])", "=")
      .replaceAll("\\*\\*", "*")
      .replaceAll("```", "``")
      .replaceAll("(^|[^`])`([^`]|$)", "$1``$2")
      ;
  }

  @CheckForNull
  private static RuleParamType getRuleParamType(Class<?> type) {

    RuleParamType ruleParamType;

    if (type == Integer.class) {
      ruleParamType = RuleParamType.INTEGER;
    } else if (type == String.class) {
      ruleParamType = RuleParamType.STRING;
    } else if (type == Boolean.class) {
      ruleParamType = RuleParamType.BOOLEAN;
    } else if (type == Float.class) {
      ruleParamType = RuleParamType.FLOAT;
    } else {
      ruleParamType = null;
    }

    return ruleParamType;
  }

  private static Map<DiagnosticSeverity, String> createDiagnosticSeverityMap() {
    Map<DiagnosticSeverity, String> map = new EnumMap<>(DiagnosticSeverity.class);
    map.put(DiagnosticSeverity.INFO, org.sonar.api.rule.Severity.INFO);
    map.put(DiagnosticSeverity.MINOR, org.sonar.api.rule.Severity.MINOR);
    map.put(DiagnosticSeverity.MAJOR, org.sonar.api.rule.Severity.MAJOR);
    map.put(DiagnosticSeverity.CRITICAL, org.sonar.api.rule.Severity.CRITICAL);
    map.put(DiagnosticSeverity.BLOCKER, org.sonar.api.rule.Severity.BLOCKER);

    return map;
  }

  private static Map<DiagnosticType, RuleType> createRuleTypeMap() {
    Map<DiagnosticType, RuleType> map = new EnumMap<>(DiagnosticType.class);
    map.put(DiagnosticType.CODE_SMELL, RuleType.CODE_SMELL);
    map.put(DiagnosticType.ERROR, RuleType.BUG);
    map.put(DiagnosticType.VULNERABILITY, RuleType.VULNERABILITY);

    return map;
  }
}
