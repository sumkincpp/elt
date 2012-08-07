/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.pty;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

import com.ibm.icu.text.MessageFormat;

public class PtyPlugin extends Plugin {
  public static final String PLUGIN_ID = "org.eclipse.cdt.core";

  private static PtyPlugin plugin;
  private static ResourceBundle resourceBundle;

  static {
    try {
      resourceBundle = ResourceBundle.getBundle("com.google.eclipse.elt.pty.internal.PtyPluginResources");
    } catch (MissingResourceException x) {
      resourceBundle = null;
    }
  }

  public PtyPlugin() {
    plugin = this;
  }

  public static String getResourceString(String key) {
    try {
      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return "!" + key + "!";
    } catch (NullPointerException e) {
      return "#" + key + "#";
    }
  }

  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  public static String getFormattedString(String key, String arg) {
    return MessageFormat.format(getResourceString(key), new Object[] { arg });
  }

  public static String getFormattedString(String key, String[] args) {
    return MessageFormat.format(getResourceString(key), (Object[]) args);
  }

  public static ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  public static PtyPlugin getDefault() {
    return plugin;
  }

  public static void log(Throwable e) {
    String msg = e.getMessage();
    if (msg == null) {
      log("Error", e);
    } else {
      log("Error: " + msg, e);
    }
  }

  public static void log(String message, Throwable e) {
    log(createStatus(message, e));
  }

  public static IStatus createStatus(String message) {
    return createStatus(message, null);
  }

  public static IStatus createStatus(String message, Throwable exception) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message, exception);
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }
}
