/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2020
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
package com.github._1c_syntax.bsl.sonar.acc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ACCRuleDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "acc-rules";
  public static final String SOURCE = "acc";
  private static final String REPOSITORY_NAME = "ACC rules";
  private static final Logger LOGGER = Loggers.get(ACCRuleDefinition.class);

  private final Configuration config;
  private NewRepository repository;

  public ACCRuleDefinition(Configuration config) {
    this.config = config;
  }

  @Override
  public void define(Context context) {
    repository = context
      .createRepository(REPOSITORY_KEY, BSLLanguage.KEY)
      .setName(REPOSITORY_NAME);
    loadRulesFromFile();
    repository.done();
  }

  private void loadRulesFromFile() {
    ACCRulesFile rulesFile = getRulesFile();

    if (rulesFile != null) {
      rulesFile.getRules().forEach(this::createRule);
    }
  }

  private void createRule(ACCRulesFile.ACCRule rule) {
    repository.createRule(rule.getCode())
      .setName(rule.getName())
      .setHtmlDescription(rule.getDescription())
      .setType(RuleType.valueOf(rule.getType()))
      .setSeverity(rule.getSeverity())
      .addTags("acc");
  }

  @CheckForNull
  protected static ACCRulesFile getRulesFile() {
    String json;

    try {
      json = IOUtils.toString(
        Objects.requireNonNull(ACCRuleDefinition.class.getClassLoader().getResourceAsStream("acc.json")),
        StandardCharsets.UTF_8.name()
      );
    } catch (IOException e) {
      LOGGER.error("Can't read json file acc rules", e);
      return null;
    }

    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(json, ACCRulesFile.class);
    } catch (IOException e) {
      LOGGER.error("Can't serialize json acc rules to object", e);
      return null;
    }
  }

}
