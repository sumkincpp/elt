/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.command;

import static com.google.eclipse.elt.view.ui.TerminalView.openTerminalView;

import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class OpenTerminalCommand extends AbstractHandler {

  @Override public Object execute(ExecutionEvent event) {
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    IResource target = resourceFrom(selection);
    if (target != null) {
      if (!(target instanceof IContainer)) {
        target = target.getParent();
      }
      openTerminalView(target.getLocation());
    }
    return null;
  }

  private IResource resourceFrom(ISelection selection) {
    if (!(selection instanceof IStructuredSelection)) {
      return null;
    }
    Object o = ((IStructuredSelection) selection).getFirstElement();
    if (!(o instanceof IAdaptable)) {
      return null;
    }
    return (IResource) ((IAdaptable) o).getAdapter(IResource.class);
  }
}
