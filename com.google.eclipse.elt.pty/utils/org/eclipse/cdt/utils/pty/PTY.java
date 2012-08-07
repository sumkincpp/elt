/*******************************************************************************
 * Copyright (c) 2002, 2010 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.utils.pty;

import java.io.IOException;

import com.google.eclipse.elt.pty.PtyPlugin;

/**
 * PTY - pseudo terminal support.
 */
public class PTY {
  static final String LIBRARY_NAME = "gpty";

  final boolean console;
  String slave;
  PTYInputStream in;
  PTYOutputStream out;
  /*
   * NOTE: Field is accessed by the native layer. Do not refactor!
   */
  int master;

  private static boolean hasPTY;
  private static boolean setTerminalSizeErrorAlreadyLogged;

  /**
   * The master fd is used on two streams. We need to wrap the fd so that when stream.close() is called the other stream
   * is disabled.
   */
  public class MasterFD {
    public int getFD() {
      return master;
    }

    void setFD(int fd) {
      master = fd;
    }
  }

  /**
   * Create PTY for use with Eclipse console. Identical to {@link PTY#PTY(boolean) PTY(true)}.
   * @throws IOException if something goes wrong.
   */
  public PTY() throws IOException {
    this(true);
  }

  /**
   * Create pseudo-terminal.
   *
   * <p>
   * The provided flag indicates whether the pseudo terminal is used with the interactive Eclipse console:
   * <ul>
   * <li>If {@code true} the terminal is configured with no echo and stderr is redirected to a pipe instead of the
   * PTY.</li>
   * <li>If {@code false} the terminal is configured with echo and stderr is connected to the PTY. This mode is
   * best suited for use with a proper terminal emulation. Note that this mode might not be supported on all platforms.
   * Known platforms which support this mode are: <code>linux-x86</code>, <code>linux-x86_64</code>,
   * <code>solaris-sparc</code>, <code>macosx</code>.</li>
   * </ul>
   * </p>
   *
   * @param console whether terminal is used with Eclipse console
   * @throws IOException if the PTY could not be created
   * @since 5.2
   */
  public PTY(boolean console) throws IOException {
    this.console = console;

    if (hasPTY) {
      slave = openMaster(console);
    }

    if (slave == null) {
      throw new IOException(PtyPlugin.getResourceString("Util.exception.cannotCreatePty"));
    }

    in = new PTYInputStream(new MasterFD());
    out = new PTYOutputStream(new MasterFD());
  }

  public String getSlaveName() {
    return slave;
  }

  public MasterFD getMasterFD() {
    return new MasterFD();
  }

  /**
   * @return whether this pseudo terminal is for use with the Eclipse console.
   */
  public final boolean isConsole() {
    return console;
  }

  public PTYOutputStream getOutputStream() {
    return out;
  }

  public PTYInputStream getInputStream() {
    return in;
  }

  /**
   * Change terminal window size to given width and height.
   * <p>
   * This should only be used when the pseudo terminal is configured for use with a terminal emulation, i.e. when
   * {@link #isConsole()} returns {@code false}.
   * </p>
   * <p>
   * <strong>Note:</strong> This method may not be supported on all platforms. Known platforms which support this method
   * are: {@code linux-x86}, {@code linux-x86_64}, {@code solaris-sparc}, {@code macosx}.
   * </p>
   * @param width the given width.
   * @param height the given height.
   */
  public final void setTerminalSize(int width, int height) {
    try {
      change_window_size(master, width, height);
    } catch (UnsatisfiedLinkError e) {
      if (!setTerminalSizeErrorAlreadyLogged) {
        setTerminalSizeErrorAlreadyLogged = true;
        PtyPlugin.log(PtyPlugin.getResourceString("Util.exception.cannotSetTerminalSize"), e);
      }
    }
  }

  public static boolean isSupported() {
    return hasPTY;
  }

  native String openMaster(boolean console);

  native int change_window_size(int fdm, int width, int height);

  static {
    System.loadLibrary(LIBRARY_NAME);
    hasPTY = true;
  }
}
