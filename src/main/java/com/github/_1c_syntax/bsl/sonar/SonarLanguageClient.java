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
package com.github._1c_syntax.bsl.sonar;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class SonarLanguageClient implements LanguageClient {

  @Override
  public void telemetryEvent(Object object) {
    LOGGER.debug("Telemetry event: {}", object);
  }

  @Override
  public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
    // no-op
  }

  @Override
  public void showMessage(MessageParams messageParams) {
    logIncomingMessage(messageParams);
  }

  @Override
  public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
    LOGGER.info("Incoming message: {}", requestParams);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void logMessage(MessageParams message) {
    logIncomingMessage(message);
  }

  private void logIncomingMessage(MessageParams message) {
    var messageType = message.getType();

    switch (messageType) {
      case Error:
        LOGGER.error(message.getMessage());
        break;
      case Warning:
        LOGGER.warn(message.getMessage());
        break;
      case Info:
        LOGGER.info(message.getMessage());
        break;
      case Log:
        LOGGER.debug(message.getMessage());
        break;
    }
  }
}
