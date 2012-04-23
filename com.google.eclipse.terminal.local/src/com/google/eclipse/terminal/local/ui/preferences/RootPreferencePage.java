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
import static com.google.eclipse.terminal.local.ui.preferences.Messages.*;
import static com.google.eclipse.terminal.local.ui.preferences.PreferenceNames.CLOSE_VIEW_ON_EXIT;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class RootPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private Button btnCloseViewOnExit;

  @Override public void init(IWorkbench workbench) {
    setPreferenceStore(preferenceStore());
  }

  @Override protected Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    contents.setLayout(new GridLayout(1, false));

    Label lblGeneralPreferences = new Label(contents, SWT.NONE);
    lblGeneralPreferences.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
    lblGeneralPreferences.setText(generalPreferencesTitle);
    new Label(contents, SWT.NONE);

    btnCloseViewOnExit = new Button(contents, SWT.CHECK);
    btnCloseViewOnExit.setText(closeViewOnExit);

    updateContents();
    return contents;
  }

  private void updateContents() {
    btnCloseViewOnExit.setSelection(getPreferenceStore().getBoolean(CLOSE_VIEW_ON_EXIT));
  }

  @Override public boolean performOk() {
    getPreferenceStore().setValue(CLOSE_VIEW_ON_EXIT, btnCloseViewOnExit.getSelection());
    return true;
  }

  @Override protected void performDefaults() {
    btnCloseViewOnExit.setSelection(getPreferenceStore().getDefaultBoolean(CLOSE_VIEW_ON_EXIT));
  }
}
