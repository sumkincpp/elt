/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.textcanvas;

import org.eclipse.swt.widgets.Display;

import com.google.eclipse.tm.terminal.model.ITerminalTextDataSnapshot;

public class PollingTextCanvasModel extends AbstractTextCanvasModel {
  private int pollInterval = 50;

  public PollingTextCanvasModel(ITerminalTextDataSnapshot snapshot) {
    super(snapshot);
    Display.getDefault().timerExec(pollInterval, new Runnable() {
      @Override public void run() {
        update();
        Display.getDefault().timerExec(pollInterval, this);
      }
    });
  }

  public void setUpdateInterval(int interval) {
    pollInterval = interval;
  }
}
