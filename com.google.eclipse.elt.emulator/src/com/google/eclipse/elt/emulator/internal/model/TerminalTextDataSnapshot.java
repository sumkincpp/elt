/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.internal.model;

import java.util.*;

import org.eclipse.core.runtime.Assert;

import com.google.eclipse.elt.emulator.model.*;

class TerminalTextDataSnapshot implements ITerminalTextDataSnapshot {
  // The changes of the current snapshot relative to the previous snapshot.
  private volatile ISnapshotChanges currentChanges;

  // Keeps track of changes that happened since the current snapshot has been made.
  private ISnapshotChanges futureChanges;

  // Is used as lock and is the reference to the terminal we take snapshots from.
  private final TerminalTextData terminal;

  // A snapshot copy of terminal. It does not need internal synchronization.
  private final TerminalTextDataWindow snapshot;

  private final List<SnapshotOutOfDateListener> listeners = new ArrayList<SnapshotOutOfDateListener>();

  private boolean listenersNeedNotify;
  private int interestWindowSize;
  private int interestWindowStartLine;

  TerminalTextDataSnapshot(TerminalTextData terminal) {
    snapshot = new TerminalTextDataWindow();
    this.terminal = terminal;
    currentChanges = new SnapshotChanges(this.terminal.getHeight());
    currentChanges.setTerminalChanged();
    futureChanges = new SnapshotChanges(this.terminal.getHeight());
    futureChanges.markLinesChanged(0, this.terminal.getHeight());
    listenersNeedNotify = true;
    interestWindowSize = -1;
  }

  @Override public void detach() {
    terminal.removeSnapshot(this);
  }

  @Override public boolean isOutOfDate() {
    // This is called from terminal, therefore we lock on terminal
    synchronized (terminal) {
      return futureChanges.hasChanged();
    }
  }

  @Override public void updateSnapshot(boolean detectScrolling) {
    // Make sure terminal does not change while we make the snapshot.
    synchronized (terminal) {
      // let's make the future changes current
      currentChanges = futureChanges;
      futureChanges = new SnapshotChanges(terminal.getHeight());
      futureChanges.setInterestWindow(interestWindowStartLine, interestWindowSize);
      // and update the snapshot
      if (snapshot.getHeight() != terminal.getHeight() || snapshot.getWidth() != terminal.getWidth()) {
        if (interestWindowSize == -1) {
          snapshot.setWindow(0, terminal.getHeight());
        }
        // if the dimensions have changed, we need a full copy.
        snapshot.copy(terminal);
        // and we mark all lines as changed.
        currentChanges.setAllChanged(terminal.getHeight());
      } else {
        // first we do the scroll on the copy
        int start = currentChanges.getScrollWindowStartLine();
        int lines = Math.min(currentChanges.getScrollWindowSize(), snapshot.getHeight() - start);
        snapshot.scroll(start, lines, currentChanges.getScrollWindowShift());
        // and then create the snapshot of the changed lines.
        currentChanges.copyChangedLines(snapshot, terminal);
      }
      listenersNeedNotify = true;
      snapshot.setCursorLine(terminal.getCursorLine());
      snapshot.setCursorColumn(terminal.getCursorColumn());
    }
    if (!detectScrolling) {
      // let's pretend there was no scrolling and convert the scrolling into line changes
      currentChanges.convertScrollingIntoChanges();
    }
  }

  @Override public char getChar(int line, int column) {
    return snapshot.getChar(line, column);
  }

  @Override public int getHeight() {
    return snapshot.getHeight();
  }

  @Override public LineSegment[] getLineSegments(int line, int column, int len) {
    return snapshot.getLineSegments(line, column, len);
  }

  @Override public Style getStyle(int line, int column) {
    return snapshot.getStyle(line, column);
  }

  @Override public int getWidth() {
    return snapshot.getWidth();
  }

  @Override public int getFirstChangedLine() {
    return currentChanges.getFirstChangedLine();
  }

  @Override public int getLastChangedLine() {
    return currentChanges.getLastChangedLine();
  }

  @Override public boolean hasLineChanged(int line) {
    return currentChanges.hasLineChanged(line);
  }

  @Override public boolean hasDimensionsChanged() {
    return currentChanges.hasDimensionsChanged();
  }

  @Override public boolean hasTerminalChanged() {
    return currentChanges.hasTerminalChanged();
  }

  @Override public int getScrollWindowStartLine() {
    return currentChanges.getScrollWindowStartLine();
  }

  @Override public int getScrollWindowSize() {
    return currentChanges.getScrollWindowSize();
  }

  @Override public int getScrollWindowShift() {
    return currentChanges.getScrollWindowShift();
  }

  void markLineChanged(int line) {
    futureChanges.markLineChanged(line);
    futureChanges.setTerminalChanged();
    notifyListers();
  }

  void markLinesChanged(int line, int size) {
    futureChanges.markLinesChanged(line, size);
    futureChanges.setTerminalChanged();
    notifyListers();
  }

  void markDimensionsChanged() {
    futureChanges.markDimensionsChanged();
    futureChanges.setTerminalChanged();
    notifyListers();
  }

  void markCursorChanged() {
    futureChanges.markCursorChanged();
    futureChanges.setTerminalChanged();
    notifyListers();
  }

  void scroll(int startLine, int size, int shift) {
    futureChanges.scroll(startLine, size, shift);
    futureChanges.setTerminalChanged();
    notifyListers();
  }

  private void notifyListers() {
    synchronized (terminal) {
      if (listenersNeedNotify) {
        for (SnapshotOutOfDateListener listener : listeners) {
          listener.snapshotOutOfDate(this);
        }
        listenersNeedNotify = false;
      }
    }
  }

  @Override public ITerminalTextDataSnapshot makeSnapshot() {
    return snapshot.makeSnapshot();
  }

  @Override synchronized public void addListener(SnapshotOutOfDateListener listener) {
    listeners.add(listener);
  }

  @Override synchronized public void removeListener(SnapshotOutOfDateListener listener) {
    listeners.remove(listener);
  }

  @Override public String toString() {
    return snapshot.toString();
  }

  @Override public int getInterestWindowSize() {
    return interestWindowSize;
  }

  @Override public int getInterestWindowStartLine() {
    return interestWindowStartLine;
  }

  @Override public void setInterestWindow(int startLine, int size) {
    Assert.isTrue(startLine >= 0 && size >= 0);
    interestWindowStartLine = startLine;
    interestWindowSize = size;
    snapshot.setWindow(startLine, size);
    futureChanges.setInterestWindow(startLine, size);
    notifyListers();
  }

  @Override public char[] getChars(int line) {
    return snapshot.getChars(line);
  }

  @Override public Style[] getStyles(int line) {
    return snapshot.getStyles(line);
  }

  @Override public int getCursorColumn() {
    return snapshot.getCursorColumn();
  }

  @Override public int getCursorLine() {
    return snapshot.getCursorLine();
  }

  @Override public ITerminalTextData getTerminalTextData() {
    return terminal;
  }
}
