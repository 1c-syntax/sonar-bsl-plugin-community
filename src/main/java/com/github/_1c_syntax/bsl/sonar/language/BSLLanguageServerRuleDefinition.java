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
package com.github._1c_syntax.bsl.sonar.language;

import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.sonar.BSLCommunityProperties;
import com.github._1c_syntax.bsl.languageserver.configuration.DiagnosticLanguage;
import com.github._1c_syntax.bsl.languageserver.diagnostics.BSLDiagnostic;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameter;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.languageserver.providers.DiagnosticProvider;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BSLLanguageServerRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "bsl-language-server";
  private static final String REPOSITORY_NAME = "BSL Language Server";
  private static final Logger LOGGER = Loggers.get(BSLLanguageServerRuleDefinition.class);
  private static final Locale systemLocale = Locale.getDefault();

  private static final Pattern PATTERN_HEADERS = Pattern.compile("(?!.+[(\\[])#(?!.+[)\\]])");
  private static final Pattern PATTERN_STARS = Pattern.compile("\\*\\*");
  private static final Pattern PATTERN_BACKTICKS = Pattern.compile("```");
  private static final Pattern PATTERN_BACKTICKS_SURROUND = Pattern.compile("(^|[^`])`([^`]|$)");

  private static final Map<DiagnosticSeverity, String> SEVERITY_MAP = createDiagnosticSeverityMap();
  private static final Map<DiagnosticType, RuleType> RULE_TYPE_MAP = createRuleTypeMap();

  private final Configuration config;
  private DiagnosticProvider diagnosticProvider = new DiagnosticProvider();

  public BSLLanguageServerRuleDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(Context context) {

    if (config.get(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE)
      .equals(DiagnosticLanguage.RU.getLanguageCode())
    ) {
      Locale.setDefault(new Locale("ru", "RU"));
    } else {
      Locale.setDefault(Locale.ENGLISH);
    }

    diagnosticProvider = new DiagnosticProvider(getLanguageServerConfiguration());

    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);

    List<Class<? extends BSLDiagnostic>> diagnosticClasses = DiagnosticProvider.getDiagnosticClasses();
    diagnosticClasses.forEach((Class<? extends BSLDiagnostic> diagnostic) -> {
      NewRule newRule = repository.createRule(DiagnosticProvider.getDiagnosticCode(diagnostic));
      setUpNewRule(diagnostic, newRule);
      setUpRuleParams(diagnostic, newRule);
    });

    repository.done();
    Locale.setDefault(systemLocale);
  }

  protected static List<String> getActivatedRuleKeys() {
    return DiagnosticProvider.getDiagnosticClasses().stream()
      .filter(DiagnosticProvider::isActivatedByDefault)
      .map(DiagnosticProvider::getDiagnosticCode)
      .collect(Collectors.toList());
  }

  private void setUpNewRule(Class<? extends BSLDiagnostic> diagnostic, NewRule newRule) {

    // todo: get localized name
    newRule
      .setName(DiagnosticProvider.getDiagnosticName(diagnostic))
      .setMarkdownDescription(convertToSonarQubeMarkdown(
              diagnosticProvider.getDiagnosticDescription(diagnostic)))
      .setType(RULE_TYPE_MAP.get(DiagnosticProvider.getDiagnosticType(diagnostic)))
      .setSeverity(SEVERITY_MAP.get(DiagnosticProvider.getDiagnosticSeverity(diagnostic)))
      .setActivatedByDefault(DiagnosticProvider.isActivatedByDefault(diagnostic))
    ;

    newRule.setDebtRemediationFunction(
      newRule.debtRemediationFunctions().linear(
        DiagnosticProvider.getMinutesToFix(diagnostic) + "min"
      )
    );
  }

  private static void setUpRuleParams(Class<? extends BSLDiagnostic> diagnostic, NewRule newRule) {
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
        Locale.setDefault(systemLocale);
        return;
      }

      NewParam newParam = newRule.createParam(paramKey);
      newParam.setType(ruleParamType);
      newParam.setDescription(diagnosticParameter.description());
      newParam.setDefaultValue(DiagnosticProvider.getDefaultValue(diagnosticParameter).toString());
    });
  }

  private static String convertToSonarQubeMarkdown(String input) {
    String result = input;
    result = PATTERN_HEADERS.matcher(result).replaceAll("=");
    result = PATTERN_STARS.matcher(result).replaceAll("*");
    result = PATTERN_BACKTICKS.matcher(result).replaceAll("``");
    result = PATTERN_BACKTICKS_SURROUND.matcher(result).replaceAll("$1``$2");

    return result;
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

  private LanguageServerConfiguration getLanguageServerConfiguration() {
    LanguageServerConfiguration languageServerConfiguration = LanguageServerConfiguration.create();
    String diagnosticLanguageCode = config
            .get(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY)
            .orElse(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE);

    languageServerConfiguration.setDiagnosticLanguage(
            DiagnosticLanguage.valueOf(diagnosticLanguageCode.toUpperCase(Locale.ENGLISH))
    );

    return languageServerConfiguration;
  }


}
