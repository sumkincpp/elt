/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.connector;

/**
 * Listens for events related to the life cycle of a pseudo-terminal process.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
public interface LifeCycleListener {
  /**
   * Notification that the execution pseudo-terminal process has finished.
   */
  void executionFinished();
}
