/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.preferences;

import static com.google.eclipse.terminal.local.ui.preferences.PreferenceNames.*;

import org.eclipse.jface.util.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public abstract class AbstractColorsAndFontsPreferencesChangeListener implements IPropertyChangeListener {
  @Override public final void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();
    if (BACKGROUND_COLOR.equals(property) || FOREGROUND_COLOR.equals(property)) {
      onColorChanged();
    }
  }

  protected abstract void onColorChanged();
}
