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
import static com.google.eclipse.terminal.local.ui.preferences.PreferenceNames.*;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
  @Override public void initializeDefaultPreferences() {
    preferenceStore().setDefault(CLOSE_VIEW_ON_EXIT, true);
    setDefault(BACKGROUND_COLOR, new RGB(0, 0, 0));
    setDefault(FOREGROUND_COLOR, new RGB(229, 229, 229));
  }

  private void setDefault(String name, RGB value) {
    PreferenceConverter.setDefault(preferenceStore(), name, value);
  }
}
