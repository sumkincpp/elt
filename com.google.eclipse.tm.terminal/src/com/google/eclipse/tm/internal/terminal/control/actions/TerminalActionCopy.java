/*******************************************************************************
 * Copyright (c) 2004, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.control.actions;

import static org.eclipse.ui.ISharedImages.*;

import static com.google.eclipse.tm.internal.terminal.control.actions.ActionMessages.copy;

import org.eclipse.ui.*;

import com.google.eclipse.tm.internal.terminal.control.ITerminalViewControl;

public class TerminalActionCopy extends AbstractTerminalAction {
  public TerminalActionCopy() {
    super(TerminalActionCopy.class.getName());
    setUpAction();
  }

  public TerminalActionCopy(ITerminalViewControl target) {
    super(target, TerminalActionCopy.class.getName());
    setUpAction();
  }

  private void setUpAction() {
    ISharedImages shared = PlatformUI.getWorkbench().getSharedImages();
    setUpAction(
        copy, copy, shared.getImageDescriptor(IMG_TOOL_COPY), shared.getImageDescriptor(IMG_TOOL_COPY),
        shared.getImageDescriptor(IMG_TOOL_COPY_DISABLED), true);
  }

  @Override public void run() {
    ITerminalViewControl target = getTarget();
    if (target != null) {
      String selection = target.getSelection();
      if (!selection.isEmpty()) {
        target.copy();
      } else {
        target.sendKey('\u0003');
      }
    }
  }

  @Override public void updateAction(boolean aboutToShow) {
    ITerminalViewControl target = getTarget();
    boolean enabled = target != null;
    if (aboutToShow && enabled) {
      enabled = target.getSelection().length() > 0;
    }
    setEnabled(enabled);
  }
}
