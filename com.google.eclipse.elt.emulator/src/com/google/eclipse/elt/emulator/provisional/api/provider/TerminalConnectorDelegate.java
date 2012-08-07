/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - [225853][api] Provide more default functionality in TerminalConnectorImpl
 *******************************************************************************/
package com.google.eclipse.elt.emulator.provisional.api.provider;

import static com.google.eclipse.elt.emulator.provisional.api.TerminalState.CLOSED;

import java.io.OutputStream;

import com.google.eclipse.elt.emulator.provisional.api.ITerminalControl;

/**
 * Abstract base class for all terminal connector implementations.
 *
 * @since org.eclipse.tm.terminal 2.0
 */
public abstract class TerminalConnectorDelegate {
  // The TerminalControl associated with this connector. Required for advertising state changes when needed.
  protected ITerminalControl terminalControl;

  /**
   * Initialize this connector. This is called once after the constructor, in order to perform any required
   * initializations such as loading required native libraries. Any work that may lead to runtime exceptions should be
   * done in this method rather than in the constructor.
   *
   * @throws Exception when the connector fails to initialize (due to missing required libraries, for instance).
   */
  public void initialize() throws Exception {}

  public final void connect(ITerminalControl control) {
    this.terminalControl = control;
    connect();
  }

  protected abstract void connect();

  public final void disconnect() {
    onDisconnect();
    terminalControl.setState(CLOSED);
  }

  protected void onDisconnect() {}

  /**
   * Returns the terminal-to-remote stream (bytes written to this stream will be sent to the remote site). For the
   * stream in the other direction (remote to terminal see {@link ITerminalControl#getRemoteToTerminalOutputStream()}.
   *
   * @return the terminal-to-remote stream.
   */
  public abstract OutputStream getTerminalToRemoteStream();

  /**
   * @return A string that represents the settings of the connection. This representation may be shown in the status
   * line of the terminal view.
   */
  public abstract String getSettingsSummary();

  /**
   * Tests if local echo is needed. The default implementation returns {@code false}.
   *
   * @return {@code false} by default.
   */
  public boolean isLocalEcho() {
    return false;
  }

  /**
   * Notifies the remote site that the size of the terminal has changed.
   *
   * @param width the new width in characters.
   * @param height the new height in characters.
   */
  public void setTerminalSize(int width, int height) {}
}
