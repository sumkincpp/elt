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
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.tm.internal.terminal.control.actions.*;
import org.eclipse.tm.internal.terminal.emulator.VT100TerminalControl;
import org.eclipse.ui.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class PopupMenu {
  private final AbstractTerminalAction copy;
  private final AbstractTerminalAction paste;
  private final AbstractTerminalAction selectAll;
  private final AbstractTerminalAction clearAll;

  PopupMenu(IViewSite viewSite, VT100TerminalControl terminalControl) {
    PopupMenuManager menuManager = new PopupMenuManager();
    copy = menuManager.add(new TerminalActionCopy(terminalControl));
    copy.setActionDefinitionId("com.google.eclipse.terminal.local.copy");
    paste = menuManager.add(new TerminalActionPaste(terminalControl));
    paste.setActionDefinitionId("com.google.eclipse.terminal.local.paste");
    menuManager.add(new Separator());
    selectAll = menuManager.add(new TerminalActionSelectAll(terminalControl));
    clearAll = menuManager.add(new TerminalActionClearAll(terminalControl));
    IActionBars actionBars = viewSite.getActionBars();
    actionBars.setGlobalActionHandler(COPY.getId(), copy);
    actionBars.setGlobalActionHandler(PASTE.getId(), paste);
    actionBars.setGlobalActionHandler(SELECT_ALL.getId(), selectAll);
    IKeyBindingService keyBindingService = viewSite.getKeyBindingService();
    keyBindingService.registerAction(copy);
    keyBindingService.registerAction(paste);
    menuManager.addMenuListener(new IMenuListener() {
      @Override public void menuAboutToShow(IMenuManager manager) {
        update(copy, paste, selectAll, clearAll);
      }
    });
    Control control = terminalControl.getControl();
    Menu menu = menuManager.createContextMenu(control);
    control.setMenu(menu);
    menu.addMenuListener(new MenuAdapter() {
      @Override public void menuHidden(MenuEvent e) {
        copy.updateAction(false);
      }
    });
  }

  void update() {
    update(copy, paste, selectAll, clearAll);
  }

  private void update(AbstractTerminalAction...actions) {
    for (AbstractTerminalAction action : actions) {
      action.updateAction(true);
    }
  }

  private static class PopupMenuManager extends MenuManager {
    PopupMenuManager() {
      super("#PopupMenu");
    }

    AbstractTerminalAction add(AbstractTerminalAction action) {
      super.add(action);
      return action;
    }
  }
}