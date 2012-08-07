/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.connector;

import java.io.*;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;

import com.google.eclipse.elt.emulator.provisional.api.ITerminalControl;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class TerminalOutputListener implements IStreamListener {
  private final PrintStream printStream;

  TerminalOutputListener(ITerminalControl control, String encoding) throws UnsupportedEncodingException {
    printStream = new PrintStream(control.getRemoteToTerminalOutputStream(), true, encoding);
  }

  @Override public void streamAppended(String text, IStreamMonitor monitor) {
    if (text.contains("\u001b[1A\u001b[K")) {
      // clean = text.replace("\u001b[1A\u001b[K", "\u001b[K");
      // TODO(alruiz) figure out why 1+ lines deleted in blaze build.
    }
    printStream.print(text);
  }
}
