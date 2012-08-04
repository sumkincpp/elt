/*******************************************************************************
 * Copyright (c) 2004, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.control.actions;

import static org.eclipse.ui.ISharedImages.*;

import static com.google.eclipse.tm.internal.terminal.control.actions.ActionMessages.cut;

import org.eclipse.ui.*;

import com.google.eclipse.tm.internal.terminal.control.ITerminalViewControl;

public class TerminalActionCut extends AbstractTerminalAction {
  public TerminalActionCut() {
    super(TerminalActionCut.class.getName());
    setUpAction();
  }

  public TerminalActionCut(ITerminalViewControl target) {
    super(target, TerminalActionCut.class.getName());
    setUpAction();
  }

  private void setUpAction() {
    ISharedImages shared = PlatformUI.getWorkbench().getSharedImages();
    setUpAction(
        cut, cut, shared.getImageDescriptor(IMG_TOOL_CUT), shared.getImageDescriptor(IMG_TOOL_CUT),
        shared.getImageDescriptor(IMG_TOOL_CUT_DISABLED), true);
  }

  @Override public void run() {
    ITerminalViewControl target = getTarget();
    if (target != null) {
      target.sendKey('\u0018');
    }
  }

  @Override public void updateAction(boolean aboutToShow) {
    // Cut is always disabled
    setEnabled(false);
  }
}
