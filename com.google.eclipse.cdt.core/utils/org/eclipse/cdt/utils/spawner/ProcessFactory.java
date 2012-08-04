/*******************************************************************************
 * Copyright (c) 2000, 2009 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.utils.spawner;

import java.io.*;

import org.eclipse.cdt.utils.pty.PTY;

import com.google.eclipse.cdt.core.CCorePlugin;

public class ProcessFactory {
  static private ProcessFactory instance;

  private boolean hasSpawner;
  private final Runtime runtime;

  private ProcessFactory() {
    hasSpawner = false;
    String OS = System.getProperty("os.name").toLowerCase();
    runtime = Runtime.getRuntime();
    // Spawner does not work for Windows 98 fallback
    if (OS != null && OS.equals("windows 98")) {
      hasSpawner = false;
    } else {
      System.loadLibrary(Spawner.LIBRARY_NAME);
      hasSpawner = true;
    }
  }

  public static ProcessFactory getFactory() {
    if (instance == null) {
      instance = new ProcessFactory();
    }
    return instance;
  }

  public Process exec(String cmd) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmd);
    }
    return runtime.exec(cmd);
  }

  public Process exec(String[] cmdarray) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmdarray);
    }
    return runtime.exec(cmdarray);
  }

  public Process exec(String[] cmdarray, String[] envp) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmdarray, envp);
    }
    return runtime.exec(cmdarray, envp);
  }

  public Process exec(String cmd, String[] envp) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmd, envp);
    }
    return runtime.exec(cmd, envp);
  }

  public Process exec(String cmd, String[] envp, File dir) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmd, envp, dir);
    }
    return runtime.exec(cmd, envp, dir);
  }

  public Process exec(String cmdarray[], String[] envp, File dir) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmdarray, envp, dir);
    }
    return runtime.exec(cmdarray, envp, dir);
  }

  public Process exec(String cmdarray[], String[] envp, File dir, PTY pty) throws IOException {
    if (hasSpawner) {
      return new Spawner(cmdarray, envp, dir, pty);
    }
    throw new UnsupportedOperationException(CCorePlugin.getResourceString("Util.exception.cannotCreatePty"));
  }
}
