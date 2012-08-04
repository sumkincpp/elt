/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.terminal.model;

import com.google.eclipse.tm.internal.terminal.model.*;

public class TerminalTextDataFactory {
  public static ITerminalTextData makeTerminalTextData() {
    return new SynchronizedTerminalTextData(new TerminalTextData());
  }
}
