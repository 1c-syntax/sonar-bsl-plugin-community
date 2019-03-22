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

import org.eclipse.lsp4j.DiagnosticSeverity;
import org.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import org.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import org.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class BSLLanguageServerRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "bsl-language-server";
  private static final String REPOSITORY_NAME = "BSL Language Server";

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);

    Map<DiagnosticSeverity, String> severityMap = createDiagnosticSeverityMap();
    Map<DiagnosticSeverity, RuleType> ruleTypeMap = createRuleTypeMap();

    List<BSLDiagnostic> diagnosticInstances = getDiagnostics();
    diagnosticInstances.forEach(diagnostic -> repository.createRule(diagnostic.getCode())
      .setName(diagnostic.getDiagnosticMessage())
      .setMarkdownDescription("# Проверка")

      .setType(ruleTypeMap.get(diagnostic.getSeverity()))
      .setSeverity(severityMap.get(diagnostic.getSeverity())));

    // don't forget to call done() to finalize the definition
    repository.done();
  }

  public static List<BSLDiagnostic> getDiagnostics() {
    DiagnosticProvider diagnosticProvider = new DiagnosticProvider(LanguageServerConfiguration.create());
    return diagnosticProvider.getDiagnosticClasses();
  }

  private Map<DiagnosticSeverity, String> createDiagnosticSeverityMap() {
    Map<DiagnosticSeverity, String> map = new EnumMap<>(DiagnosticSeverity.class);
    map.put(DiagnosticSeverity.Warning, org.sonar.api.rule.Severity.MAJOR);
    map.put(DiagnosticSeverity.Information, org.sonar.api.rule.Severity.MINOR);
    map.put(DiagnosticSeverity.Hint, org.sonar.api.rule.Severity.INFO);
    map.put(DiagnosticSeverity.Error, org.sonar.api.rule.Severity.CRITICAL);

    return map;
  }

  private Map<DiagnosticSeverity, RuleType> createRuleTypeMap() {
    Map<DiagnosticSeverity, RuleType> map = new EnumMap<>(DiagnosticSeverity.class);
    map.put(DiagnosticSeverity.Warning, RuleType.CODE_SMELL);
    map.put(DiagnosticSeverity.Information, RuleType.CODE_SMELL);
    map.put(DiagnosticSeverity.Hint, RuleType.CODE_SMELL);
    map.put(DiagnosticSeverity.Error, RuleType.BUG);

    return map;
  }
}
