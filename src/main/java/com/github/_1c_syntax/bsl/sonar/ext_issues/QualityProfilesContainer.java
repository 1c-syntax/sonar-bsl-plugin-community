/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2023
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
import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import lombok.Getter;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Контейнер профилей качества внешних репортеров
 */
public class QualityProfilesContainer implements BuiltInQualityProfilesDefinition {

  private static final String PROFILE_ALL_RULES = "BSL - all rules";
  private final List<QualityProfile> qualityProfiles;

  public QualityProfilesContainer(Configuration config) {
    qualityProfiles = ExternalReporters.REPORTERS.stream()
      .map(reporter -> new QualityProfile(config, reporter))
      .collect(Collectors.toList());
  }

  @Override
  public void define(Context context) {
    var enabledQualityProfiles = qualityProfiles.stream()
      .filter(QualityProfile::isEnabled).collect(Collectors.toList());

    if (enabledQualityProfiles.isEmpty()) {
      return;
    }

    // добавление общего профиля для все диагностик
    var rulesBSL = BSLLanguageServerRuleDefinition.getActivatedRuleKeys();
    var fullBSLProfile = context.createBuiltInQualityProfile(
      PROFILE_ALL_RULES,
      BSLLanguage.KEY
    );

    enabledQualityProfiles.forEach((QualityProfile qualityProfile) -> {
      qualityProfile.define(context);
      qualityProfile.activateDefaultRules(fullBSLProfile);
    });

    rulesBSL
      .forEach(key -> fullBSLProfile.activateRule(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, key));
    fullBSLProfile.done();
  }

  /**
   * Создает профили качества для внешнего репортера
   */
  private static class QualityProfile {

    @Getter
    private final boolean isEnabled;
    private final Reporter reporter;
    private final List<RulesFile> rulesFiles = new ArrayList<>();

    protected QualityProfile(Configuration config, Reporter reporter) {
      this.reporter = reporter;
      isEnabled = config.getBoolean(reporter.getEnabledKey()).orElse(reporter.isEnableDefaultValue());

      if (isEnabled) {
        rulesFiles.addAll(RulesFileReader.getRulesFiles(
            reporter.getRulesDefaultPath(),
            config.getStringArray(reporter.getRulesPathsKey())
          )
        );
      }
    }

    protected void define(Context context) {
      if (!isEnabled) {
        return;
      }

      addFullCheckProfile(context);
      if (reporter.isInclude1CCertifiedProfile()) {
        add1CCertifiedProfile(context);
      }
    }

    protected void activateDefaultRules(NewBuiltInQualityProfile profile) {
      activateRules(profile, RulesFile.Rule::isActive);
    }

    private void addFullCheckProfile(Context context) {
      var profile = createQualityProfile(context, "%s - full check");
      activateDefaultRules(profile);
      profile.done();
    }

    private void add1CCertifiedProfile(Context context) {
      var profile = createQualityProfile(context, "%s - 1C:Compatible");
      activateRules(profile, RulesFile.Rule::isNeedForCertificate);
      profile.done();
    }

    private NewBuiltInQualityProfile createQualityProfile(Context context, String nameTemplate) {
      return context.createBuiltInQualityProfile(
        String.format(nameTemplate, reporter.getSubcategory()),
        BSLLanguage.KEY
      );
    }

    private void activateRules(NewBuiltInQualityProfile profile, Predicate<? super RulesFile.Rule> filter) {
      rulesFiles.stream()
        .map(RulesFile::getRules)
        .flatMap(Collection::stream)
        .filter(filter)
        .map(RulesFile.Rule::getCode)
        .distinct()
        .forEach(key -> profile.activateRule(reporter.getRepositoryKey(), key));
    }
  }
}
