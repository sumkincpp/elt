/*******************************************************************************
 * Copyright (c) 2003, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.control.actions;

import org.eclipse.osgi.util.NLS;

public class ActionMessages extends NLS {
  public static String copy;
  public static String cut;
  public static String paste;
  public static String selectAll;
  public static String clearAll;

  static {
    Class<?> clazz = ActionMessages.class;
    NLS.initializeMessages(clazz.getName(), clazz);
  }
}
