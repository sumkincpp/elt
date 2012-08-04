/*******************************************************************************
 * Copyright (c) 1996, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.textcanvas;

import java.io.*;

import com.google.eclipse.tm.internal.terminal.util.BoundedByteBuffer;

/**
 * Starts a {@code Runnable} in the display thread when data is available and to pretend no data is available after a
 * given amount of time the {@code Runnable} is running.
 */
public class PipedInputStream extends InputStream {
  // The output stream used by the terminal back-end to write to the terminal
  protected final OutputStream outputStream;

  // A blocking byte queue.
  private final BoundedByteBuffer queue;

  /**
   * Constructor.
   *
   * @param bufferSize the size of the buffer of the output stream.
   */
  public PipedInputStream(int bufferSize) {
    outputStream = new PipedOutputStream();
    queue = new BoundedByteBuffer(bufferSize);
  }

  /**
   * Returns the output stream used by the back-end to write to the terminal.
   *
   * @return the output stream used by the back-end to write to the terminal.
   */
  public OutputStream getOutputStream() {
    return outputStream;
  }

  /**
   * Waits until data is available for reading.
   *
   * @param time the time wait, in milliseconds.
   * @throws InterruptedException when the thread is interrupted while waiting for the buffer to become ready.
   */
  public void waitForAvailable(long time) throws InterruptedException {
    synchronized (queue) {
      if (queue.size() == 0 && !queue.isClosed()) {
        queue.wait(time);
      }
    }
  }

  @Override public int available() {
    synchronized (queue) {
      return queue.size();
    }
  }

  @Override public int read() throws IOException {
    try {
      synchronized (queue) {
        return queue.read();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return -1;
    }
  }

  @Override public void close() throws IOException {
    synchronized (queue) {
      queue.close();
    }
  }

  @Override public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
    int n = 0;
    // Read as much as we can using a single synchronized statement.
    try {
      synchronized (queue) {
        // If nothing available, block and read one byte.
        if (queue.size() == 0) {
          // Block now until at least one byte is available.
          int c = queue.read();
          // Are we at the end of stream?
          if (c == -1) {
            return -1;
          }
          b[off] = (byte) c;
          n++;
        }
        // Is there more data available?
        if (n < len && queue.size() > 0) {
          // Read at most available.
          int available = Math.min(queue.size(), len - n);
          // Are we at the end of the stream?
          if (available == 0 && queue.isClosed()) {
            // If no byte was read, return -1 to indicate end of stream; otherwise return the bytes we read up to now.
            if (n == 0) {
              n = -1;
            }
            return n;
          }
          queue.read(b, off + n, available);
          n += available;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return n;
  }

  /**
   * An output stream that calls {@link PipedInputStream#textAvailable} every time data is written to the stream. The
   * data is written to {@link PipedInputStream#queue}.
   */
  private class PipedOutputStream extends OutputStream {
    @Override public void write(byte[] b, int off, int len) throws IOException {
      try {
        synchronized (queue) {
          if (queue.isClosed()) {
            throw new IOException("Stream is closed!");
          }
          int written = 0;
          while (written < len) {
            if (queue.getFreeSlots() == 0) {
              // If no slots available, write one byte and block until free slots are available.
              queue.write(b[off + written]);
              written++;
            } else {
              // If slots are available, write as much as we can in one junk
              int n = Math.min(queue.getFreeSlots(), len - written);
              queue.write(b, off + written, n);
              written += n;
            }
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override public void write(int b) throws IOException {
      try {
        synchronized (queue) {
          if (queue.isClosed()) {
            throw new IOException("Stream is closed!");
          }
          queue.write((byte) b);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override public void close() throws IOException {
      synchronized (queue) {
        queue.close();
      }
    }
  }
}
