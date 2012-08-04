/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse public synchronized License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.model;

import com.google.eclipse.tm.terminal.model.*;

/**
 * This is a decorator to make all access to an {@link ITerminalTextData} synchronized.
 */
public class SynchronizedTerminalTextData implements ITerminalTextData {
  private final ITerminalTextData data;

  public SynchronizedTerminalTextData(ITerminalTextData data) {
    this.data = data;
  }

  @Override public synchronized void addLine() {
    data.addLine();
  }

  @Override public synchronized void cleanLine(int line) {
    data.cleanLine(line);
  }

  @Override public synchronized void copy(ITerminalTextData source) {
    data.copy(source);
  }

  @Override public synchronized void copyLine(ITerminalTextData source, int sourceLine, int destinationLine) {
    data.copyLine(source, sourceLine, destinationLine);
  }

  @Override public synchronized void copyRange(
      ITerminalTextData source, int sourceStartLine, int destinationStartLine, int length) {
    data.copyRange(source, sourceStartLine, destinationStartLine, length);
  }

  @Override public synchronized char getChar(int line, int column) {
    return data.getChar(line, column);
  }

  @Override public synchronized char[] getChars(int line) {
    return data.getChars(line);
  }

  @Override public synchronized int getCursorColumn() {
    return data.getCursorColumn();
  }

  @Override public synchronized int getCursorLine() {
    return data.getCursorLine();
  }

  @Override public synchronized int getHeight() {
    return data.getHeight();
  }

  @Override public synchronized LineSegment[] getLineSegments(int line, int startColumn, int columnCount) {
    return data.getLineSegments(line, startColumn, columnCount);
  }

  @Override public synchronized int getMaxHeight() {
    return data.getMaxHeight();
  }

  @Override public synchronized Style getStyle(int line, int column) {
    return data.getStyle(line, column);
  }

  @Override public synchronized Style[] getStyles(int line) {
    return data.getStyles(line);
  }

  @Override public synchronized int getWidth() {
    return data.getWidth();
  }

  @Override public synchronized ITerminalTextDataSnapshot makeSnapshot() {
    return data.makeSnapshot();
  }

  @Override public synchronized void scroll(int startLine, int size, int shift) {
    data.scroll(startLine, size, shift);
  }

  @Override public synchronized void setChar(int line, int column, char c, Style style) {
    data.setChar(line, column, c, style);
  }

  @Override public synchronized void setChars(int line, int column, char[] chars, int start, int length, Style style) {
    data.setChars(line, column, chars, start, length, style);
  }

  @Override public synchronized void setChars(int line, int column, char[] chars, Style style) {
    data.setChars(line, column, chars, style);
  }

  @Override public synchronized void setCursorColumn(int column) {
    data.setCursorColumn(column);
  }

  @Override public synchronized void setCursorLine(int line) {
    data.setCursorLine(line);
  }

  @Override public synchronized void setDimensions(int height, int width) {
    data.setDimensions(height, width);
  }

  @Override public synchronized void setMaxHeight(int height) {
    data.setMaxHeight(height);
  }
}
