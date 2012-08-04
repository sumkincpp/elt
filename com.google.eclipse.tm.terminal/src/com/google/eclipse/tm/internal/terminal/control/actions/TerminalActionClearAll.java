/*******************************************************************************
 * Copyright (c) 2004, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ********************************************************************************/
package com.google.eclipse.tm.internal.terminal.control.actions;

import static com.google.eclipse.tm.internal.terminal.control.actions.ActionMessages.clearAll;
import static com.google.eclipse.tm.internal.terminal.control.actions.Images.*;

import com.google.eclipse.tm.internal.terminal.control.ITerminalViewControl;

public class TerminalActionClearAll extends AbstractTerminalAction {
  public TerminalActionClearAll() {
    super(TerminalActionClearAll.class.getName());
    setupAction();
  }

  public TerminalActionClearAll(ITerminalViewControl target) {
    super(target, TerminalActionClearAll.class.getName());
    setupAction();
  }

  private void setupAction() {
    setUpAction(clearAll, clearAll, IMAGE_CLCL_CLEAR_ALL, IMAGE_ELCL_CLEAR_ALL, IMAGE_DLCL_CLEAR_ALL, false);
  }

  @Override public void run() {
    ITerminalViewControl target = getTarget();
    if (target != null) {
      target.clearTerminal();
    }
  }

  @Override public void updateAction(boolean aboutToShow) {
    ITerminalViewControl target = getTarget();
    setEnabled(target != null && !target.isEmpty());
  }
}
