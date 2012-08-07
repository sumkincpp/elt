/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.internal.model;

import com.google.eclipse.elt.emulator.model.ITerminalTextData;

public interface ISnapshotChanges {
  void markLineChanged(int line);

  void markLinesChanged(int line, int size);

  /**
   * Marks all lines within the scrolling region changed and resets the scrolling information.
   */
  void convertScrollingIntoChanges();

  boolean hasChanged();

  void scroll(int startLine, int size, int shift);

  void setAllChanged(int height);

  int getFirstChangedLine();

  int getLastChangedLine();

  int getScrollWindowStartLine();

  int getScrollWindowSize();

  int getScrollWindowShift();

  boolean hasLineChanged(int line);

  void markDimensionsChanged();

  boolean hasDimensionsChanged();

  void markCursorChanged();

  boolean hasTerminalChanged();

  void setTerminalChanged();

  void copyChangedLines(ITerminalTextData destination, ITerminalTextData source);

  void setInterestWindow(int startLine, int size);

  int getInterestWindowStartLine();

  int getInterestWindowSize();
}
