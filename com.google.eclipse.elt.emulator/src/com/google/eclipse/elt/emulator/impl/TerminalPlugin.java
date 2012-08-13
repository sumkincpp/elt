/*******************************************************************************
 * Copyright (c) 2003, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.impl;

import static com.google.eclipse.elt.emulator.actions.Images.*;
import static org.eclipse.core.runtime.IStatus.ERROR;

import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class TerminalPlugin extends AbstractUIPlugin {
  public static final String PLUGIN_ID = "com.google.eclipse.elt.emulator";

  protected static TerminalPlugin plugin;

  public TerminalPlugin() {
    plugin = this;
  }

  public static TerminalPlugin getDefault() {
    return plugin;
  }

  public static boolean isOptionEnabled(String option) {
    String enabled = Platform.getDebugOption(option);
    if (enabled == null) {
      return false;
    }
    return Boolean.parseBoolean(enabled);
  }

  @Override protected void initializeImageRegistry(ImageRegistry imageRegistry) {
    // Local tool-bars
    putImageInRegistry(imageRegistry, IMAGE_CLCL_CLEAR_ALL, IMAGE_DIR_LOCALTOOL + "clear_co.gif");
    // Enabled local tool-bars
    putImageInRegistry(imageRegistry, IMAGE_ELCL_CLEAR_ALL, IMAGE_DIR_ELCL + "clear_co.gif");
    // Disabled local tool-bars
    putImageInRegistry(imageRegistry, IMAGE_DLCL_CLEAR_ALL, IMAGE_DIR_DLCL + "clear_co.gif");
  }

  protected void putImageInRegistry(ImageRegistry imageRegistry, String strKey, String relativePath) {
    URL url = TerminalPlugin.getDefault().getBundle().getEntry(relativePath);
    ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
    imageRegistry.put(strKey, imageDescriptor);
  }

  public static void log(String message, Exception error) {
    getDefault().getLog().log(new Status(ERROR, PLUGIN_ID, message, error));
  }
}
