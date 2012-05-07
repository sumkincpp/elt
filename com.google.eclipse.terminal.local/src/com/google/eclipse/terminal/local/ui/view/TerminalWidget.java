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
import static org.eclipse.tm.internal.terminal.provisional.api.TerminalState.CONNECTING;

import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.tm.internal.terminal.control.ITerminalListener;
import org.eclipse.tm.internal.terminal.emulator.VT100TerminalControl;
import org.eclipse.tm.internal.terminal.provisional.api.*;
import org.eclipse.ui.IActionBars;

import com.google.eclipse.terminal.local.core.connector.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class TerminalWidget extends Composite {
  private static Pattern WHITE_SPACE_PATTERN = Pattern.compile("[\\s]+");

  private final TerminalListener terminalListener = new TerminalListener();

  private final VT100TerminalControl terminalControl;
  private final EditActions editActions;
  private final LastCommandTracker lastCommandTracker;

  private LifeCycleListener lifeCycleListener;

  TerminalWidget(Composite parent, int style) {
    super(parent, style);
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
    editActions = new EditActions(terminalControl);
    MenuManager menuManager = new MenuManager("#PopupMenu");
    editActions.addActionsTo(menuManager);
    menuManager.addMenuListener(new IMenuListener() {
      @Override public void menuAboutToShow(IMenuManager manager) {
        editActions.update();
      }
    });
    Menu menu = createContextMenu(menuManager);
    menu.addMenuListener(new MenuAdapter() {
      @Override public void menuHidden(MenuEvent e) {
        editActions.onMenuHidden();
      }
    });
    Control terminalTextControl = terminalTextControl();
    terminalTextControl.addFocusListener(new FocusAdapter() {
      @Override public void focusGained(FocusEvent e) {
        editActions.update();
      }
    });
    lastCommandTracker = new LastCommandTracker();
    terminalTextControl.addKeyListener(lastCommandTracker);
  }

  private Menu createContextMenu(MenuManager menuManager) {
    Control control = terminalTextControl();
    Menu menu = menuManager.createContextMenu(control);
    control.setMenu(menu);
    return menu;
  }

  private Control terminalTextControl() {
    return terminalControl.getControl();
  }

  void setUpGlobalEditActionHandlers(IActionBars actionBars) {
    editActions.setUpGlobalActionHandlers(actionBars);
  }

  void connect() {
    if (terminalControl.getState() == CONNECTING || terminalControl.isDisposed()) {
      return;
    }
    terminalControl.connectTerminal();
    localTerminalConnector().addListenerToOutput(lastCommandTracker);
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
    return terminalControl.isScrollLock();
  }

  void enableScrollLock(boolean enabled) {
    terminalControl.setScrollLock(enabled);
  }

  private class LastCommandTracker extends KeyAdapter implements IStreamListener {
    private static final String CRLF = "\r\n";

    private final List<String> words = new ArrayList<String>();

    @Override public void streamAppended(String text, IStreamMonitor monitor) {
      int charCount = text.length();
      if (charCount == 0) {
        return;
      }
      String word = text;
      int index = text.lastIndexOf(CRLF);
      if (index != -1) {
        words.clear();
        word = text.substring(index + CRLF.length(), charCount);
      }
      if (!word.isEmpty()) {
        words.add(word);
      }
    }

    @Override public void keyPressed(KeyEvent e) {
      if (e.character == '\r') {
        int line = terminalControl.getCursorLine();
        String text = new String(terminalControl.getChars(line));
        if (words.size() > 1) {
          String prompt = words.get(0);
          text = text.substring(prompt.length());
          String command = WHITE_SPACE_PATTERN.split(text)[0];
          System.out.println("command: " + command);
        }
      }
    }
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
