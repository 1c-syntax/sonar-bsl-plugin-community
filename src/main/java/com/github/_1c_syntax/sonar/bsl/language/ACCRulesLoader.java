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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github._1c_syntax.sonar.bsl.BSLCommunityProperties;
import com.github._1c_syntax.sonar.bsl.common.RulesFile;
import org.apache.commons.io.FileUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ACCRulesLoader implements RulesDefinition {

  public static final String REPOSITORY_KEY = "acc-rules";
  private static final String REPOSITORY_NAME = "ACC rules (BSL)";

  private static final Logger LOGGER = Loggers.get(ACCRulesLoader.class);

  private final Configuration config;
  private NewRepository repository;

  public ACCRulesLoader(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(Context context) {

    repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);

    Optional<String> optionPath = config.get(BSLCommunityProperties.LANG_SERVER_ACCRULES_PATH);
    if (optionPath.isPresent()) {
      String rulesPath = optionPath.get();
      if (!rulesPath.isEmpty()) {
        File rulesFile = new File(rulesPath);
        if (rulesFile.exists()) {
          loadRulesFromFile(rulesFile);
        }
      }
    }
    repository.done();
  }

  private void createRule(RulesFile.ACCRule rule) {
    repository.createRule(rule.getCode())
      .setName(rule.getName())
      .setHtmlDescription(rule.getDescription())
      .setType(RuleType.valueOf(rule.getType()))
      .setSeverity(rule.getSeverity());
  }

  private void loadRulesFromFile(File file) {

    RulesFile rulesFile = getRulesFile(file);

    if (rulesFile != null) {
      rulesFile.getRules().forEach(this::createRule);
    }
  }

  @CheckForNull
  private static RulesFile getRulesFile(File file) {
    String json;
    try {
      json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Can't read json file acc rules", file.toURI().toString(), e);
      return null;
    }
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(json, RulesFile.class);
    } catch (IOException e) {
      LOGGER.error("Can't serialize json acc rules to object", e);
      return null;
    }
  }

}
