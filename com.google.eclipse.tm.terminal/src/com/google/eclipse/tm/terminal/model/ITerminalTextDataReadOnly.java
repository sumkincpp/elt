/*******************************************************************************
 * Copyright (c) 2007, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *******************************************************************************/
package com.google.eclipse.tm.terminal.model;

public interface ITerminalTextDataReadOnly {
  int getWidth();

  int getHeight();

  LineSegment[] getLineSegments(int line, int startCol, int numberOfCols);

  char getChar(int line, int column);

  Style getStyle(int line, int column);

  /**
   * Creates a new instance of {@link ITerminalTextDataSnapshot} that can be used to track changes. Make sure to call
   * {@link ITerminalTextDataSnapshot#detach()} if you don't need the snapshots anymore.
   * <p>
   * <b>Note: </b>A new snapshot is empty and needs a call to {@link ITerminalTextDataSnapshot#updateSnapshot(boolean)}
   * to get its initial values. You might want to setup the snapshot to your needs by calling
   * {@link ITerminalTextDataSnapshot#setInterestWindow(int, int)}.
   * </p>
   *
   * @return a new instance of {@link ITerminalTextDataSnapshot} that "listens" to changes of this one.
   */
  public ITerminalTextDataSnapshot makeSnapshot();

  char[] getChars(int line);

  Style[] getStyles(int line);

  int getCursorLine();

  int getCursorColumn();
}