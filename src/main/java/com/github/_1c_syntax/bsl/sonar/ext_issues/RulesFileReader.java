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
package com.github._1c_syntax.bsl.sonar.ext_issues;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Читатель файлов с описаниями диагностик
 */
public class RulesFileReader {

  private static final Logger LOGGER = Loggers.get(RulesFileReader.class);

  private final String[] filePaths;
  private int current;

  public RulesFileReader(String[] filePaths) {
    this.filePaths = filePaths.clone();
  }

  /**
   * Выполняет чтение встроенных описаний диагностики по имени ресурса
   *
   * @param resourceName Имя ресурса-файла
   * @return Контейнер, содержащий объект прочитанного файла диагностик либо пустоту
   */
  public static Optional<RulesFile> getRulesFromResource(String resourceName) {
    String json;

    try {
      json = IOUtils.toString(
        Objects.requireNonNull(RulesFileReader.class.getClassLoader().getResourceAsStream(resourceName)),
        StandardCharsets.UTF_8.name()
      );
    } catch (IOException e) {
      LOGGER.error("Can't read json file rules", e);
      return Optional.empty();
    }

    return getRulesFile(json);
  }

  private static Optional<RulesFile> getRulesFile(String json) {
    var objectMapper = JsonMapper.builder()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build();
    try {
      return Optional.of(objectMapper.readValue(json, RulesFile.class));
    } catch (IOException e) {
      LOGGER.error("Can't serialize json rules to object", e);
      return Optional.empty();
    }
  }

  /**
   * Итератор для перехода к следующему прочитанному файлу описаний
   *
   * @return Контейнер, содержащий объект прочитанного файла диагностик либо пустоту
   */
  public Optional<RulesFile> getNext() {
    if (hasMore()) {
      Optional<RulesFile> rules = getRulesFromFile();
      current++;
      return rules;
    }

    return Optional.empty();
  }

  /**
   * Возвращает признак наличия еще не обработанных файлов описаний
   *
   * @return Наличие необработанных файлов
   */
  public boolean hasMore() {
    return current < filePaths.length;
  }

  private Optional<RulesFile> getRulesFromFile() {
    var file = new File(filePaths[current]);
    String json;

    try {
      json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOGGER.error("Can't read json file rules", file.toURI().toString(), e);
      return Optional.empty();
    }

    return getRulesFile(json);
  }

}
