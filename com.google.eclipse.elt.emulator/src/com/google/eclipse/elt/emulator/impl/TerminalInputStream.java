/*******************************************************************************
 * Copyright (c) 1996, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Michael Scharf (Wind River)
 * Douglas Lea (Addison Wesley) - [cq:1552] BoundedBufferWithStateTracking adapted to BoundedByteBuffer
 *******************************************************************************/

package com.google.eclipse.elt.emulator.impl;

import java.io.*;
import java.util.*;

import org.eclipse.swt.widgets.Display;

import com.google.eclipse.elt.emulator.util.BoundedByteBuffer;

/**
 * The main purpose of this class is to start a {@code Runnable} in the display thread when data is available and to
 * pretend no data is available after a given amount of time the {@code Runnable} is running.
 */
public class TerminalInputStream extends InputStream {
  // The maximum time in milliseconds the 'notifyChange' runs until 'ready()' returns false.
  private final int uiTimeout;

  // The output stream used by the terminal backend to write to the terminal.
  protected final OutputStream outputStream;

  // This runnable is called every time some characters are available from.
  private final Runnable notifyChange;

  // A shared timer for all terminals. This times is used to limit the time used in the display thread.
  static Timer timer = new Timer(false);

  // A blocking byte queue.
  private final BoundedByteBuffer queue;

  // The maximum amount of data read and written in one shot. The timer cannot interrupt reading this amount of data.
  // This is used as optimization, because reading single characters can be very inefficient, because each call is
  // synchronized.
  // Block size must be smaller than the Queue capacity!
  final int BLOCK_SIZE = 64;

  // The runnable that is scheduled in the display tread. Takes care of the timeout management. It calls the
  // 'notifyChange'.
  // Synchronized with 'queue'.
  private Runnable runnable;

  // Used as flag to indicate that the current runnable has used enough time in the display thread. This variable is set
  // by a timer thread after the Runnable starts to run in the Display thread after 'uiTimeout'.
  // Synchronized with 'queue'.
  private boolean enoughDisplayTime;

  /**
   * Constructor.
   *
   * @param bufferSize the size of the buffer of the output stream
   * @param uiTimeout the maximum time the {@code notifyChange} {@code Runnable} runs. It will be rescheduled after
   *         {@code uiTimeout} if input data is still available.
   * @param notifyChange a {@code Runnable} that is posted to the display thread via {@link Display#asyncExec}. The
   *         {@code Runnable} is posted several times!
   */
  public TerminalInputStream(int bufferSize, int uiTimeout, Runnable notifyChange) {
    outputStream = new TerminalOutputStream();
    this.notifyChange = notifyChange;
    int capacity = bufferSize;
    if (capacity < BLOCK_SIZE) {
      capacity = 2 * BLOCK_SIZE;
    }
    queue = new BoundedByteBuffer(capacity);
    this.uiTimeout = uiTimeout;
  }

  /**
   * Posts the {@code Runnable} {@link #notifyChange} to the display thread, unless the {@code Runnable} is already
   * scheduled. It will make {@link #ready} return {@code false} after {@link #uiTimeout} milliseconds.
   */
  void bytesAreAvailable() {
    // synchronize on the queue to reduce the locks
    synchronized (queue) {
      if (runnable == null) {
        runnable = new Runnable() {
          @Override public void run() {
            synchronized (queue) {
              runnable = null;
            }
            startTimer(uiTimeout);
            notifyChange.run();
          }
        };
        // TODO: Make sure we don't create a display if the display is disposed.
        Display.getDefault().asyncExec(runnable);
      }
    }
  }

  /**
   * Starts a timer that sets {@link #enoughDisplayTime} to {@code true} after the given milliseconds.
   *
   * @param milliseconds the time after which {@code enoughDisplayTime} is set to {@code true}.
   */
  void startTimer(int milliseconds) {
    synchronized (queue) {
      enoughDisplayTime = false;
    }
    timer.schedule(new TimerTask() {
      @Override public void run() {
        synchronized (queue) {
          enoughDisplayTime = true;
          // there is some data available
          if (queue.size() > 0) {
            // schedule a new runnable to do the work
            bytesAreAvailable();
          }
        }
      }
    }, milliseconds);
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
   * Indicate whether a character is available for the terminal to show. Must be called in the display thread.
   * @return {@code true} if a character is available for the terminal to show, {@code false} otherwise.
   */
  @Override public int available() {
    int available;
    synchronized (queue) {
      if (enoughDisplayTime) {
        return 0;
      }
      available = queue.size();
    }
    // Limit the available amount of data. Otherwise our trick of limiting the time spend reading might not work.
    if (available > BLOCK_SIZE) {
      available = BLOCK_SIZE;
    }
    return available;
  }

  /**
   * Returns the next available byte. Checks with {@link #available} if characters are available.
   *
   * @return the next available byte.
   */
  @Override public int read() throws IOException {
    try {
      return queue.read();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return -1;
    }
  }

  /**
   * Closing a {@code ByteArrayInputStream} has no effect. The methods in this class can be called after the stream has
   * been closed without generating an {@code IOException}.
   */
  @Override public void close() {}

  @Override public int read(byte[] b, int off, int len) throws IOException {
    int n = 0;
    // Read as much as we can using a single synchronized statement.
    synchronized (queue) {
      try {
        // Make sure that not more than BLOCK_SIZE is read in one call.
        while (queue.size() > 0 && n < len && n < BLOCK_SIZE) {
          b[off + n] = queue.read();
          n++;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return n;
  }

  /**
   * An output stream that calls {@link TerminalInputStream#textAvailable} every time data is written to the stream. The
   * data is written to {@link TerminalInputStream#queue}.
   *
   */
  class TerminalOutputStream extends OutputStream {
    @Override public void write(byte[] b, int off, int len) throws IOException {
      try {
        // optimization to avoid many synchronized sections: put the data in junks into the queue.
        int newOff = off;
        int end = off + len;
        while (newOff < end) {
          int n = newOff + BLOCK_SIZE;
          if (n > end) {
            n = end;
          }
          // now block the queue for the time we need to
          // add some characters
          synchronized (queue) {
            for (int i = newOff; i < n; i++) {
              queue.write(b[i]);
            }
            bytesAreAvailable();
          }
          newOff += BLOCK_SIZE;
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    @Override public void write(int b) throws IOException {
      try {
        // A kind of optimization, because both calls use the queue lock.
        synchronized (queue) {
          queue.write((byte) b);
          bytesAreAvailable();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
