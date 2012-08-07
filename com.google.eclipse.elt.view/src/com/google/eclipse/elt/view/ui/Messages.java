/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.ui;

import org.eclipse.osgi.util.NLS;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class Messages extends NLS {
  public static String alwaysCloseWithoutWarn;
  public static String changeTerminalTitle;
  public static String closeTerminalQuestion;
  public static String confirmCloseDialogTitle;
  public static String defaultViewTitle;
  public static String enterTerminalTitleDialogTitle;
  public static String enterTerminalTitlePrompt;
  public static String newLocalTerminal;
  public static String scrollLock;

  static {
    Class<Messages> type = Messages.class;
    NLS.initializeMessages(type.getName(), type);
  }

  private Messages() {}
}
