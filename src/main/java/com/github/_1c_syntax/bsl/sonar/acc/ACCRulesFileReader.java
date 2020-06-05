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
package com.github._1c_syntax.bsl.sonar.acc;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public class ACCRulesFileReader {

  private static final Logger LOGGER = Loggers.get(ACCRulesFileReader.class);

  private final String[] filePaths;
  private int current;

  public ACCRulesFileReader(String[] filePaths) {
    this.filePaths = filePaths.clone();
  }

  public Optional<ACCRulesFile> getNext() {
    if (hasMore()) {
      Optional<ACCRulesFile> rules = getRulesFromFile();
      current++;
      return rules;
    }

    return Optional.empty();
  }

  public boolean hasMore() {
    return current < filePaths.length;
  }

  public static Optional<ACCRulesFile> getRulesFromResource() {
    String json;

    try {
      json = IOUtils.toString(
          Objects.requireNonNull(ACCRuleDefinition.class.getClassLoader().getResourceAsStream("acc.json")),
          StandardCharsets.UTF_8.name()
      );
    } catch (IOException e) {
      LOGGER.error("Can't read json file acc rules", e);
      return Optional.empty();
    }

    return getAccRulesFile(json);
  }

  private static Optional<ACCRulesFile> getAccRulesFile(String json) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    try {
      return Optional.of(objectMapper.readValue(json, ACCRulesFile.class));
    } catch (IOException e) {
      LOGGER.error("Can't serialize json acc rules to object", e);
      return Optional.empty();
    }
  }

  private Optional<ACCRulesFile> getRulesFromFile() {
    File file = new File(filePaths[current]);
    String json;

    try {
      json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Can't read json file acc rules", file.toURI().toString(), e);
      return Optional.empty();
    }

    return getAccRulesFile(json);
  }

}
