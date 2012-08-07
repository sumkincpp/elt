/*******************************************************************************
 * Copyright (c) 2006, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.provisional.api;

/**
 * Represent the sate of a terminal connection.
 *
 * @author Michael Scharf
 */
public enum TerminalState {
  /**
   * The terminal is not connected.
   */
  CLOSED("CLOSED"), CONNECTING("CONNECTING..."), CONNECTED("CONNECTED");

  private final String state;

  private TerminalState(String state) {
    this.state = state;
  }

  @Override public String toString() {
    return state;
  }
}
