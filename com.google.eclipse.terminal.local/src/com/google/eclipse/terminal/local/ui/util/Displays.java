/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.util;

import static java.lang.Thread.currentThread;
import static org.eclipse.swt.widgets.Display.findDisplay;
import static org.eclipse.ui.PlatformUI.*;

import org.eclipse.swt.widgets.Display;

/**
 * Utility methods related to <code>{@link Display}</code>s.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
public final class Displays {
  /**
   * Causes the {@code run()} method of the given {@code Runnable} to be invoked by the user-interface thread.
   * @param runnable the given {@code Runnable}.
   */
  public static void runInDisplayThread(Runnable runnable) {
    if (findDisplay(currentThread()) != null) {
      runnable.run();
      return;
    }
    if (isWorkbenchRunning()) {
      getWorkbench().getDisplay().syncExec(runnable);
    }
  }

  private Displays() {}
}
