/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.control;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;

/**
 * Interface to create a command input control.
 */
public interface ICommandInputField {
  void createControl(Composite parent, ITerminalViewControl terminal);

  void dispose();

  /**
   * Sets the font of a control created with {@link #createControl(Composite, ITerminalViewControl)}.
   * @param font the new text font.
   */
  void setFont(Font font);
}