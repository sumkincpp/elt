/*******************************************************************************
 * Copyright (c) 2006, 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.control;

import com.google.eclipse.tm.internal.terminal.provisional.api.TerminalState;

/**
 * Provided by a view implementation.
 */
public interface ITerminalListener {
  /**
   * Called when the state of the connection has changed.
   * @param state the state of the connection.
   */
  void setState(TerminalState state);

  /**
   * Set the title of the terminal.
   * @param title the new title.
   */
  void setTerminalTitle(String title);
}
