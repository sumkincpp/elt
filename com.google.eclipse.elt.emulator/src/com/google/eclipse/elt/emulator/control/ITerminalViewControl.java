/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.google.eclipse.elt.emulator.control;

import java.io.UnsupportedEncodingException;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;

import com.google.eclipse.elt.emulator.provisional.api.*;

public interface ITerminalViewControl {
  void setEncoding(String encoding) throws UnsupportedEncodingException;

  String getEncoding();

  boolean isEmpty();

  void setFont(Font font);

  void setInvertedColors(boolean invert);

  Font getFont();

  Control getControl();

  Control getRootControl();

  boolean isDisposed();

  void selectAll();

  void clearTerminal();

  void copy();

  void paste();

  String getSelection();

  TerminalState getState();

  Clipboard getClipboard();

  void connectTerminal();

  void disconnectTerminal();

  void disposeTerminal();

  boolean isConnected();

  String getSettingsSummary();

  boolean setFocus();

  ITerminalConnector getTerminalConnector();

  void setConnector(ITerminalConnector connector);

  ITerminalConnector[] getConnectors();

  void sendKey(char arg0);

  public boolean pasteString(String string);

  void setCommandInputField(ICommandInputField inputField);

  ICommandInputField getCommandInputField();

  public int getBufferLineLimit();

  public void setBufferLineLimit(int bufferLineLimit);

  boolean isScrollLockOn();

  void setScrollLockOn(boolean on);
}
