/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.impl;

import org.eclipse.osgi.util.NLS;

public class TerminalMessages extends NLS {
  public static String terminalError;
  public static String socketError;
  public static String ioError;
  public static String cannotConnectTo;
  public static String notInitialized;

  static {
    Class<?> clazz = TerminalMessages.class;
    NLS.initializeMessages(clazz.getName(), clazz);
  }
}
