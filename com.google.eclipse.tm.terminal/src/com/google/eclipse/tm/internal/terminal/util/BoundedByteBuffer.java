package com.google.eclipse.tm.internal.terminal.util;

import org.eclipse.core.runtime.Assert;

/**
 * A byte bounded buffer used to synchronize the input and the output stream.
 * <p>
 * Adapted from {@code BoundedBufferWithStateTracking} http://gee.cs.oswego.edu/dl/cpj/allcode.java
 * http://gee.cs.oswego.edu/dl/cpj/
 * <p>
 * BoundedBufferWithStateTracking is part of the examples for the book Concurrent Programming in Java: Design
 * Principles and Patterns by Doug Lea (ISBN 0-201-31009-0). Second edition published by Addison-Wesley, November
 * 1999. The code is Copyright(c) Douglas Lea 1996, 1999 and released to the public domain and may be used for any
 * purposes whatsoever.
 * <p>
 * For some reasons a solution based on PipedOutputStream/PipedIntputStream does work *very* slowly:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4404700
 * <p>
 */
public class BoundedByteBuffer {
  private final byte[] buffer; // the elements
  private int putPosition; // circular indices
  private int takePosition;
  private int usedSlots; // the count
  private boolean closed;

  public BoundedByteBuffer(int capacity) throws IllegalArgumentException {
    // Make sure we don't deadlock on too small capacity.
    if (capacity <= 0) {
      throw new IllegalArgumentException("Capacity should be greater than zero");
    }
    buffer = new byte[capacity];
  }

  /**
   * Returns the bytes available for {@link #read()}.
   *
   * @return the bytes available for reading.
   */
  public int size() {
    return usedSlots;
  }

  /**
   * Writes a single byte to the buffer. Blocks if the buffer is full.
   *
   * @param b byte to write to the buffer.
   * @throws InterruptedException when the thread is interrupted while waiting for the buffer to become ready.
   */
  public void write(byte b) throws InterruptedException {
    while (usedSlots == buffer.length) {
      // Wait until not full.
      wait();
    }
    buffer[putPosition] = b;
    putPosition = (putPosition + 1) % buffer.length; // cyclically increment
    if (usedSlots++ == 0) {
      notifyAll();
    }
  }

  public int getFreeSlots() {
    return buffer.length - usedSlots;
  }

  public void write(byte[] b, int off, int len) throws InterruptedException {
    Assert.isTrue(len <= getFreeSlots());
    while (usedSlots == buffer.length) {
      // Wait until not full.
      wait();
    }
    int n = Math.min(len, buffer.length - putPosition);
    System.arraycopy(b, off, buffer, putPosition, n);
    if (putPosition + len > buffer.length) {
      System.arraycopy(b, off + n, buffer, 0, len - n);
    }
    putPosition = (putPosition + len) % buffer.length; // cyclically increment
    boolean wasEmpty = usedSlots == 0;
    usedSlots += len;
    if (wasEmpty) {
      notifyAll();
    }
  }

  /**
   * Read a single byte. Blocks until a byte is available.
   *
   * @return a byte from the buffer.
   * @throws InterruptedException when the thread is interrupted while waiting for the buffer to become ready.
   */
  public byte read() throws InterruptedException {
    while (usedSlots == 0) {
      if (closed) {
        return -1;
      }
      // Wait until not empty.
      wait();
    }
    byte b = buffer[takePosition];
    takePosition = (takePosition + 1) % buffer.length;
    if (usedSlots-- == buffer.length) {
      notifyAll();
    }
    return b;
  }

  public int read(byte[] b, int off, int len) throws InterruptedException {
    Assert.isTrue(len <= size());
    while (usedSlots == 0) {
      if (closed) {
        return 0;
      }
      // Wait until not empty.
      wait();
    }
    int n = Math.min(len, buffer.length - takePosition);
    System.arraycopy(buffer, takePosition, b, off, n);
    if (takePosition + len > n) {
      System.arraycopy(buffer, 0, b, off + n, len - n);
    }
    takePosition = (takePosition + len) % buffer.length;
    boolean wasFull = usedSlots == buffer.length;
    usedSlots -= len;
    if (wasFull) {
      notifyAll();
    }
    return len;
  }

  public void close() {
    closed = true;
    notifyAll();
  }

  public boolean isClosed() {
    return closed;
  }
}
