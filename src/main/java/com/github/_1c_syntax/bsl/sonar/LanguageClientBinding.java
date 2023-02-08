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

// todo: interface в api. Либо сделать ProtocolExtension extends LanguageServer и тогда можно использовать родную
// реализацию через setRemoteInterface

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Slf4j
public class LanguageClientBinding {

  public static LanguageClientBinding INSTANCE = new LanguageClientBinding();

  {
//    INSTANCE.start();
  }

  private Process process;

  @Getter
  private BSLLanguageServerInterface server;

  public void start() {
    createProcess();
    connectToProcess();
  }

  public void stop() {
    if (server != null) {
      server.shutdown();
      server.exit();
    }
    clear();
  }

  public void restart() {
    stop();
    start();
  }

  public boolean isLaunched() {
    return process != null && process.isAlive();
  }

  @SneakyThrows
  private void createProcess() {
    var pathToWorkspace = getPathToWorkspace();
    var pathToLSP = getPathToBSLLS();

    List<String> arguments = new ArrayList<>();
    arguments.add("java");
    arguments.add("-jar");
    arguments.add("\"" + pathToLSP + "\"");

    try {
      process = new ProcessBuilder()
        .command(arguments)
        .directory(pathToWorkspace.toFile())
        .start();

      Thread.sleep(500);

      if (!process.isAlive()) {
        LOGGER.warn("Не удалось запустить процесс с BSL LS. Процесс был аварийно завершен.");
      }
    } catch (IOException e) {
      LOGGER.error("Не удалось запустить процесс BSL LS", e);
    }
  }

  @NotNull
  private static Path getPathToWorkspace() {
    return Paths.get(".").toAbsolutePath();
  }

  @SneakyThrows
  private void connectToProcess() {
    if (process == null) {
      return;
    }

    var launcher = new LSPLauncher.Builder<BSLLanguageServerInterface>()
      .setLocalService(new SonarLanguageClient())
      .setRemoteInterface(BSLLanguageServerInterface.class)
      .setInput(process.getInputStream())
      .setOutput(process.getOutputStream())
      .create();

    server = launcher.getRemoteProxy();

    Executors.newCachedThreadPool().execute(() -> {
        var future = launcher.startListening();
        while (true) {
          try {
            future.get();
            break;
          } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
          }
        }
      }
    );

    Thread.sleep(2000);

    var params = new InitializeParams();

    params.setProcessId((int) ProcessHandle.current().pid());

    var workspaceFolder = new WorkspaceFolder(getPathToWorkspace().toUri().toString());
    params.setWorkspaceFolders(List.of(workspaceFolder));

    var clientCapabilities = new ClientCapabilities();

    var textDocument = new TextDocumentClientCapabilities();
    clientCapabilities.setTextDocument(textDocument);

    params.setCapabilities(clientCapabilities);

    var initializeResult = server.initialize(params).get();

    LOGGER.info("BSL LS version: {}", initializeResult.getServerInfo().getVersion());
  }

  private void clear() {
    process = null;
    server = null;
  }

  private static Path getPathToBSLLS() {
    return Path.of(
      "d:",
      "git",
      "1c-syntax",
      "bsl-language-server",
      "build",
      "libs",
      "bsl-language-server-develop-004e007-DIRTY-exec.jar"
    );
  }

}
