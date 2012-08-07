/*******************************************************************************
 * Copyright (c) 2005, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.provisional.api;

import static org.eclipse.core.runtime.IStatus.*;

import java.io.*;

import org.eclipse.core.runtime.Status;

import com.google.eclipse.elt.emulator.impl.TerminalPlugin;

/**
 * A simple logger class. Every method in this class is static, so they can be called from both class and instance
 * methods. To use this class, write code like this:
 * <p>
 *
 * <pre>
 * Logger.log(&quot;something has happened&quot;);
 * Logger.log(&quot;counter is &quot; + counter);
 * </pre>
 *
 * @author Fran Litterio <francis.litterio@windriver.com>
 */
public final class Logger {
  public static final String TRACE_DEBUG_LOG = "org.eclipse.tm.terminal/debug/log";
  public static final String TRACE_DEBUG_LOG_ERROR = "org.eclipse.tm.terminal/debug/log/error";
  public static final String TRACE_DEBUG_LOG_INFO = "org.eclipse.tm.terminal/debug/log/info";
  public static final String TRACE_DEBUG_LOG_CHAR = "org.eclipse.tm.terminal/debug/log/char";
  public static final String TRACE_DEBUG_LOG_BUFFER_SIZE = "org.eclipse.tm.terminal/debug/log/buffer/size";

  private static PrintStream logStream;

  static {
    String logFile = logFile();
    if (logFile != null) {
      try {
        logStream = new PrintStream(new FileOutputStream(logFile, true));
      } catch (Exception ex) {
        logStream = System.err;
        logStream.println("Exception when opening log file -- logging to stderr!");
        ex.printStackTrace(logStream);
      }
    }
  }

  private static String logFile() {
    File directory = new File("C:\\eclipselogs");
    if (directory.isDirectory()) {
      return directory + "\\tmterminal.log";
    }
    directory = new File("/tmp/eclipselogs");
    if (directory.isDirectory()) {
      return directory + "/tmterminal.log";
    }
    return null;
  }

  /**
   * Encodes text such that non-printable control characters are converted into user-readable escape sequences for
   * logging.
   * @param message the text to encode
   * @return encoded the encoded text;
   */
  public static final String encode(String message) {
    boolean encoded = false;
    StringBuilder buffer = new StringBuilder(message.length() + 32);
    for (int i = 0; i < message.length(); i++) {
      char c = message.charAt(i);
      switch (c) {
      case '\\':
      case '\'':
        buffer.append('\\').append(c);
        encoded = true;
        break;
      case '\r':
        buffer.append('\\').append('r');
        encoded = true;
        break;
      case '\n':
        buffer.append('\\').append('n');
        encoded = true;
        break;
      case '\t':
        buffer.append('\\').append('t');
        encoded = true;
        break;
      case '\f':
        buffer.append('\\').append('f');
        encoded = true;
        break;
      case '\b':
        buffer.append('\\').append('b');
        encoded = true;
        break;
      default:
        if (c <= '\u000f') {
          buffer.append('\\').append('x').append('0').append(Integer.toHexString(c));
          encoded = true;
        } else if (c >= ' ' && c < '\u007f') {
          buffer.append(c);
        } else if (c <= '\u00ff') {
          buffer.append('\\').append('x').append(Integer.toHexString(c));
          encoded = true;
        } else {
          buffer.append('\\').append('u');
          if (c <= '\u0fff') {
            buffer.append('0');
          }
          buffer.append(Integer.toHexString(c));
          encoded = true;
        }
      }
    }
    if (encoded) {
      return buffer.toString();
    }
    return message;
  }

  public static final boolean isLogEnabled() {
    return (logStream != null);
  }

  public static final void log(String message) {
    if (logStream != null) {
      StackTraceElement caller = Thread.currentThread().getStackTrace()[1];
      String className = caller.getClassName();
      className = className.substring(className.lastIndexOf('.') + 1);
      logStream.println(className + "." + caller.getMethodName() + ":" + caller.getLineNumber() + ": " + message);
      logStream.flush();
    }
  }

  public static final void logException(Exception e) {
    if (TerminalPlugin.getDefault() != null) {
      TerminalPlugin.getDefault().getLog().log(new Status(ERROR, TerminalPlugin.PLUGIN_ID, OK, e.getMessage(), e));
    } else {
      e.printStackTrace();
    }
  }
}
