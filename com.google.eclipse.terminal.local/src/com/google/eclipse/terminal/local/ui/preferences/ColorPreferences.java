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

import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class ColorPreferences {

  public static RGB background() {
    return PreferenceConverter.getColor(preferenceStore(), BACKGROUND_COLOR);
  }

  public static RGB foreground() {
    return PreferenceConverter.getColor(preferenceStore(), FOREGROUND_COLOR);
  }
}
