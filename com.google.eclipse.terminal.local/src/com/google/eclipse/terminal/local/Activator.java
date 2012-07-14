/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local;

import static com.google.eclipse.terminal.local.ImageKeys.*;
import static org.eclipse.core.runtime.IStatus.*;

import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.*;
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

  @Override protected void initializeImageRegistry(ImageRegistry registry) {
    addImage(registry, CHANGE_TITLE, "icons/change_title.gif");
    addImage(registry, NEW_TERMINAL, "icons/new_terminal.gif");
    addImage(registry, SCROLL_LOCK, "icons/scroll_lock.gif");
  }

  private void addImage(ImageRegistry registry, String key, String path) {
    URL imageUrl = instance().getBundle().getEntry(path);
    registry.put(key, ImageDescriptor.createFromURL(imageUrl));

  }

  public static void log(String message, Throwable cause) {
    log(new Status(ERROR, PLUGIN_ID, OK, message, cause));
  }

  public static void log(IStatus status) {
    instance().getLog().log(status);
  }

  public static ImageDescriptor imageDescriptor(String key) {
    return instance().getImageRegistry().getDescriptor(key);
  }

  public static Activator instance() {
    return plugin;
  }

  public static IPreferenceStore preferenceStore() {
    return instance().getPreferenceStore();
  }
}
