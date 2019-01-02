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
package com.github._1c_syntax.sonar.bsl;

import org.sonar.api.config.PropertyDefinition;

import java.util.Collections;
import java.util.List;

public class BSLCommunityProperties {

    public static final String LANG_SERVER_REPORT_PATH_KEY = "sonar.bsl.language.server.report.path";
    private static final String CATEGORY = "1C (BSL) Community";

    private BSLCommunityProperties() {
        // only statics
    }

    public static List<PropertyDefinition> getProperties() {
        return Collections.singletonList(
                PropertyDefinition.builder(LANG_SERVER_REPORT_PATH_KEY)
                        .name("BSL Language Server report path")
                        .description("Path to json report from BSL Language Server")
                        .defaultValue("")
                        .category(CATEGORY)
                        .build());
    }

}
