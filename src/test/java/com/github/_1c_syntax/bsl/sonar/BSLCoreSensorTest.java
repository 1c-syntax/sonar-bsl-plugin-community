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
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.configuration.DiagnosticLanguage;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguage;
import com.github._1c_syntax.bsl.sonar.language.BSLLanguageServerRuleDefinition;
import org.junit.jupiter.api.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Version;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BSLCoreSensorTest {

  private final String BASE_PATH = "src/test/resources/src";
  private final File BASE_DIR = new File(BASE_PATH).getAbsoluteFile();
  private final String FILE_NAME = "test.bsl";
  final Version SONAR_VERSION = Version.create(7, 9);
  private SensorContextTester context = SensorContextTester.create(BASE_DIR);

  @Test
  void testDescriptor() {
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);

    BSLCoreSensor sensor = new BSLCoreSensor(context, fileLinesContextFactory);
    DefaultSensorDescriptor sensorDescriptor = new DefaultSensorDescriptor();
    sensor.describe(sensorDescriptor);

    assertThat(sensorDescriptor.name()).isEqualTo("BSL Core Sensor");
    assertThat(sensorDescriptor.languages().toArray()[0]).isEqualTo(BSLLanguage.KEY);
  }

  @Test
  void testExecute() {

    String diagnosticName = "OneStatementPerLine";
    RuleKey ruleKey = RuleKey.of(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, diagnosticName);

    SensorContextTester context;
    BSLCoreSensor sensor;

    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    context = createSensorContext();
    setActiveRules(context, diagnosticName, ruleKey);
    sensor = new BSLCoreSensor(context, fileLinesContextFactory);
    sensor.execute(context);

    assertThat(context.isCancelled()).isFalse();

    context = createSensorContext();
    setActiveRules(context, diagnosticName, ruleKey);
    context.settings().setProperty(BSLCommunityProperties.LANG_SERVER_ENABLED_KEY, false);
    sensor = new BSLCoreSensor(context, fileLinesContextFactory);
    sensor.execute(context);

    assertThat(context.isCancelled()).isFalse();

    context = createSensorContext();
    setActiveRules(context, diagnosticName, ruleKey);
    context.settings().setProperty(BSLCommunityProperties.LANG_SERVER_DIAGNOSTIC_LANGUAGE_KEY, DiagnosticLanguage.EN.getLanguageCode());
    sensor = new BSLCoreSensor(context, fileLinesContextFactory);
    sensor.execute(context);

    assertThat(context.isCancelled()).isFalse();
    
  }

  @Test
  void testExecuteCastDiagnosticParameterValue() {

    String diagnosticEmptyCodeBlock = "EmptyCodeBlock";
    String diagnosticCommentedCode = "CommentedCode";
    String diagnosticLineLength = "LineLength";
    String diagnosticUsingHardcodeNetworkAddress = "UsingHardcodeNetworkAddress";

    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    SensorContextTester context = createSensorContext();
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(newActiveRule(diagnosticEmptyCodeBlock, "commentAsCode", "true"))
      .addRule(newActiveRule(diagnosticCommentedCode, "threshold", "0.9F"))
      .addRule(newActiveRule(diagnosticLineLength, "maxLineLength", "100"))
      .addRule(
        newActiveRule(
          diagnosticUsingHardcodeNetworkAddress,
          "searchWordsExclusion",
          "Верси|Version"))
      .build();
    context.setActiveRules(activeRules);

    BSLCoreSensor sensor = new BSLCoreSensor(context, fileLinesContextFactory);
    sensor.execute(context);

    assertThat(context.isCancelled()).isFalse();
  }

  @Test
  void testExecuteCoverage() {
    final String diagnosticName = "OneStatementPerLine";
    final RuleKey ruleKey = RuleKey.of(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, diagnosticName);

    SensorContextTester context;
    BSLCoreSensor sensor;

    // Mock visitor for metrics.
    FileLinesContext fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);

    context = createSensorContext();
    context.settings().setProperty(BSLCommunityProperties.LANG_SERVER_ENABLED_KEY, false);
    context.settings().setProperty(BSLCommunityProperties.BSL_CALCULATE_LINE_TO_COVER_KEY, true);
    setActiveRules(context, diagnosticName, ruleKey);
    sensor = new BSLCoreSensor(context, fileLinesContextFactory);
    sensor.execute(context);

    assertThat(context.isCancelled()).isFalse();
  }

  private void setActiveRules(SensorContextTester context, String diagnosticName, RuleKey ruleKey) {
    ActiveRules activeRules = new ActiveRulesBuilder()
      .addRule(new NewActiveRule.Builder()
        .setRuleKey(ruleKey)
        .setName(diagnosticName)
        .build())
      .build();
    context.setActiveRules(activeRules);
  }

  private NewActiveRule newActiveRule(String diagnosticName, String key, String value) {
    return new NewActiveRule.Builder()
      .setRuleKey(RuleKey.of(BSLLanguageServerRuleDefinition.REPOSITORY_KEY, diagnosticName))
      .setName(diagnosticName)
      .setParam(key, value)
      .build();
  }


  private SensorContextTester createSensorContext() {
    SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarLint(SONAR_VERSION);
    SensorContextTester context = SensorContextTester.create(BASE_DIR);
    context.setRuntime(sonarRuntime);

    InputFile inputFile = Tools.inputFileBSL(FILE_NAME, BASE_DIR);
    context.fileSystem().add(inputFile);

    return context;
  }

}
