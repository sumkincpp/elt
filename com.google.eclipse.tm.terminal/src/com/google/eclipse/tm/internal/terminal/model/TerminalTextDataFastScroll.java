/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.model;

import org.eclipse.core.runtime.Assert;

import com.google.eclipse.tm.terminal.model.*;

/**
 * This class is optimized for scrolling the entire {@link #getHeight()}. The scrolling is done by moving an offset into
 * the data and using the modulo operator.
 */
public class TerminalTextDataFastScroll implements ITerminalTextData {
  private final ITerminalTextData data;
  private int height;
  private int maxHeight;
  private int offset;

  public TerminalTextDataFastScroll(ITerminalTextData data, int maxHeight) {
    this.maxHeight = maxHeight;
    this.data = data;
    this.data.setDimensions(this.maxHeight, this.data.getWidth());
    if (this.maxHeight > 2) {
      moveOffset(-2);
    }
  }

  public TerminalTextDataFastScroll(int maxHeight) {
    this(new TerminalTextDataStore(), maxHeight);
  }

  public TerminalTextDataFastScroll() {
    this(new TerminalTextDataStore(), 1);
  }

  private int getPositionOfLine(int line) {
    return (line + offset) % maxHeight;
  }

  private void moveOffset(int delta) {
    Assert.isTrue(Math.abs(delta) < maxHeight);
    offset = (maxHeight + offset + delta) % maxHeight;
  }

  @Override public void addLine() {
    if (getHeight() < maxHeight) {
      setDimensions(getHeight() + 1, getWidth());
    } else {
      scroll(0, getHeight(), -1);
    }
  }

  @Override public void cleanLine(int line) {
    data.cleanLine(getPositionOfLine(line));
  }

  @Override public void copy(ITerminalTextData source) {
    int height = source.getHeight();
    setDimensions(source.getHeight(), source.getWidth());
    for (int i = 0; i < height; i++) {
      data.copyLine(source, i, getPositionOfLine(i));
    }
  }

  @Override public void copyLine(ITerminalTextData source, int sourceLine, int destinationLine) {
    data.copyLine(source, sourceLine, getPositionOfLine(destinationLine));
  }

  @Override public void copyRange(ITerminalTextData source, int sourceStartLine, int destinationStartLine, int length) {
    Assert.isTrue(destinationStartLine >= 0 && destinationStartLine + length <= height);
    for (int i = 0; i < length; i++) {
      data.copyLine(source, i + sourceStartLine, getPositionOfLine(i + destinationStartLine));
    }
  }

  @Override public char getChar(int line, int column) {
    Assert.isTrue(line >= 0 && line < height);
    return data.getChar(getPositionOfLine(line), column);
  }

  @Override public char[] getChars(int line) {
    Assert.isTrue(line >= 0 && line < height);
    return data.getChars(getPositionOfLine(line));
  }

  @Override public int getHeight() {
    return height;
  }

  @Override public LineSegment[] getLineSegments(int line, int startCol, int numberOfCols) {
    Assert.isTrue(line >= 0 && line < height);
    return data.getLineSegments(getPositionOfLine(line), startCol, numberOfCols);
  }

  @Override public int getMaxHeight() {
    return maxHeight;
  }

  @Override public Style getStyle(int line, int column) {
    Assert.isTrue(line >= 0 && line < height);
    return data.getStyle(getPositionOfLine(line), column);
  }

  @Override public Style[] getStyles(int line) {
    Assert.isTrue(line >= 0 && line < height);
    return data.getStyles(getPositionOfLine(line));
  }

  @Override public int getWidth() {
    return data.getWidth();
  }

  @Override public ITerminalTextDataSnapshot makeSnapshot() {
    return data.makeSnapshot();
  }

  private void cleanLines(int line, int len) {
    for (int i = line; i < line + len; i++) {
      data.cleanLine(getPositionOfLine(i));
    }
  }

  @Override public void scroll(int startLine, int size, int shift) {
    Assert.isTrue(startLine >= 0 && startLine + size <= height);
    if (shift >= maxHeight || -shift >= maxHeight) {
      cleanLines(startLine, maxHeight - startLine);
      return;
    }
    if (size == height) {
      // This is the case this class is optimized for!
      moveOffset(-shift);
      // We only have to clean the lines that appear by the move.
      if (shift < 0) {
        cleanLines(Math.max(startLine, startLine + size + shift), Math.min(-shift, getHeight() - startLine));
      } else {
        cleanLines(startLine, Math.min(shift, getHeight() - startLine));
      }
    } else {
      // we have to copy the lines.
      if (shift < 0) {
        // move the region up
        for (int i = startLine; i < startLine + size + shift; i++) {
          data.copyLine(data, getPositionOfLine(i - shift), getPositionOfLine(i));
        }
        // then clean the opened lines
        cleanLines(Math.max(0, startLine + size + shift), Math.min(-shift, getHeight() - startLine));
      } else {
        for (int i = startLine + size - 1; i >= startLine && i - shift >= 0; i--) {
          data.copyLine(data, getPositionOfLine(i - shift), getPositionOfLine(i));
        }
        cleanLines(startLine, Math.min(shift, getHeight() - startLine));
      }
    }
  }

  @Override public void setChar(int line, int column, char c, Style style) {
    Assert.isTrue(line >= 0 && line < height);
    data.setChar(getPositionOfLine(line), column, c, style);
  }

  @Override public void setChars(int line, int column, char[] chars, int start, int len, Style style) {
    Assert.isTrue(line >= 0 && line < height);
    data.setChars(getPositionOfLine(line), column, chars, start, len, style);
  }

  @Override public void setChars(int line, int column, char[] chars, Style style) {
    Assert.isTrue(line >= 0 && line < height);
    data.setChars(getPositionOfLine(line), column, chars, style);
  }

  @Override public void setDimensions(int height, int width) {
    Assert.isTrue(height >= 0 && width >= 0);
    if (height > maxHeight) {
      setMaxHeight(height);
    }
    this.height = height;
    if (width != data.getWidth()) {
      data.setDimensions(maxHeight, width);
    }
  }

  @Override public void setMaxHeight(int maxHeight) {
    Assert.isTrue(maxHeight >= height);
    // move everything to offset0
    int start = getPositionOfLine(0);
    if (start != 0) {
      // invent a more efficient algorithm....
      ITerminalTextData buffer = new TerminalTextDataStore();
      // create a buffer with the expected height
      buffer.setDimensions(maxHeight, getWidth());
      int n = Math.min(maxHeight - start, maxHeight);
      // copy the first part
      buffer.copyRange(data, start, 0, n);
      // copy the second part
      if (n < maxHeight) {
        buffer.copyRange(data, 0, n, Math.min(maxHeight - n, maxHeight - n));
      }
      // copy the buffer back to our data
      data.copy(buffer);
      moveOffset(-start);
    } else {
      data.setDimensions(maxHeight, data.getWidth());
    }
    this.maxHeight = maxHeight;
  }

  @Override public int getCursorColumn() {
    throw new UnsupportedOperationException();
  }

  @Override public int getCursorLine() {
    throw new UnsupportedOperationException();
  }

  @Override public void setCursorColumn(int column) {
    throw new UnsupportedOperationException();
  }

  @Override public void setCursorLine(int line) {
    throw new UnsupportedOperationException();
  }
}
