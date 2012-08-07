/*******************************************************************************
 * Copyright (c) 2004, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.actions;

import static com.google.eclipse.elt.emulator.actions.ActionMessages.selectAll;

import org.eclipse.jface.resource.ImageDescriptor;

import com.google.eclipse.elt.emulator.control.ITerminalViewControl;

public class TerminalActionSelectAll extends AbstractTerminalAction {
  public TerminalActionSelectAll() {
    super(TerminalActionSelectAll.class.getName());
    setUpAction();
  }

  public TerminalActionSelectAll(ITerminalViewControl target) {
    super(target, TerminalActionSelectAll.class.getName());
    setUpAction();
  }

  private void setUpAction() {
    setUpAction(selectAll, selectAll, (ImageDescriptor) null, null, null, false);
  }

  @Override public void run() {
    ITerminalViewControl target = getTarget();
    if (target != null) {
      target.selectAll();
    }
  }

  @Override public void updateAction(boolean aboutToShow) {
    ITerminalViewControl target = getTarget();
    setEnabled(target != null && !target.isEmpty());
  }
}
