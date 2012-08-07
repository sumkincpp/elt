/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.ui;

import static org.eclipse.jface.dialogs.IDialogConstants.*;

import static com.google.eclipse.elt.view.preferences.GeneralPreferences.warnOnClose;
import static com.google.eclipse.elt.view.ui.Messages.*;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class WarnOnCloseDialog extends MessageDialog {
  private static final String[] BUTTON_LABELS = { YES_LABEL, NO_LABEL };

  private Button btnCloseWithoutWarn;

  static boolean open(Shell parent) {
    WarnOnCloseDialog dialog = new WarnOnCloseDialog(parent);
    return dialog.open() == OK;
  }

  private WarnOnCloseDialog(Shell parentShell) {
    super(parentShell, confirmCloseDialogTitle, null, closeTerminalQuestion, QUESTION, BUTTON_LABELS, 0);
  }

  @Override protected Control createCustomArea(Composite parent) {
    btnCloseWithoutWarn = new Button(parent, SWT.CHECK);
    btnCloseWithoutWarn.setText(alwaysCloseWithoutWarn);
    btnCloseWithoutWarn.setSelection(!warnOnClose());
    return btnCloseWithoutWarn;
  }

  @Override protected void buttonPressed(int buttonId) {
    boolean closeWithoutWarn = btnCloseWithoutWarn.getSelection();
    warnOnClose(!closeWithoutWarn);
    super.buttonPressed(buttonId);
  }
}
