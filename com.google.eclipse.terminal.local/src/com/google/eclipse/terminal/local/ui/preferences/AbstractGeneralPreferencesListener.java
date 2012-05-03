/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.preferences;

import static com.google.eclipse.terminal.local.ui.preferences.PreferenceNames.BUFFER_LINE_COUNT;

import org.eclipse.jface.util.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public abstract class AbstractGeneralPreferencesListener implements IPropertyChangeListener {
  @Override public final void propertyChange(PropertyChangeEvent event) {
    if (BUFFER_LINE_COUNT.equals(event.getProperty())) {
      onBufferLineCountChanged();
    }
  }

  protected abstract void onBufferLineCountChanged();
}