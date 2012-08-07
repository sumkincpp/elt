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
import static com.google.eclipse.elt.view.preferences.Messages.*;
import static com.google.eclipse.elt.view.preferences.PreferenceNames.*;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class RootPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  public RootPreferencePage() {
  }
  private static final int MINIMUM_BUFFER_LINE_COUNT = 100;
  private static final int MAXIMUM_BUFFER_LINE_COUNT = 50000;
  private static final String INVALID_BUFFER_LINE_COUNT_MESSAGE =
      NLS.bind(invalidBufferLineCount, MINIMUM_BUFFER_LINE_COUNT, MAXIMUM_BUFFER_LINE_COUNT);

  private Text txtBufferLineCount;
  private Button btnCloseViewOnExit;
  private Button btnWarnOnClose;
  private Button btnUseBlinkingCursor;

  private int newBufferLineCount;

  @Override public void init(IWorkbench workbench) {
    setPreferenceStore(preferenceStore());
  }

  @Override protected Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    contents.setLayout(new GridLayout(2, false));

    Label lblGeneralPreferences = new Label(contents, SWT.NONE);
    lblGeneralPreferences.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
    lblGeneralPreferences.setText(generalPreferencesTitle);

    Label filler = new Label(contents, SWT.NONE);
    filler.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

    Label lblBufferLineCount = new Label(contents, SWT.NONE);
    lblBufferLineCount.setText(bufferLineCount);

    txtBufferLineCount = new Text(contents, SWT.BORDER);
    txtBufferLineCount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
    txtBufferLineCount.addModifyListener(new ModifyListener() {
      @Override public void modifyText(ModifyEvent event) {
        try {
          newBufferLineCount = Integer.parseInt(txtBufferLineCount.getText());
        } catch (NumberFormatException e) {
          setInvalid(INVALID_BUFFER_LINE_COUNT_MESSAGE);
          return;
        }
        if (newBufferLineCount < MINIMUM_BUFFER_LINE_COUNT || newBufferLineCount > MAXIMUM_BUFFER_LINE_COUNT) {
          setInvalid(INVALID_BUFFER_LINE_COUNT_MESSAGE);
          return;
        }
        setErrorMessage(null);
        setValid(true);
      }
    });

    btnCloseViewOnExit = new Button(contents, SWT.CHECK);
    btnCloseViewOnExit.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    btnCloseViewOnExit.setText(closeViewOnExit);

    btnWarnOnClose = new Button(contents, SWT.CHECK);
    btnWarnOnClose.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    btnWarnOnClose.setText(warnOnClose);

    btnUseBlinkingCursor = new Button(contents, SWT.CHECK);
    btnUseBlinkingCursor.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
    btnUseBlinkingCursor.setText(useBlinkingCursor);

    updateContents();
    return contents;
  }

  private void setInvalid(String errorMessage) {
    setErrorMessage(errorMessage);
    setValid(false);
  }

  private void updateContents() {
    txtBufferLineCount.setText(getPreferenceStore().getString(BUFFER_LINE_COUNT));
    btnCloseViewOnExit.setSelection(getPreferenceStore().getBoolean(CLOSE_VIEW_ON_EXIT));
    btnWarnOnClose.setSelection(getPreferenceStore().getBoolean(WARN_ON_CLOSE));
    btnUseBlinkingCursor.setSelection(getPreferenceStore().getBoolean(USE_BLINKING_CURSOR));
  }

  @Override public boolean performOk() {
    getPreferenceStore().setValue(BUFFER_LINE_COUNT, newBufferLineCount);
    getPreferenceStore().setValue(CLOSE_VIEW_ON_EXIT, btnCloseViewOnExit.getSelection());
    getPreferenceStore().setValue(WARN_ON_CLOSE, btnWarnOnClose.getSelection());
    getPreferenceStore().setValue(USE_BLINKING_CURSOR, btnUseBlinkingCursor.getSelection());
    return true;
  }

  @Override protected void performDefaults() {
    txtBufferLineCount.setText(getPreferenceStore().getDefaultString(BUFFER_LINE_COUNT));
    btnCloseViewOnExit.setSelection(getPreferenceStore().getDefaultBoolean(CLOSE_VIEW_ON_EXIT));
    btnWarnOnClose.setSelection(getPreferenceStore().getDefaultBoolean(WARN_ON_CLOSE));
    btnUseBlinkingCursor.setSelection(getPreferenceStore().getDefaultBoolean(USE_BLINKING_CURSOR));
  }
}
