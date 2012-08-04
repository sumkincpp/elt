/*******************************************************************************
 * Copyright (c) 2006, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.provisional.api;

import java.io.OutputStream;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A connection type.
 *
 * @author Michael Scharf
 */
public interface ITerminalConnector extends IAdaptable {
  String getId();

  String getName();

  boolean isInitialized();

  String getInitializationErrorMessage();

  void connect(ITerminalControl control);

  void disconnect();

  boolean isLocalEcho();

  void setTerminalSize(int newWidth, int newHeight);

  OutputStream getTerminalToRemoteStream();

  String getSettingsSummary();
}
