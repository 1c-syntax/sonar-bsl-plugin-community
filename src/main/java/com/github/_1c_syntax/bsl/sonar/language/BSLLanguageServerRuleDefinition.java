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
package com.github._1c_syntax.bsl.sonar.language;

import com.github._1c_syntax.bsl.languageserver.BSLLSBinding;
import com.github._1c_syntax.bsl.languageserver.configuration.Language;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticCode;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticParameterInfo;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticSeverity;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticType;
import com.github._1c_syntax.bsl.sonar.BSLCommunityProperties;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BSLLanguageServerRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "bsl-language-server";
  public static final String PARAMETERS_TAG_NAME = "parameters";
  private static final String REPOSITORY_NAME = "BSL Language Server";
  private static final Logger LOGGER = Loggers.get(BSLLanguageServerRuleDefinition.class);

  private static final Map<DiagnosticSeverity, String> SEVERITY_MAP = createDiagnosticSeverityMap();
  private static final Map<DiagnosticType, RuleType> RULE_TYPE_MAP = createRuleTypeMap();

  private final Configuration config;
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;
  private DiagnosticInfo diagnosticInfo;

  public BSLLanguageServerRuleDefinition(Configuration config) {
    this.config = config;

    var configuration = BSLLSBinding.getLanguageServerConfiguration();
    configuration.setLanguage(createDiagnosticLanguage());

    List<Extension> extensions = Arrays.asList(
      TablesExtension.create(),
      AutolinkExtension.create(),
      HeadingAnchorExtension.create()
    );

    markdownParser = Parser.builder()
      .extensions(extensions)
      .build();

    htmlRenderer = HtmlRenderer.builder()
      .extensions(extensions)
      .build();
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);

    var diagnosticInfos = BSLLSBinding.getDiagnosticInfos();

    diagnosticInfos.forEach((DiagnosticInfo currentDiagnosticInfo) -> {
        diagnosticInfo = currentDiagnosticInfo;
        NewRule newRule = repository.createRule(diagnosticInfo.getCode().getStringValue());
        setUpNewRule(newRule);
        setUpRuleParams(newRule);
      });

    repository.done();

    BSLLSBinding.getApplicationContext().close();
  }

  public static List<String> getActivatedRuleKeys() {
    return BSLLSBinding.getDiagnosticInfos()
      .stream()
      .filter(DiagnosticInfo::isActivatedByDefault)
      .map(DiagnosticInfo::getCode)
      .map((DiagnosticCode diagnosticCode) -> diagnosticCode.getStringValue())
      .collect(Collectors.toList());
  }

  private void setUpNewRule(NewRule newRule) {

    newRule
      .setName(diagnosticInfo.getName())
      .setHtmlDescription(getHtmlDescription(diagnosticInfo.getDescription()))
      .setType(RULE_TYPE_MAP.get(diagnosticInfo.getType()))
      .setSeverity(SEVERITY_MAP.get(diagnosticInfo.getSeverity()))
      .setActivatedByDefault(diagnosticInfo.isActivatedByDefault())
    ;

    String[] tagsName = diagnosticInfo.getTags()
      .stream()
      .map(Enum::name)
      .map(String::toLowerCase)
      .toArray(String[]::new);

    if (!diagnosticInfo.getParameters().isEmpty()) {
      newRule.addTags(PARAMETERS_TAG_NAME);
    }

    if (tagsName.length > 0) {
      newRule.addTags(tagsName);
    }

    newRule.setDebtRemediationFunction(
      newRule.debtRemediationFunctions().linear(
        diagnosticInfo.getMinutesToFix() + "min"
      )
    );
  }

  private String getHtmlDescription(String markdownDescription) {
    return htmlRenderer.render(markdownParser.parse(markdownDescription));
  }

  private void setUpRuleParams(NewRule newRule) {
    diagnosticInfo.getParameters()
      .forEach((DiagnosticParameterInfo diagnosticParameter) -> {
        RuleParamType ruleParamType = getRuleParamType(diagnosticParameter.getType());
        if (ruleParamType == null) {
          LOGGER.error(
            String.format(
              "Can't cast rule param type %s for rule %s",
              diagnosticParameter.getType(),
              newRule.key()
            )
          );
          return;
        }

        NewParam newParam = newRule.createParam(diagnosticParameter.getName());
        newParam.setType(ruleParamType);
        newParam.setDescription(diagnosticParameter.getDescription());
        newParam.setDefaultValue(diagnosticParameter.getDefaultValue().toString());
      });
  }

  private Language createDiagnosticLanguage() {

    String diagnosticLanguageCode = config
      .get(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY)
      .orElse(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_DEFAULT_VALUE);

    return Language.valueOf(diagnosticLanguageCode.toUpperCase(Locale.ENGLISH));
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
    map.put(DiagnosticType.SECURITY_HOTSPOT, RuleType.SECURITY_HOTSPOT);

    return map;
  }

}

