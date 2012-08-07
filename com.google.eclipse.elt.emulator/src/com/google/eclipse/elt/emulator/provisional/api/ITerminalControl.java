/*******************************************************************************
 * Copyright (c) 2006, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.provisional.api;

import java.io.*;

import org.eclipse.swt.widgets.*;

/**
 * Represents the terminal view as seen by a terminal connection.
 *
 * @author Michael Scharf
 */
public interface ITerminalControl {
  TerminalState getState();

  void setState(TerminalState state);

  void setUpTerminal(Composite parent);

  Shell getShell();

  /**
   * Set the encoding that the terminal uses to decode bytes from the Terminal-to-remote-Stream into Unicode characters
   * used in Java; or, to encode characters typed by the user into bytes sent over the wire to the remote.
   * <p>
   * By default, the local platform default encoding is used. Also note that the encoding must not be applied in case
   * the terminal stream is processed by some data transfer protocol which requires binary data.
   * </p>
   * <p>
   * Validity of the encoding set here is not checked. Since some encodings do not cover the entire range of Unicode
   * characters, it can happen that a particular Unicode text typed in by the user can not be encoded into a byte stream
   * with the encoding specified. and {@link UnsupportedEncodingException} will be thrown in this case at the time the
   * text is about to be processed.
   * </p>
   * <p>
   * The concrete encoding to use can either be specified manually by a user, by means of a dialog, or a connector can
   * try to obtain it automatically from the remote side (e.g. by evaluating an environment variable such as LANG on
   * UNIX systems.)
   * </p>
   *
   * @param encoding the new encoding.
   * @throws UnsupportedEncodingException if the given encoding is not supported.
   */
  void setEncoding(String encoding) throws UnsupportedEncodingException;

  String getEncoding();

  /**
   * Show a text in the terminal. If puts newlines at the beginning and the end.
   *
   * @param text the text to display.
   */
  void displayTextInTerminal(String text);

  /**
   * Returns the stream used to write to the terminal. Any bytes written to this stream appear in the terminal or are
   * interpreted by the emulator as control sequences. The stream in the opposite direction, terminal to remote is in
   * {@link ITerminalConnector#getTerminalToRemoteStream()}.
   *
   * @return the stream used to write to the terminal.
   */
  OutputStream getRemoteToTerminalOutputStream();

  void setTerminalTitle(String title);

  /**
   * Show an error message during connect.
   * @param errorMessage the new error message.
   */
  // TODO(Michael Scharf): Should be replaced by a better error notification mechanism!
  void setErrorMessage(String errorMessage);
}
