/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.view;

import java.util.*;
import java.util.regex.Pattern;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.swt.events.*;
import org.eclipse.tm.internal.terminal.emulator.VT100TerminalControl;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class LastCommandTracker extends KeyAdapter implements IStreamListener {
  private static final String CRLF = "\r\n";
  private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("[\\s]+");

  private final List<String> words = new ArrayList<String>();

  private final VT100TerminalControl terminalControl;
  private CommandListener commandListener;

  LastCommandTracker(VT100TerminalControl terminalControl) {
    this.terminalControl = terminalControl;
  }

  void setCommandListener(CommandListener listener) {
    commandListener = listener;
  }

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
      trackLastCommand();
    }
  }

  private void trackLastCommand() {
    int line = terminalControl.getCursorLine();
    String text = textAtLine(line);
    if (text.isEmpty() || words.isEmpty()) {
      return;
    }
    String prompt = words.get(0);
    if (text.startsWith(prompt)) {
      String command = lastCommand(prompt, text);
      notifyCommandIssued(command);
      return;
    }
    // most likely line wrapped;
    while (!text.startsWith(prompt)) {
      String previousLineText = textAtLine(--line);
      if (previousLineText.isEmpty()) {
        return;
      }
      text = previousLineText.concat(text);
    }
    String command = lastCommand(prompt, text);
    notifyCommandIssued(command);
  }

  private String textAtLine(int line) {
    if (line < 0) {
      return "";
    }
    char[] chars = terminalControl.getChars(line);
    if (chars == null || chars.length == 0) {
      return "";
    }
    return new String(chars).trim();
  }

  private String lastCommand(String prompt, String text) {
    String command = null;
    String withoutPrompt = text.substring(prompt.length());
    String[] actualInput = WHITE_SPACE_PATTERN.split(withoutPrompt);
    if (actualInput.length != 0) {
      command = actualInput[0];
    }
    return (command == null) ? "" : command;
  }

  private void notifyCommandIssued(String command) {
    if (!command.isEmpty() && commandListener != null) {
      commandListener.commandIssued(command);
    }
  }
}