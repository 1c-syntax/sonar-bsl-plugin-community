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
import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import lombok.Getter;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контейнер профилей качества внешних репортеров
 */
public class QualityProfilesContainer implements BuiltInQualityProfilesDefinition {

  private final List<QualityProfile> qualityProfiles;

  public QualityProfilesContainer(Configuration config) {
    qualityProfiles = AllReporters.getReporters().stream()
      .map(reporter -> new QualityProfile(config, reporter))
      .collect(Collectors.toList());
  }

  @Override
  public void define(Context context) {
    var enabledQualityProfiles = qualityProfiles.stream()
      .filter(profile -> !profile.isNotEnabled()).collect(Collectors.toList());

    if (enabledQualityProfiles.isEmpty()) {
      return;
    }

    // добавление общего профиля для все диагностик
    var rulesBSL = BSLLanguageServerRuleDefinition.getActivatedRuleKeys();
    var fullBSLProfile = context.createBuiltInQualityProfile(
      "BSL - all rules",
      BSLLanguage.KEY
    );

    enabledQualityProfiles.forEach(qualityProfile -> {
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

    private final List<RulesFile> externalFiles = new ArrayList<>();
    @Getter
    private final boolean notEnabled;
    private final Reporter reporter;
    private RulesFile rulesFile;

    protected QualityProfile(Configuration config, Reporter reporter) {
      this.reporter = reporter;

      RulesFileReader.getRulesFromResource(reporter.rulesDefaultPath())
        .ifPresent((RulesFile file) -> this.rulesFile = file);

      notEnabled = !config.getBoolean(reporter.enabledKey()).orElse(reporter.enableDefaultValue())
        || rulesFile == null;
      RulesFileReader loader = new RulesFileReader(config.getStringArray(reporter.rulesPathsKey()));

      while (loader.hasMore()) {
        loader.getNext().ifPresent(externalFiles::add);
      }
    }

    protected void activateDefaultRules(NewBuiltInQualityProfile profile) {
      ArrayList<String> activatedRules = new ArrayList<>();
      rulesFile.getRules()
        .stream()
        .filter(RulesFile.Rule::isActive)
        .map(RulesFile.Rule::getCode)
        .forEach((String key) -> {
          profile.activateRule(reporter.repositoryKey(), key);
          activatedRules.add(key);
        });
      externalFiles.stream()
        .map(RulesFile::getRules)
        .flatMap(Collection::stream)
        .filter(RulesFile.Rule::isActive)
        .map(RulesFile.Rule::getCode)
        .filter(key -> !activatedRules.contains(key))
        .forEach(key -> profile.activateRule(reporter.repositoryKey(), key));
    }

    protected void define(Context context) {
      if (notEnabled) {
        return;
      }

      addFullCheckProfile(context);
      if (reporter.include1CCertifiedProfile()) {
        add1CCertifiedProfile(context);
      }
    }

    private void addFullCheckProfile(Context context) {
      NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
        String.format("%s - full check", reporter.subcategory()),
        BSLLanguage.KEY
      );
      activateDefaultRules(profile);
      profile.done();
    }

    private void add1CCertifiedProfile(Context context) {
      NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
        String.format("%s - 1C:Compatible", reporter.subcategory()),
        BSLLanguage.KEY
      );
      ArrayList<String> activatedRules = new ArrayList<>();
      rulesFile.getRules()
        .stream()
        .filter(RulesFile.Rule::isNeedForCertificate)
        .map(RulesFile.Rule::getCode)
        .forEach((String key) -> {
          profile.activateRule(reporter.repositoryKey(), key);
          activatedRules.add(key);
        });
      externalFiles.stream()
        .map(RulesFile::getRules)
        .flatMap(Collection::stream)
        .filter(RulesFile.Rule::isNeedForCertificate)
        .map(RulesFile.Rule::getCode)
        .filter(key -> !activatedRules.contains(key))
        .forEach(key -> profile.activateRule(reporter.repositoryKey(), key));
      profile.done();
    }

  }
}
