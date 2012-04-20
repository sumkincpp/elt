/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local;

import static org.eclipse.core.runtime.IStatus.*;

import org.eclipse.core.runtime.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
public class Activator extends AbstractUIPlugin {
  public static final String PLUGIN_ID = "com.google.eclipse.terminal.local";

  private static Activator plugin;

  @Override public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }

  @Override public void stop(BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  public static void log(String message, Throwable cause) {
    log(new Status(ERROR, PLUGIN_ID, OK, message, cause));
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  public static Activator getDefault() {
    return plugin;
  }
}
