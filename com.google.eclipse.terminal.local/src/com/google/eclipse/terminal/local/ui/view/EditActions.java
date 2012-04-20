/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.view;

import static org.eclipse.ui.actions.ActionFactory.*;

import org.eclipse.jface.action.*;
import org.eclipse.tm.internal.terminal.control.actions.*;
import org.eclipse.tm.internal.terminal.emulator.VT100TerminalControl;
import org.eclipse.ui.IActionBars;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class EditActions {
  private final AbstractTerminalAction copy;
  private final AbstractTerminalAction paste;
  private final AbstractTerminalAction clearAll;
  private final AbstractTerminalAction selectAll;

  EditActions(VT100TerminalControl terminalControl) {
    copy = new TerminalActionCopy(terminalControl);
    paste = new TerminalActionPaste(terminalControl);
    clearAll = new TerminalActionClearAll(terminalControl);
    selectAll = new TerminalActionSelectAll(terminalControl);
  }

  void addActionsTo(IMenuManager menuManager) {
    menuManager.add(copy);
    menuManager.add(paste);
    menuManager.add(new Separator());
    menuManager.add(clearAll);
    menuManager.add(selectAll);
  }

  void update() {
    update(copy, paste, clearAll, selectAll);
  }

  private void update(AbstractTerminalAction...actions) {
    for (AbstractTerminalAction action : actions) {
      action.updateAction(true);
    }
  }

  void onMenuHidden() {
    copy.updateAction(false);
  }

  void setUpGlobalActionHandlers(IActionBars actionBars) {
    actionBars.setGlobalActionHandler(COPY.getId(), copy);
    actionBars.setGlobalActionHandler(PASTE.getId(), paste);
    actionBars.setGlobalActionHandler(SELECT_ALL.getId(), selectAll);
  }
}