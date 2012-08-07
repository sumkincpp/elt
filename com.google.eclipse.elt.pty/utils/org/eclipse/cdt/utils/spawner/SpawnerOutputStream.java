/*******************************************************************************
 * Copyright (c) 2000, 2011 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.utils.spawner;

import java.io.*;

public class SpawnerOutputStream extends OutputStream {
  private int fd;

  /**
   * From a Unix valid file descriptor set a Reader.
   * @param fd file descriptor.
   */
  public SpawnerOutputStream(int fd) {
    this.fd = fd;
  }

  @Override public void write(byte[] b, int off, int len) throws IOException {
    if (b == null) {
      throw new NullPointerException();
    }
    if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
      throw new IndexOutOfBoundsException();
    }
    if (len == 0) {
      return;
    }
    byte[] tmpBuf = new byte[len];
    System.arraycopy(b, off, tmpBuf, off, len);
    write0(fd, tmpBuf, len);
  }

  @Override public void write(int b) throws IOException {
    byte[] buf = new byte[1];
    buf[0] = (byte) b;
    write(buf, 0, 1);
  }

  @Override public void close() throws IOException {
    if (fd == -1) {
      return;
    }
    int status = close0(fd);
    if (status == -1) {
      throw new IOException("close error");
    }
    fd = -1;
  }

  @Override protected void finalize() throws IOException {
    close();
  }

  private native int write0(int fd, byte[] b, int len) throws IOException;

  private native int close0(int fd);

  static {
    System.loadLibrary(Spawner.LIBRARY_NAME);
  }
}
