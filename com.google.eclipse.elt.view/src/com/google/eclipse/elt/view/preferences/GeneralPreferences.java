/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.preferences;

import static com.google.eclipse.elt.view.Activator.preferenceStore;
import static com.google.eclipse.elt.view.preferences.PreferenceNames.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public final class GeneralPreferences {
  public static int bufferLineCount() {
    return preferenceStore().getInt(BUFFER_LINE_COUNT);
  }

  public static boolean closeViewOnExit() {
    return preferenceStore().getBoolean(CLOSE_VIEW_ON_EXIT);
  }

  public static boolean warnOnClose() {
    return preferenceStore().getBoolean(WARN_ON_CLOSE);
  }

  public static void warnOnClose(boolean newValue) {
    preferenceStore().setValue(WARN_ON_CLOSE, newValue);
  }

  private GeneralPreferences() {}
}
