/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.internal.model;

import org.eclipse.core.runtime.Assert;

import com.google.eclipse.elt.emulator.model.*;

/**
 * This class stores the data only within a window {@link #setWindow(int, int)} and {@link #getWindowStartLine()} and
 * {@link #getWindowSize()}. Everything outside the is {@code char=='\000'} and {@code style=null}.
 */
public class TerminalTextDataWindow implements ITerminalTextData {
  private final ITerminalTextData data;

  private int windowStartLine;
  private int windowSize;
  private int height;
  private int maxHeight;

  public TerminalTextDataWindow(ITerminalTextData data) {
    this.data = data;
  }

  public TerminalTextDataWindow() {
    this(new TerminalTextDataStore());
  }

  private boolean isInWindow(int line) {
    return line >= windowStartLine && line < windowStartLine + windowSize;
  }

  @Override public char getChar(int line, int column) {
    if (!isInWindow(line)) {
      return 0;
    }
    return data.getChar(line - windowStartLine, column);
  }

  @Override public char[] getChars(int line) {
    if (!isInWindow(line)) {
      return null;
    }
    return data.getChars(line - windowStartLine);
  }

  @Override public int getHeight() {
    return height;
  }

  @Override public LineSegment[] getLineSegments(int line, int startingColumn, int columnCount) {
    if (!isInWindow(line)) {
      return new LineSegment[] { new LineSegment(startingColumn, new String(new char[columnCount]), null) };
    }
    return data.getLineSegments(line - windowStartLine, startingColumn, columnCount);
  }

  @Override public int getMaxHeight() {
    return maxHeight;
  }

  @Override public Style getStyle(int line, int column) {
    if (!isInWindow(line)) {
      return null;
    }
    return data.getStyle(line - windowStartLine, column);
  }

  @Override public Style[] getStyles(int line) {
    if (!isInWindow(line)) {
      return null;
    }
    return data.getStyles(line - windowStartLine);
  }

  @Override public int getWidth() {
    return data.getWidth();
  }

  @Override public ITerminalTextDataSnapshot makeSnapshot() {
    throw new UnsupportedOperationException();
  }

  @Override public void addLine() {
    if (maxHeight > 0 && getHeight() < maxHeight) {
      setDimensions(getHeight() + 1, getWidth());
    } else {
      scroll(0, getHeight(), -1);
    }
  }

  @Override public void copy(ITerminalTextData source) {
    // We inherit the dimensions of the source.
    setDimensions(source.getHeight(), source.getWidth());
    int n = Math.min(windowSize, source.getHeight() - windowStartLine);
    if (n > 0) {
      data.copyRange(source, windowStartLine, 0, n);
    }
  }

  @Override public void copyRange(ITerminalTextData source, int sourceStartLine, int destinationStartLine, int length) {
    int newLength = length;
    int destinationStart = destinationStartLine - windowStartLine;
    int sourceStart = sourceStartLine;
    // If start outside our range, cut the length to copy.
    if (destinationStart < 0) {
      newLength += destinationStart;
      sourceStart -= destinationStart;
      destinationStart = 0;
    }
    // Do not exceed the window size.
    newLength = Math.min(newLength, windowSize);
    if (newLength > 0) {
      data.copyRange(source, sourceStart, destinationStart, newLength);
    }
  }

  @Override public void copyLine(ITerminalTextData source, int sourceLine, int destLine) {
    if (isInWindow(destLine)) {
      data.copyLine(source, sourceLine, destLine - windowStartLine);
    }
  }

  @Override public void scroll(int startLine, int lineCount, int shift) {
    Assert.isTrue(startLine >= 0 && startLine + lineCount <= height);
    int length = lineCount;
    int start = startLine - windowStartLine;
    // If start outside our range, cut the length to copy.
    if (start < 0) {
      length += start;
      start = 0;
    }
    length = Math.min(length, windowSize - start);
    // do not exceed the window size
    if (length > 0) {
      data.scroll(start, length, shift);
    }
  }

  @Override public void setChar(int line, int column, char c, Style style) {
    if (!isInWindow(line)) {
      return;
    }
    data.setChar(line - windowStartLine, column, c, style);
  }

  @Override public void setChars(int line, int column, char[] chars, int start, int len, Style style) {
    if (!isInWindow(line)) {
      return;
    }
    data.setChars(line - windowStartLine, column, chars, start, len, style);
  }

  @Override public void setChars(int line, int column, char[] chars, Style style) {
    if (!isInWindow(line)) {
      return;
    }
    data.setChars(line - windowStartLine, column, chars, style);
  }

  @Override public void setDimensions(int height, int width) {
    Assert.isTrue(height >= 0);
    data.setDimensions(windowSize, width);
    setHeight(height);
  }

  @Override public void setMaxHeight(int maxHeight) {
    this.maxHeight = maxHeight;
  }

  public void setWindow(int startLine, int size) {
    windowStartLine = startLine;
    windowSize = size;
    data.setDimensions(windowSize, getWidth());
  }

  public int getWindowStartLine() {
    return windowStartLine;
  }

  public int getWindowSize() {
    return windowSize;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  @Override public void cleanLine(int line) {
    if (isInWindow(line)) {
      data.cleanLine(line - windowStartLine);
    }
  }

  @Override public int getCursorColumn() {
    return data.getCursorColumn();
  }

  @Override public int getCursorLine() {
    return data.getCursorLine();
  }

  @Override public void setCursorColumn(int column) {
    data.setCursorColumn(column);
  }

  @Override public void setCursorLine(int line) {
    data.setCursorLine(line);
  }
}
