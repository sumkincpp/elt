/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.view;

import java.util.logging.*;

import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.keys.*;
import org.eclipse.ui.internal.keys.WorkbenchKeyboard.KeyDownFilter;
import org.eclipse.ui.keys.IBindingService;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
@SuppressWarnings("restriction")
class CommandLauncher extends KeyAdapter {
  private static Logger logger = Logger.getLogger(CommandLauncher.class.getCanonicalName());

  @Override public void keyPressed(KeyEvent e) {
    if (!e.doit) {
      return;
    }
    IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
    // Necessary to handle copy/paste/"select all" keyboard accelerators.
    if (bindingService instanceof BindingService) {
      KeyDownFilter filter = ((BindingService) bindingService).getKeyboard().getKeyDownFilter();
      Control focusControl = e.display.getFocusControl();
      boolean enabled = filter.isEnabled();
      try {
        filter.setEnabled(true);
        filter.handleEvent(copyOf(e));
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Unable to handle event: " + e, t);
      } finally {
        if (focusControl == e.display.getFocusControl() && !enabled) {
          filter.setEnabled(enabled);
        }
      }
    }
  }

  private Event copyOf(KeyEvent e) {
    Event event = new Event();
    event.character = e.character;
    event.data = e.data;
    event.display = e.display;
    event.doit = e.doit;
    event.keyCode = e.keyCode;
    event.keyLocation = e.keyLocation;
    event.stateMask = e.stateMask;
    event.time = e.time;
    event.widget = e.widget;
    return event;
  }
}
