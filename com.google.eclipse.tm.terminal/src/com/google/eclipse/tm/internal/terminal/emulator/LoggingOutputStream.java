/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.emulator;

import java.io.*;

import com.google.eclipse.tm.internal.terminal.provisional.api.Logger;

public class LoggingOutputStream extends FilterOutputStream {
  public LoggingOutputStream(OutputStream out) {
    super(out);
  }

  @Override public void write(byte[] b, int off, int len) throws IOException {
    if (Logger.isLogEnabled()) {
      Logger.log("Received " + len + " bytes: '" + Logger.encode(new String(b, 0, len)) + "'");
    }
    if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
      throw new IndexOutOfBoundsException();
    }
    for (int i = 0; i < len; i++) {
      super.write(b[off + i]);
    }
  }

  @Override public void write(int b) throws IOException {
    if (Logger.isLogEnabled()) {
      Logger.log("Received " + 1 + " bytes: '" + Logger.encode(new String(new byte[] { (byte) b }, 0, 1)) + "'");
    }
    super.write(b);
  }
}
