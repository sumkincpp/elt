/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.internal.model;

import java.util.*;

import com.google.eclipse.elt.emulator.model.*;

public class TerminalTextData implements ITerminalTextData {
  private final ITerminalTextData data;
  private final List<TerminalTextDataSnapshot> snapshots = new ArrayList<TerminalTextDataSnapshot>();
  private int cursorLine;
  private int cursorColumn;

  public TerminalTextData() {
    this(new TerminalTextDataFastScroll());
  }

  public TerminalTextData(ITerminalTextData data) {
    this.data = data;
  }

  @Override public int getWidth() {
    return data.getWidth();
  }

  @Override public int getHeight() {
    return data.getHeight();
  }

  @Override public void setDimensions(int height, int width) {
    int currentHeight = getHeight();
    int currentWidth = getWidth();
    if (currentWidth == width && currentHeight == height) {
      return;
    }
    data.setDimensions(height, width);
    sendDimensionsChanged(currentHeight, currentWidth, height, width);
  }

  private void sendDimensionsChanged(int oldHeight, int oldWidth, int newHeight, int newWidth) {
    // determine what has changed
    if (oldWidth == newWidth) {
      if (oldHeight < newHeight) {
        sendLinesChangedToSnapshot(oldHeight, newHeight - oldHeight);
      } else {
        sendLinesChangedToSnapshot(newHeight, oldHeight - newHeight);
      }
    } else {
      sendLinesChangedToSnapshot(0, oldHeight);
    }
    sendDimensionsChanged();
  }

  @Override public LineSegment[] getLineSegments(int line, int column, int len) {
    return data.getLineSegments(line, column, len);
  }

  @Override public char getChar(int line, int column) {
    return data.getChar(line, column);
  }

  @Override public Style getStyle(int line, int column) {
    return data.getStyle(line, column);
  }

  @Override public void setChar(int line, int column, char c, Style style) {
    data.setChar(line, column, c, style);
    sendLineChangedToSnapshots(line);
  }

  @Override public void setChars(int line, int column, char[] chars, Style style) {
    data.setChars(line, column, chars, style);
    sendLineChangedToSnapshots(line);
  }

  @Override public void setChars(int line, int column, char[] chars, int start, int length, Style style) {
    data.setChars(line, column, chars, start, length, style);
    sendLineChangedToSnapshots(line);
  }

  @Override public void scroll(int startLine, int size, int shift) {
    data.scroll(startLine, size, shift);
    sendScrolledToSnapshots(startLine, size, shift);
  }

  @Override public String toString() {
    return data.toString();
  }

  private void sendDimensionsChanged() {
    for (TerminalTextDataSnapshot snapshot : snapshots) {
      snapshot.markDimensionsChanged();
    }
  }

  protected void sendLineChangedToSnapshots(int line) {
    for (TerminalTextDataSnapshot snapshot : snapshots) {
      snapshot.markLineChanged(line);
    }
  }

  protected void sendLinesChangedToSnapshot(int startingLine, int lineCount) {
    for (TerminalTextDataSnapshot snapshot : snapshots) {
      snapshot.markLinesChanged(startingLine, lineCount);
    }
  }

  /**
   * Notifies snapshot that a region was scrolled.
   * @param startingLine first line of scrolled region.
   * @param lineCount size of scrolled region (number of lines.)
   * @param shift delta by which the region is scrolled.
   */
  protected void sendScrolledToSnapshots(int startingLine, int lineCount, int shift) {
    for (TerminalTextDataSnapshot snapshot : snapshots) {
      snapshot.scroll(startingLine, lineCount, shift);
    }
  }

  protected void sendCursorChanged() {
    for (TerminalTextDataSnapshot snapshot : snapshots) {
      snapshot.markCursorChanged();
    }
  }

  /**
   * Removes the snapshot from the observer list.
   * @param snapshot a snapshot of a terminal model.
   */
  protected void removeSnapshot(TerminalTextDataSnapshot snapshot) {
    snapshots.remove(snapshot);
  }

  @Override public ITerminalTextDataSnapshot makeSnapshot() {
    TerminalTextDataSnapshot snapshot = new TerminalTextDataSnapshot(this);
    snapshot.markDimensionsChanged();
    snapshots.add(snapshot);
    return snapshot;
  }

  @Override public void addLine() {
    int oldHeight = getHeight();
    data.addLine();
    // was is an append or a scroll?
    int newHeight = getHeight();
    if (newHeight > oldHeight) {
      // the line was appended
      sendLinesChangedToSnapshot(oldHeight, 1);
      int width = getWidth();
      sendDimensionsChanged(oldHeight, width, newHeight, width);
    } else {
      // the line was scrolled
      sendScrolledToSnapshots(0, oldHeight, -1);
    }
  }

  @Override public void copy(ITerminalTextData source) {
    data.copy(source);
    cursorLine = source.getCursorLine();
    cursorColumn = source.getCursorColumn();
  }

  @Override public void copyLine(ITerminalTextData source, int sourceLine, int destinationLine) {
    data.copyLine(source, sourceLine, destinationLine);
  }

  @Override public void copyRange(ITerminalTextData source, int sourceStartLine, int destinationStartLine, int length) {
    data.copyRange(source, sourceStartLine, destinationStartLine, length);
  }

  @Override public char[] getChars(int line) {
    return data.getChars(line);
  }

  @Override public Style[] getStyles(int line) {
    return data.getStyles(line);
  }

  @Override public int getMaxHeight() {
    return data.getMaxHeight();
  }

  @Override public void setMaxHeight(int height) {
    data.setMaxHeight(height);
  }

  @Override public void cleanLine(int line) {
    data.cleanLine(line);
    sendLineChangedToSnapshots(line);
  }

  @Override public int getCursorColumn() {
    return cursorColumn;
  }

  @Override public int getCursorLine() {
    return cursorLine;
  }

  @Override public void setCursorColumn(int column) {
    cursorColumn = column;
    sendCursorChanged();
  }

  @Override public void setCursorLine(int line) {
    cursorLine = line;
    sendCursorChanged();
  }
}
