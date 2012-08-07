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

import org.eclipse.debug.core.model.IStreamsProxy;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class TerminalOutputStream extends OutputStream {
  private final IStreamsProxy streamsProxy;
  private final String encoding;

  TerminalOutputStream(IStreamsProxy streamsProxy, String encoding) {
    this.streamsProxy = streamsProxy;
    this.encoding = encoding;
  }

  @Override
  public void write(int b) throws IOException {
    streamsProxy.write(new String(new byte[] { (byte) (b & 0xFF) }, encoding));
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    String input = new String(b, off, len, encoding);
    streamsProxy.write(input);
  }
}
