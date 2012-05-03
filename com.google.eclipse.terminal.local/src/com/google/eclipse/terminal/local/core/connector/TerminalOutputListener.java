/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.core.connector;

import java.io.*;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalControl;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class TerminalOutputListener implements IStreamListener {
  private final PrintStream printStream;

  TerminalOutputListener(ITerminalControl control) throws UnsupportedEncodingException {
    printStream = new PrintStream(control.getRemoteToTerminalOutputStream(), true, LocalTerminalConnector.ENCODING);
  }

  @Override public void streamAppended(String text, IStreamMonitor monitor) {
    printStream.print(text);
  }
}
