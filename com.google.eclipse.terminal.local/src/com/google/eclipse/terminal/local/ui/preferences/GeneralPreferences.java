/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.preferences;

import static com.google.eclipse.terminal.local.Activator.preferenceStore;
import static com.google.eclipse.terminal.local.ui.preferences.PreferenceNames.CLOSE_VIEW_ON_EXIT;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public final class GeneralPreferences {
  public static boolean closeViewOnExit() {
    return preferenceStore().getBoolean(CLOSE_VIEW_ON_EXIT);
  }

  private GeneralPreferences() {}
}
