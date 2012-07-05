/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.tm.internal.terminal.emulator;

import static org.eclipse.jface.bindings.keys.SWTKeySupport.convertKeyStrokeToAccelerator;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
final class Accelerators {
  static int createEditActionAccelerator(int naturalKey) {
    int modifierKeys = (SWT.COMMAND == SWT.MOD1) ? SWT.COMMAND : SWT.CONTROL | SWT.SHIFT;
    KeyStroke keyStroke = KeyStroke.getInstance(modifierKeys, naturalKey);
    return convertKeyStrokeToAccelerator(keyStroke);
  }

  private Accelerators() {}
}
