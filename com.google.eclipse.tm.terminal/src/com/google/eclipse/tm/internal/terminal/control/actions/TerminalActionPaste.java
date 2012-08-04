/*******************************************************************************
 * Copyright (c) 2004, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Initial Contributors:
 * The following Wind River employees contributed to the Terminal component
 * that contains this file: Chris Thew, Fran Litterio, Stephen Lamb,
 * Helmut Haigermoser and Ted Williams.
 *
 * Contributors:
 * Michael Scharf (Wind River) - split into core, view and connector plugins
 * Martin Oberhuber (Wind River) - fixed copyright headers and beautified
 * Anna Dushistova (MontaVista) - [227537] moved actions from terminal.view to terminal plugin
 * Uwe Stieber (Wind River) - [260372] [terminal] Certain terminal actions are enabled if no target terminal control is available
 * Uwe Stieber (Wind River) - [294719] [terminal] SWT Widget disposed in TerminalActionPaste
 * Martin Oberhuber (Wind River) - [296212] Cannot paste text into terminal on some Linux hosts
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.control.actions;

import static org.eclipse.ui.ISharedImages.*;

import static com.google.eclipse.tm.internal.terminal.control.actions.ActionMessages.paste;
import static com.google.eclipse.tm.internal.terminal.provisional.api.TerminalState.CONNECTED;

import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.*;

import com.google.eclipse.tm.internal.terminal.control.ITerminalViewControl;

public class TerminalActionPaste extends AbstractTerminalAction {
  public TerminalActionPaste() {
    super(TerminalActionPaste.class.getName());
    setUpAction();
  }

  public TerminalActionPaste(ITerminalViewControl target) {
    super(target, TerminalActionPaste.class.getName());
    setUpAction();
  }

  private void setUpAction() {
    ISharedImages shared = PlatformUI.getWorkbench().getSharedImages();
    setUpAction(
        paste, paste, shared.getImageDescriptor(IMG_TOOL_PASTE), shared.getImageDescriptor(IMG_TOOL_PASTE_DISABLED),
        shared.getImageDescriptor(IMG_TOOL_PASTE), false);
  }

  @Override public void run() {
    ITerminalViewControl target = getTarget();
    if (target != null) {
      target.paste();
    }
  }

  @Override public void updateAction(boolean aboutToShow) {
    ITerminalViewControl target = getTarget();
    boolean enabled = target != null && target.getClipboard() != null && !target.getClipboard().isDisposed();
    if (enabled) {
      String text = (String) target.getClipboard().getContents(TextTransfer.getInstance());
      enabled = text != null && !text.isEmpty() && target.getState() == CONNECTED;
    }
    setEnabled(enabled);
  }
}
