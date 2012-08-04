/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.util;

import static java.util.Collections.singletonMap;

import static org.eclipse.core.runtime.Platform.*;

import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;

/**
 * Utility methods related to the underlying platform.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
public final class Platform {
  // TODO investigate if "TERM" should be "xterm-color"
  private static final Map<String, String> TERM_ANSI = singletonMap("TERM", "ansi");
  private static final String ENVIRONMENT_VARIABLE_FORMAT = "%s=%s";

  /**
   * Returns the system's default shell.
   * @return the system's default shell.
   */
  public static File defaultShell() {
    String shell = System.getenv("SHELL");
    if (shell == null) {
      shell = (runningOnWindows()) ? "C:\\Windows\\System32\\cmd.exe" : "/bin/sh";
    }
    return new File(shell);
  }

  private static boolean runningOnWindows() {
    return OS_WIN32.equals(getOS());
  }

  /**
   * Returns an array of environment variables. Each entry is of the form "<code>name=value</code>".
   * @return an array of environment variables.
   */
  public static String[] environment() {
    Map<String, String> environment = new HashMap<String, String>();
    environment.putAll(TERM_ANSI);
    environment.putAll(nativeEnvironment());
    List<String> variables = new ArrayList<String>(environment.size());
    for (Map.Entry<String, String> entry : environment.entrySet()) {
      String variable = String.format(ENVIRONMENT_VARIABLE_FORMAT, entry.getKey(), entry.getValue());
      variables.add(variable);
    }
    return variables.toArray(new String[variables.size()]);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> nativeEnvironment() {
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    return launchManager.getNativeEnvironmentCasePreserved();
  }

  /**
   * Returns the path of the user home directory.
   * @return the path of the user home directory or {@code null} if it cannot be found.
   */
  public static IPath userHomeDirectory() {
    String path = System.getProperty("user.home", "/");
    File userHome = new File(path);
    return (userHome.isDirectory()) ? new Path(path) : null;
  }

  private Platform() {}
}
