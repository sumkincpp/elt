/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.core.connector;

import static com.google.eclipse.terminal.local.util.Platform.*;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.cdt.utils.spawner.ProcessFactory;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class PseudoTerminal {
  private final List<LifeCycleListener> lifeCycleListeners = new ArrayList<LifeCycleListener>();
  private final ExecutorService executor = newSingleThreadExecutor();

  private final File workingDirectory;

  private Process process;
  private PTY pty;

  private int width;
  private int height;

  PseudoTerminal(File workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  void launch() throws IOException {
    ProcessFactory factory = ProcessFactory.getFactory();
    pty = new PTY(false);
    process = factory.exec(command(), environment(), workingDirectory, pty);
    executor.execute(new Runnable() {
      @Override public void run() {
        try {
          process.waitFor();
        } catch (InterruptedException ignored) {
          currentThread().interrupt();
        } finally {
          notifyExecutionFinished();
        }
      }
    });
  }

  private String[] command() {
    return new String[] { defaultShell().getAbsolutePath(), "-i" };
  }

  private void notifyExecutionFinished() {
    for (LifeCycleListener listener : lifeCycleListeners) {
      listener.executionFinished();
    }
  }

  void addLifeCycleListener(LifeCycleListener listener) {
    lifeCycleListeners.add(listener);
  }

  Process systemProcess() {
    return process;
  }

  void updateSize(int newWidth, int newHeight) {
    if (pty != null && (newWidth != width || newHeight != height)) {
      pty.setTerminalSize(width, height);
      width = newWidth;
      height = newHeight;
    }
  }

  static boolean isPlatformSupported() {
    return PTY.isSupported();
  }

  void disconnect() {
    if (process != null) {
      try {
        process.exitValue();
      } catch (IllegalThreadStateException e) {
        process.destroy();
      }
    }
  }
}
