/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.view;


import static com.google.eclipse.terminal.local.core.connector.LocalTerminalConnector.createLocalTerminalConnector;
import static com.google.eclipse.tm.internal.terminal.provisional.api.TerminalState.CONNECTING;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.layout.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IViewSite;

import com.google.eclipse.terminal.local.core.connector.*;
import com.google.eclipse.tm.internal.terminal.control.ITerminalListener;
import com.google.eclipse.tm.internal.terminal.emulator.VT100TerminalControl;
import com.google.eclipse.tm.internal.terminal.provisional.api.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class TerminalWidget extends Composite {
  private final TerminalListener terminalListener = new TerminalListener();

  private final VT100TerminalControl terminalControl;

  private LifeCycleListener lifeCycleListener;

  TerminalWidget(Composite parent, IViewSite viewSite) {
    super(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(this);
    ITerminalConnector terminalConnector = createLocalTerminalConnector();
    terminalControl = new VT100TerminalControl(terminalListener, this, new ITerminalConnector[] { terminalConnector });
    terminalControl.setConnector(terminalConnector);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(terminalControl.getRootControl());
    terminalControl.setInvertedColors(true);
    addDisposeListener(new DisposeListener() {
      @Override public void widgetDisposed(DisposeEvent e) {
        disposeTerminalControl();
      }
    });
    final PopupMenu popupMenu = new PopupMenu(viewSite, terminalControl);
    terminalTextControl().addFocusListener(new FocusAdapter() {
      @Override public void focusGained(FocusEvent e) {
        popupMenu.update();
      }
    });
  }

  private Control terminalTextControl() {
    return terminalControl.getControl();
  }

  void connect() {
    if (terminalControl.getState() == CONNECTING || terminalControl.isDisposed()) {
      return;
    }
    terminalControl.connectTerminal();
    attachLifeCycleListener();
  }

  private void attachLifeCycleListener() {
    LocalTerminalConnector connector = localTerminalConnector();
    if (connector != null && lifeCycleListener != null) {
      connector.addLifeCycleListener(lifeCycleListener);
    }
  }

  void disposeTerminalControl() {
    if (!terminalControl.isDisposed()) {
      terminalControl.disposeTerminal();
    }
  }

  boolean isConnected() {
    return terminalControl.isConnected();
  }

  void setLifeCycleListener(LifeCycleListener listener) {
    lifeCycleListener = listener;
  }

  void setTerminalListener(ITerminalListener listener) {
    terminalListener.delegate = listener;
  }

  void setWorkingDirectory(IPath workingDirectory) {
    LocalTerminalConnector connector = localTerminalConnector();
    if (connector != null) {
      connector.setWorkingDirectory(workingDirectory);
    }
  }

  private LocalTerminalConnector localTerminalConnector() {
    Object connector = terminalControl.getTerminalConnector().getAdapter(LocalTerminalConnector.class);
    return (LocalTerminalConnector) connector;
  }

  void setColors(RGB background, RGB foreground) {
    terminalControl.setColors(background, foreground);
  }

  @Override public void setFont(Font font) {
    terminalControl.setFont(font);
  }

  void setBufferLineCount(int lineCount) {
    terminalControl.setBufferLineLimit(lineCount);
  }

  @Override public boolean setFocus() {
    return terminalControl.setFocus();
  }

  boolean isScrollLockEnabled() {
    return terminalControl.isScrollLockOn();
  }

  void enableScrollLock(boolean enabled) {
    terminalControl.setScrollLockOn(enabled);
  }

  void setBlinkingCursor(boolean useBlinkingCursor) {
    terminalControl.setBlinkingCursor(useBlinkingCursor);
  }

  private static class TerminalListener implements ITerminalListener {
    ITerminalListener delegate;

    @Override public void setState(TerminalState state) {
      if (delegate != null) {
        delegate.setState(state);
      }
    }

    @Override public void setTerminalTitle(String title) {
      if (delegate != null) {
        delegate.setTerminalTitle(title);
      }
    }
  }
}
