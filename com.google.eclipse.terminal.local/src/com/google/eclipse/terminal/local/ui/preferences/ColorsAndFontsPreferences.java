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
import static org.eclipse.jface.preference.PreferenceConverter.*;

import org.eclipse.swt.graphics.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class ColorsAndFontsPreferences {

  public static RGB background() {
    return getColor(preferenceStore(), BACKGROUND_COLOR);
  }

  public static RGB foreground() {
    return getColor(preferenceStore(), FOREGROUND_COLOR);
  }

  public static boolean useCustomFont() {
    return preferenceStore().getBoolean(USE_CUSTOM_FONT);
  }

  public static FontData customFontData() {
    return getFontData(preferenceStore(), CUSTOM_FONT_DATA);
  }
}
