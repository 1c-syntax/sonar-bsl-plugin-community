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
package com.github._1c_syntax.bsl.sonar;

import com.github._1c_syntax.bsl.languageserver.configuration.LanguageServerConfiguration;
import com.github._1c_syntax.bsl.languageserver.context.ServerContext;
import com.github._1c_syntax.bsl.languageserver.diagnostics.infrastructure.DiagnosticInfosConfiguration;
import com.github._1c_syntax.bsl.languageserver.diagnostics.metadata.DiagnosticInfo;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import java.util.Collection;
import java.util.Map;

@SpringBootConfiguration
@Import({
  LanguageServerConfiguration.class,
  ServerContext.class,
  DiagnosticInfosConfiguration.class
})
//@ComponentScan(
//  value = "com.github._1c_syntax.bsl.languageserver",
//  lazyInit = true
//)
public class BSLLSBinding {

  private static final ApplicationContext context = createContext();

  @SuppressWarnings("unchecked")
  public static Collection<DiagnosticInfo> getDiagnosticInfos() {
    return (Collection<DiagnosticInfo>) context.getBean("diagnosticInfos", Map.class)
      .values();
  }

  public static LanguageServerConfiguration getLanguageServerConfiguration() {
    return context.getBean(LanguageServerConfiguration.class);
  }

  public static ServerContext getServerContext() {
    return context.getBean(ServerContext.class);
  }

  private static ApplicationContext createContext() {
    return new SpringApplicationBuilder(BSLLSBinding.class)
      .bannerMode(Banner.Mode.OFF)
      .web(WebApplicationType.NONE)
      .logStartupInfo(false)
      .build()
      .run();
  }
}
