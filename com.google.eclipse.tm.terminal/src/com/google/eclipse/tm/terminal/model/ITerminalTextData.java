/*******************************************************************************
 * Copyright (c) 2007, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.terminal.model;

/**
 * A writable matrix of characters and {@link Style}. This is intended to be the low level representation of the text of
 * a Terminal. Higher layers are responsible to fill the text and styles into this representation.
 */
public interface ITerminalTextData extends ITerminalTextDataReadOnly {

  void setDimensions(int height, int width);

  void setMaxHeight(int height);

  int getMaxHeight();

  void setChar(int line, int column, char c, Style style);

  void setChars(int line, int column, char[] chars, Style style);

  void setChars(int line, int column, char[] chars, int start, int len, Style style);

  void cleanLine(int line);

  /**
   * Shifts some lines up or down. The "empty" space is filled with {@code '\000'} chars and {@code null} {@link Style}.
   * <p>
   * To illustrate shift, here is some sample data:
   * <pre>
   * 0 aaaa
   * 1 bbbb
   * 2 cccc
   * 3 dddd
   * 4 eeee
   * </pre>
   *
   * Shift a region of 3 lines <b>up</b> by one line {@code shift(1,3,-1)}
   * <pre>
   * 0 aaaa
   * 1 cccc
   * 2 dddd
   * 3
   * 4 eeee
   * </pre>
   *
   * Shift a region of 3 lines <b>down</b> by one line {@code shift(1,3,1)}
   * <pre>
   * 0 aaaa
   * 1
   * 2 bbbb
   * 3 cccc
   * 4 eeee
   * </pre>
   *
   * @param startLine the start line of the shift.
   * @param size the number of lines to shift.
   * @param shift how much scrolling is done. New scrolled area is filled with {@code '\000'}. A negative number means
   *        scroll down, positive scroll up (see example above).
   */
  void scroll(int startLine, int size, int shift);

  /**
   * Adds a new line to the terminal. If maxHeigth is reached, the entire terminal will be scrolled. Else a line will be
   * added.
   */
  void addLine();

  void copy(ITerminalTextData source);

  void copyLine(ITerminalTextData source, int sourceLine, int destinationLine);

  void copyRange(ITerminalTextData source, int sourceStartLine, int destinationStartLine, int length);

  void setCursorLine(int line);

  void setCursorColumn(int column);
}
