/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.impl;

import java.io.OutputStream;

import com.google.eclipse.elt.emulator.provisional.api.*;

public interface ITerminalControlForText {

  TerminalState getState();

  void setState(TerminalState state);

  void setTerminalTitle(String title);

  ITerminalConnector getTerminalConnector();

  OutputStream getOutputStream();
}
