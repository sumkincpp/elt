/*******************************************************************************
 * Copyright (c) 2004, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.control.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.*;

import com.google.eclipse.tm.internal.terminal.control.ITerminalViewControl;
import com.google.eclipse.tm.internal.terminal.control.impl.TerminalPlugin;

public abstract class AbstractTerminalAction extends Action {
  private final ITerminalViewControl target;

  public AbstractTerminalAction(String id) {
    this(null, id, 0);
  }

  public AbstractTerminalAction(ITerminalViewControl target, String id) {
    this(target, id, 0);
  }

  public AbstractTerminalAction(ITerminalViewControl target, String id, int style) {
    super("", style);
    this.target = target;
    setId(id);
  }

  @Override public abstract void run();

  protected void setUpAction(
      String text, String toolTip, String image, String enabledImage, String disabledImage, boolean enabled) {
    setUpAction(
        text, toolTip, image, enabledImage, disabledImage, enabled, TerminalPlugin.getDefault().getImageRegistry());
  }

  protected void setUpAction(
      String text, String toolTip, String hoverImage, String enabledImage, String disabledImage, boolean enabled,
      ImageRegistry imageRegistry) {
    setUpAction(
        text, toolTip, imageRegistry.getDescriptor(hoverImage), imageRegistry.getDescriptor(enabledImage),
        imageRegistry.getDescriptor(disabledImage), enabled);
  }

  protected void setUpAction(
      String text, String toolTip, ImageDescriptor hoverImage, ImageDescriptor enabledImage,
      ImageDescriptor disabledImage, boolean enabled) {
    setText(text);
    setToolTipText(toolTip);
    setEnabled(enabled);
    if (enabledImage != null) {
      setImageDescriptor(enabledImage);
    }
    if (disabledImage != null) {
      setDisabledImageDescriptor(disabledImage);
    }
    if (hoverImage != null) {
      setHoverImageDescriptor(hoverImage);
    }
  }

  /**
   * Returns the terminal instance on which the action should operate.
   *
   * @return the terminal instance on which the action should operate.
   */
  protected ITerminalViewControl getTarget() {
    return target;
  }

  /**
   * Subclasses can update their action.
   *
   * @param aboutToShow {@code true} before the menu is shown, {@code false} when the menu gets hidden.
   */
  public void updateAction(boolean aboutToShow) {}
}
