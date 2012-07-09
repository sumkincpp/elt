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

import java.util.*;

import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class EditActionAccelerators {
  private static final String COPY_COMMAND_ID = "com.google.eclipse.terminal.local.copy"; //$NON-NLS-1$
  private static final String PASTE_COMMAND_ID = "com.google.eclipse.terminal.local.paste"; //$NON-NLS-1$

  private final Map<Integer, String> commandIdsByAccelerator = new HashMap<Integer, String>();

  private void load() {
    addAccelerator(COPY_COMMAND_ID);
    addAccelerator(PASTE_COMMAND_ID);
  }

  private void addAccelerator(String commandId) {
    KeySequence keySequence = bindingFor(commandId);
    if (keySequence == null) {
      return;
    }
    KeyStroke[] keyStrokes = keySequence.getKeyStrokes();
    if (keyStrokes.length != 0) {
      int accelerator = convertKeyStrokeToAccelerator(keyStrokes[0]);
      commandIdsByAccelerator.put(new Integer(accelerator), commandId);
    }
  }

  private static KeySequence bindingFor(String commandId) {
    IBindingService bindingService = bindingService();
    TriggerSequence binding = bindingService.getBestActiveBindingFor(commandId);
    if (binding instanceof KeySequence) {
      KeySequence keySequence = (KeySequence) binding;
      return keySequence;
    }
    return null;
  }

  private static IBindingService bindingService() {
    return (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
  }

  boolean isCopyAction(int accelerator) {
    return isMatchingAction(accelerator, COPY_COMMAND_ID);
  }

  boolean isPasteAction(int accelerator) {
    return isMatchingAction(accelerator, PASTE_COMMAND_ID);
  }

  private boolean isMatchingAction(int accelerator, String commandId) {
    if (commandIdsByAccelerator.isEmpty()) {
      load();
    }
    return commandId.equals(commandIdsByAccelerator.get(new Integer(accelerator)));
  }
}
