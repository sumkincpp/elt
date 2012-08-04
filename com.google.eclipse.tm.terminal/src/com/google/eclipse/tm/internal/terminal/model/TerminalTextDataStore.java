/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.model;

import static java.util.Arrays.copyOf;

import java.lang.reflect.Array;
import java.util.*;

import org.eclipse.core.runtime.Assert;

import com.google.eclipse.tm.terminal.model.*;

public class TerminalTextDataStore implements ITerminalTextData {
  private char[][] chars;
  private Style[][] styles;
  private int width;
  private int height;
  private int maxHeight;
  private int cursorColumn;
  private int cursorLine;

  public TerminalTextDataStore() {
    chars = new char[0][];
    styles = new Style[0][];
    width = 0;
  }

  @Override public int getWidth() {
    return width;
  }

  @Override public int getHeight() {
    return height;
  }

  @Override public void setDimensions(int height, int width) {
    Assert.isTrue(height >= 0 && width >= 0);
    // Just extend the region.
    if (height > chars.length) {
      int h = 4 * height / 3;
      if (maxHeight > 0 && h > maxHeight) {
        h = maxHeight;
      }
      styles = (Style[][]) resizeArray(styles, height);
      chars = (char[][]) resizeArray(chars, height);
    }
    // Clean the new lines
    if (height > this.height) {
      for (int i = this.height; i < height; i++) {
        styles[i] = null;
        chars[i] = null;
      }
    }
    // Set dimensions after successful resize.
    this.width = width;
    this.height = height;
  }

  /**
   * Reallocates an array with a new size, and copies the contents of the old array to the new array.
   *
   * @param originalArray the old array, to be reallocated.
   * @param newSize the new array size.
   * @return A new array with the same contents (chopped off if needed or filled with 0 or null).
   */
  private Object resizeArray(Object originalArray, int newSize) {
    int oldSize = Array.getLength(originalArray);
    if (oldSize == newSize) {
      return originalArray;
    }
    Class<?> elementType = originalArray.getClass().getComponentType();
    Object newArray = Array.newInstance(elementType, newSize);
    int preserveLength = Math.min(oldSize, newSize);
    if (preserveLength > 0) {
      System.arraycopy(originalArray, 0, newArray, 0, preserveLength);
    }
    return newArray;
  }

  @Override public LineSegment[] getLineSegments(int line, int startingColumn, int columnCount) {
    // get the styles and chars for this line
    Style[] styles = this.styles[line];
    char[] chars = this.chars[line];
    int column = startingColumn;
    int size = startingColumn + columnCount;
    // Expand the line if needed
    if (styles == null) {
      styles = new Style[size];
    } else if (styles.length < size) {
      styles = (Style[]) resizeArray(styles, size);
    }
    if (chars == null) {
      chars = new char[size];
    } else if (chars.length < size) {
      chars = (char[]) resizeArray(chars, size);
    }
    // Create the line segments
    Style style = styles[startingColumn];
    List<LineSegment> segments = new ArrayList<LineSegment>();
    for (int i = startingColumn; i < size; i++) {
      if (styles[i] != style) {
        segments.add(new LineSegment(column, new String(chars, column, i - column), style));
        style = styles[i];
        column = i;
      }
    }
    if (column < size) {
      segments.add(new LineSegment(column, new String(chars, column, size - column), style));
    }
    return segments.toArray(new LineSegment[segments.size()]);
  }

  @Override public char getChar(int line, int column) {
    Assert.isTrue(column < width);
    if (chars[line] == null || column >= chars[line].length) {
      return 0;
    }
    return chars[line][column];
  }

  @Override public Style getStyle(int line, int column) {
    Assert.isTrue(column < width);
    if (styles[line] == null || column >= styles[line].length) {
      return null;
    }
    return styles[line][column];
  }

  void ensureLineLength(int iLine, int length) {
    if (length > width) {
      throw new RuntimeException();
    }
    if (chars[iLine] == null) {
      chars[iLine] = new char[length];
    } else if (chars[iLine].length < length) {
      chars[iLine] = (char[]) resizeArray(chars[iLine], length);
    }
    if (styles[iLine] == null) {
      styles[iLine] = new Style[length];
    } else if (styles[iLine].length < length) {
      styles[iLine] = (Style[]) resizeArray(styles[iLine], length);
    }
  }

  @Override public void setChar(int line, int column, char c, Style style) {
    ensureLineLength(line, column + 1);
    chars[line][column] = c;
    styles[line][column] = style;
  }

  @Override public void setChars(int line, int column, char[] chars, Style style) {
    setChars(line, column, chars, 0, chars.length, style);
  }

  @Override public void setChars(int line, int column, char[] chars, int start, int len, Style style) {
    ensureLineLength(line, column + len);
    for (int i = 0; i < len; i++) {
      this.chars[line][column + i] = chars[i + start];
      this.styles[line][column + i] = style;
    }
  }

  @Override public void scroll(int startLine, int size, int shift) {
    Assert.isTrue(startLine + size <= getHeight());
    if (shift < 0) {
      // move the region up
      for (int i = startLine; i < startLine + size + shift; i++) {
        chars[i] = chars[i - shift];
        styles[i] = styles[i - shift];
      }
      // then clean the opened lines
      cleanLines(Math.max(startLine, startLine + size + shift), Math.min(-shift, getHeight() - startLine));
    } else {
      for (int i = startLine + size - 1; i >= startLine && i - shift >= 0; i--) {
        chars[i] = chars[i - shift];
        styles[i] = styles[i - shift];
      }
      cleanLines(startLine, Math.min(shift, getHeight() - startLine));
    }
  }

  private void cleanLines(int line, int len) {
    for (int i = line; i < line + len; i++) {
      chars[i] = null;
      styles[i] = null;
    }
  }

  @Override public String toString() {
    StringBuffer buff = new StringBuffer();
    for (int line = 0; line < getHeight(); line++) {
      if (line > 0) {
        buff.append("\n");
      }
      for (int column = 0; column < width; column++) {
        buff.append(getChar(line, column));
      }
    }
    return buff.toString();
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
    width = source.getWidth();
    int newHeight = source.getHeight();
    if (getHeight() != newHeight) {
      chars = new char[newHeight][];
      styles = new Style[newHeight][];
    }
    for (int i = 0; i < newHeight; i++) {
      chars[i] = source.getChars(i);
      styles[i] = source.getStyles(i);
    }
    height = newHeight;
    cursorLine = source.getCursorLine();
    cursorColumn = source.getCursorColumn();
  }

  @Override public void copyRange(ITerminalTextData source, int sourceStartLine, int destStartLine, int length) {
    for (int i = 0; i < length; i++) {
      chars[i + destStartLine] = source.getChars(i + sourceStartLine);
      styles[i + destStartLine] = source.getStyles(i + sourceStartLine);
    }
  }

  @Override public void copyLine(ITerminalTextData source, int sourceLine, int destLine) {
    chars[destLine] = source.getChars(sourceLine);
    styles[destLine] = source.getStyles(sourceLine);
  }

  @Override public char[] getChars(int line) {
    if (chars[line] == null) {
      return null;
    }
    return chars[line].clone();
  }

  @Override public Style[] getStyles(int line) {
    if (styles[line] == null) {
      return null;
    }
    return styles[line].clone();
  }

  public void setLine(int line, char[] chars, Style[] styles) {
    this.chars[line] = copyOf(chars, chars.length);
    this.styles[line] = copyOf(styles, styles.length);
  }

  @Override public void setMaxHeight(int height) {
    maxHeight = height;
  }

  @Override public int getMaxHeight() {
    return maxHeight;
  }

  @Override public void cleanLine(int line) {
    chars[line] = null;
    styles[line] = null;
  }

  @Override public int getCursorColumn() {
    return cursorColumn;
  }

  @Override public int getCursorLine() {
    return cursorLine;
  }

  @Override public void setCursorColumn(int column) {
    cursorColumn = column;
  }

  @Override public void setCursorLine(int line) {
    cursorLine = line;
  }
}
