/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright © 2018-2022
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
package com.github._1c_syntax.bsl.sonar.language;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.rule.RulesDefinition;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class BSLLanguageServerRuleDefinitionTest {

    @Test
    void test_init() {
        Configuration config = new MapSettings().asConfig();
        BSLLanguageServerRuleDefinition ruleDefinition = new BSLLanguageServerRuleDefinition(config);
        RulesDefinition.Context context = new RulesDefinition.Context();
        ruleDefinition.define(context);

        assertThat(context.repositories()).hasSize(1);
        assertThat(context.repository(BSLLanguageServerRuleDefinition.REPOSITORY_KEY)).isNotNull();
    }

    @Test
    void testCheckTagParameters() {
        Configuration config = new MapSettings().asConfig();
        BSLLanguageServerRuleDefinition ruleDefinition = new BSLLanguageServerRuleDefinition(config);
        RulesDefinition.Context context = new RulesDefinition.Context();
        ruleDefinition.define(context);

        assertThat(context.repositories()).hasSize(1);
        assertThat(Objects.requireNonNull(context.repository(BSLLanguageServerRuleDefinition.REPOSITORY_KEY))
            .rules().stream()
            .filter(rule -> !rule.params().isEmpty())
            .filter(rule -> !rule.tags().contains(BSLLanguageServerRuleDefinition.PARAMETERS_TAG_NAME))
            .count()
        ).isZero();
        assertThat(Objects.requireNonNull(context.repository(BSLLanguageServerRuleDefinition.REPOSITORY_KEY))
            .rules().stream()
            .filter(rule -> rule.params().isEmpty())
            .filter(rule -> rule.tags().contains(BSLLanguageServerRuleDefinition.PARAMETERS_TAG_NAME))
            .count()
        ).isZero();
    }

}
