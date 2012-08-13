/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.connector;

import java.io.*;

class TerminalToRemoteInjectionOutputStream extends FilterOutputStream {
  private static class BufferInterceptor {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private OutputStream original;

    void begin(OutputStream original) {
      this.original = original;
    }

    void close() throws IOException {
      original.write(buffer.toByteArray());
    }

    void write(byte[] b, int off, int len) {
      buffer.write(b, off, len);
    }

    void write(int b) {
      buffer.write(b);
    }

    void flush() {}
  }

  private class TerminalFilterOutputStream extends OutputStream {
    final private Object lock = new Object();

    @Override public void close() throws IOException {
      synchronized (lock) {
        if (injection == this) {
          flush();
          ungrabOutput();
        }
      }
    }

    @Override public void write(byte[] b, int off, int len) throws IOException {
      synchronized (lock) {
        checkStream();
        out.write(b, off, len);
      }
    }

    @Override public void write(byte[] b) throws IOException {
      synchronized (lock) {
        checkStream();
        out.write(b);
      }
    }

    @Override public void flush() throws IOException {
      synchronized (lock) {
        checkStream();
        out.flush();
      }
    }

    @Override public void write(int b) throws IOException {
      synchronized (lock) {
        checkStream();
        out.write(b);
      }
    }

    private void checkStream() throws IOException {
      if (injection != this)
       {
        throw new IOException("Stream is closed");
      }
    }
  }

  private BufferInterceptor interceptor;
  private TerminalFilterOutputStream injection;

  public TerminalToRemoteInjectionOutputStream(OutputStream out) {
    super(out);
  }

  synchronized protected void ungrabOutput() throws IOException {
    if (interceptor != null) {
      interceptor.close();
      interceptor = null;
    }
    injection = null;
  }

  /**
   * There can only be one injection stream active at a time. You must call close on the returned output stream to end
   * the injection.
   * @param interceptor This is used handle bytes sent while the injection stream is active.
   * @return a output stream that can be used to write to the decorated stream.
   * @throws IOException if something goes wrong.
   */
  public synchronized OutputStream grabOutput(BufferInterceptor interceptor) throws IOException {
    if (injection != null) {
      throw new IOException("Buffer in use");
    }
    this.interceptor = interceptor;
    this.interceptor.begin(out);
    injection = new TerminalFilterOutputStream();
    return injection;
  }

  public synchronized OutputStream grabOutput() throws IOException {
    return grabOutput(new BufferInterceptor());
  }

  @Override synchronized public void close() throws IOException {
    if (injection != null) {
      injection.close();
    }
    super.close();
  }

  @Override synchronized public void flush() throws IOException {
    if (interceptor != null) {
      interceptor.flush();
    }
    out.flush();
  }

  @Override synchronized public void write(byte[] b, int off, int len) throws IOException {
    if (interceptor != null) {
      interceptor.write(b, off, len);
    } else {
      out.write(b, off, len);
    }
  }

  @Override synchronized public void write(int b) throws IOException {
    if (interceptor != null) {
      interceptor.write(b);
    } else {
      out.write(b);
    }
  }
}
