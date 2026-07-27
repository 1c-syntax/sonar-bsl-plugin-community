/*
 * This file is a part of SonarQube 1C (BSL) Community Plugin.
 *
 * Copyright (c) 2018-2026
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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BSLCoreSensorPoolTest {

  @Test
  void wrapsExecutionExceptionAsIllegalState() throws Exception {
    var executor = mock(ExecutorService.class);
    Future<?> future = mock(Future.class);
    doReturn(future).when(executor).submit(any(Runnable.class));
    when(future.get()).thenThrow(new ExecutionException(new RuntimeException("boom")));

    var sourceDir = Path.of("sourceDir");
    assertThatThrownBy(() ->
      BSLCoreSensor.awaitInWorkspacePool(executor, sourceDir, () -> { }))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Error processing files in")
      .hasMessageContaining("sourceDir");
  }

  @Test
  void wrapsInterruptedExceptionAndRestoresInterruptFlag() throws Exception {
    var executor = mock(ExecutorService.class);
    Future<?> future = mock(Future.class);
    doReturn(future).when(executor).submit(any(Runnable.class));
    when(future.get()).thenThrow(new InterruptedException("interrupted"));

    var sourceDir = Path.of("sourceDir");
    assertThatThrownBy(() ->
      BSLCoreSensor.awaitInWorkspacePool(executor, sourceDir, () -> { }))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Interrupted while processing files in");

    // The catch block restores the thread's interrupt status; consume it via
    // Thread.interrupted() so the flag does not leak into other tests on this thread.
    assertThat(Thread.interrupted()).isTrue();
  }
}
