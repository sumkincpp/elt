/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.textcanvas;

import java.util.*;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Point;

import com.google.eclipse.tm.terminal.model.*;

public abstract class AbstractTextCanvasModel implements ITextCanvasModel {
  private final List<ITextCanvasModelListener> listeners = new ArrayList<ITextCanvasModelListener>();
  private final Point selectionAnchor = new Point(0, 0);

  private final ITerminalTextDataSnapshot snapshot;

  private int cursorLine;
  private int cursorColumn;
  private boolean showCursor;
  private long cursorTime;
  private boolean cursorIsEnabled;
  private int lines;

  private int selectionStartLine = -1;
  private int seletionEndLine;
  private int selectionStartCoumn;
  private int selectionEndColumn;
  private ITerminalTextDataSnapshot selectionSnapshot;
  private String currentSelection = "";
  /**
   * do not update while update is running
   */
  boolean inUpdate;
  private int columns;

  public AbstractTextCanvasModel(ITerminalTextDataSnapshot snapshot) {
    this.snapshot = snapshot;
    lines = this.snapshot.getHeight();
  }

  @Override public void addCellCanvasModelListener(ITextCanvasModelListener listener) {
    listeners.add(listener);
  }

  @Override public void removeCellCanvasModelListener(ITextCanvasModelListener listener) {
    listeners.remove(listener);
  }

  private void fireCellRangeChanged(int x, int y, int width, int height) {
    for (ITextCanvasModelListener listener : listeners) {
      listener.rangeChanged(x, y, width, height);
    }
  }

  private void fireDimensionsChanged(int width, int height) {
    for (ITextCanvasModelListener listener : listeners) {
      listener.dimensionsChanged(width, height);
    }
  }

  private void fireTerminalDataChanged() {
    for (ITextCanvasModelListener listener : listeners) {
      listener.terminalDataChanged();
    }
  }

  @Override public ITerminalTextDataReadOnly getTerminalText() {
    return snapshot;
  }

  protected ITerminalTextDataSnapshot getSnapshot() {
    return snapshot;
  }

  private void updateSnapshot() {
    if (!inUpdate && snapshot.isOutOfDate()) {
      inUpdate = true;
      try {
        snapshot.updateSnapshot(false);
        if (snapshot.hasTerminalChanged()) {
          fireTerminalDataChanged();
        }
        // TODO why does hasDimensionsChanged not work?
        // if (snapshot.hasDimensionsChanged()) fireDimensionsChanged();
        if (lines != snapshot.getHeight() || columns != snapshot.getWidth()) {
          fireDimensionsChanged(snapshot.getWidth(), snapshot.getHeight());
          lines = snapshot.getHeight();
          columns = snapshot.getWidth();
        }
        int y = snapshot.getFirstChangedLine();
        // has any line changed?
        if (y < Integer.MAX_VALUE) {
          int height = snapshot.getLastChangedLine() - y + 1;
          fireCellRangeChanged(0, y, snapshot.getWidth(), height);
        }
      } finally {
        inUpdate = false;
      }
    }
  }

  /**
   * This method must be called from the UI thread.
   */
  public void update() {
    updateSnapshot();
    updateSelection();
    updateCursor();
  }

  @Override public int getCursorColumn() {
    return cursorColumn;
  }

  @Override public int getCursorLine() {
    return cursorLine;
  }

  @Override public boolean isCursorOn() {
    return showCursor && cursorIsEnabled;
  }

  // TODO: should be called regularly to draw an update of the blinking cursor?
  private void updateCursor() {
    if (!cursorIsEnabled) {
      return;
    }
    int cursorLine = getSnapshot().getCursorLine();
    int cursorColumn = getSnapshot().getCursorColumn();
    // If cursor at the end put it to the end of the last line.
    if (cursorLine >= getSnapshot().getHeight()) {
      cursorLine = getSnapshot().getHeight() - 1;
      cursorColumn = getSnapshot().getWidth() - 1;
    }
    // Has the cursor moved?
    if (this.cursorLine != cursorLine || this.cursorColumn != cursorColumn) {
      // Hide the old cursor!
      showCursor = false;
      // Clean the previous cursor. Bug 206363: paint also the char to the left and right of the cursor
      int col = this.cursorColumn;
      int width = 2;
      if (col > 0) {
        col--;
        width++;
      }
      fireCellRangeChanged(col, this.cursorLine, width, 1);
      // The cursor is shown when it moves.
      showCursor = true;
      cursorTime = System.currentTimeMillis();
      this.cursorLine = cursorLine;
      this.cursorColumn = cursorColumn;
      // Draw the new cursor
      fireCellRangeChanged(this.cursorColumn, this.cursorLine, 1, 1);
    } else {
      long time = System.currentTimeMillis();
      // TODO Make the cursor blink time customizable.
      if (time - cursorTime > 500) {
        showCursor = !showCursor;
        cursorTime = time;
        // On some windows machines, there is some leftover when updating the cursor.
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=206363
        int col = this.cursorColumn;
        int width = 2;
        if (col > 0) {
          col--;
          width++;
        }
        fireCellRangeChanged(col, this.cursorLine, width, 1);
      }
    }
  }

  @Override public void setVisibleRectangle(int startLine, int startCol, int height, int width) {
    snapshot.setInterestWindow(Math.max(0, startLine), Math.max(1, height));
    update();
  }

  protected void showCursor(boolean show) {
    showCursor = true;
  }

  @Override public void setCursorEnabled(boolean visible) {
    cursorTime = System.currentTimeMillis();
    showCursor = visible;
    cursorIsEnabled = visible;
    fireCellRangeChanged(cursorColumn, cursorLine, 1, 1);
  }

  @Override public boolean isCursorEnabled() {
    return cursorIsEnabled;
  }

  @Override public Point getSelectionEnd() {
    if (selectionStartLine < 0) {
      return null;
    }
    return new Point(selectionEndColumn, seletionEndLine);
  }

  @Override public Point getSelectionStart() {
    if (selectionStartLine < 0) {
      return null;
    }
    return new Point(selectionStartCoumn, selectionStartLine);
  }

  @Override public Point getSelectionAnchor() {
    if (selectionStartLine < 0) {
      return null;
    }
    return new Point(selectionAnchor.x, selectionAnchor.y);
  }

  @Override public void setSelectionAnchor(Point anchor) {
    selectionAnchor.x = anchor.x;
    selectionAnchor.y = anchor.y;
  }

  @Override public void setSelection(int startLine, int endLine, int startColumn, int endColumn) {
    doSetSelection(startLine, endLine, startColumn, endColumn);
    currentSelection = extractSelectedText();
  }

  private void doSetSelection(int startLine, int endLine, int startColumn, int endColumn) {
    Assert.isTrue(startLine < 0 || startLine <= endLine);
    if (startLine >= 0) {
      if (selectionSnapshot == null) {
        selectionSnapshot = snapshot.getTerminalTextData().makeSnapshot();
        selectionSnapshot.updateSnapshot(true);
      }
    } else if (selectionSnapshot != null) {
      selectionSnapshot.detach();
      selectionSnapshot = null;
    }
    int oldStart = selectionStartLine;
    int oldEnd = seletionEndLine;
    selectionStartLine = startLine;
    seletionEndLine = endLine;
    selectionStartCoumn = startColumn;
    selectionEndColumn = endColumn;
    if (selectionSnapshot != null) {
      selectionSnapshot.setInterestWindow(0, selectionSnapshot.getHeight());
    }
    int changedStart;
    int changedEnd;
    if (oldStart < 0) {
      changedStart = selectionStartLine;
      changedEnd = seletionEndLine;
    } else if (selectionStartLine < 0) {
      changedStart = oldStart;
      changedEnd = oldEnd;
    } else {
      changedStart = Math.min(oldStart, selectionStartLine);
      changedEnd = Math.max(oldEnd, seletionEndLine);
    }
    if (changedStart >= 0) {
      fireCellRangeChanged(0, changedStart, snapshot.getWidth(), changedEnd - changedStart + 1);
    }
  }

  @Override public boolean hasLineSelection(int line) {
    if (selectionStartLine < 0) {
      return false;
    }
    return line >= selectionStartLine && line <= seletionEndLine;
  }

  @Override public String getSelectedText() {
    return currentSelection;
  }

  /**
   * Calculates the currently selected text
   * @return the currently selected text
   */
  private String extractSelectedText() {
    if (selectionStartLine < 0 || selectionStartCoumn < 0 || selectionSnapshot == null) {
      return "";
    }
    StringBuilder buffer = new StringBuilder();
    for (int line = selectionStartLine; line <= seletionEndLine; line++) {
      String text;
      char[] chars = selectionSnapshot.getChars(line);
      if (chars != null) {
        text = new String(chars);
        if (line == seletionEndLine && selectionEndColumn >= 0) {
          text = text.substring(0, Math.min(selectionEndColumn + 1, text.length()));
        }
        if (line == selectionStartLine) {
          text = text.substring(Math.min(selectionStartCoumn, text.length()));
        }
        // get rid of the empty space at the end of the lines
        text = text.replaceAll("\000+$","");
        // null means space
        text = text.replace('\000', ' ');
      } else {
        text = "";
      }
      buffer.append(text);
      if (line < seletionEndLine) {
        buffer.append('\n');
      }
    }
    return buffer.toString();
  }

  private void updateSelection() {
    if (selectionSnapshot != null && selectionSnapshot.isOutOfDate()) {
      selectionSnapshot.updateSnapshot(true);
      // Has the selection moved?
      if (selectionSnapshot != null && selectionStartLine >= 0 && selectionSnapshot.getScrollWindowSize() > 0) {
        int start = selectionStartLine + selectionSnapshot.getScrollWindowShift();
        int end = seletionEndLine + selectionSnapshot.getScrollWindowShift();
        if (start < 0) {
          if (end >= 0) {
            start = 0;
          } else {
            start = -1;
          }
        }
        doSetSelection(start, end, selectionStartCoumn, selectionEndColumn);
      }
      // Check if the content of the selection has changed. If the content has changed, clear the selection.
      if (currentSelection.length() > 0 && selectionSnapshot != null
          && selectionSnapshot.getFirstChangedLine() <= seletionEndLine
          && selectionSnapshot.getLastChangedLine() >= selectionStartLine) {
        // Has the selected text changed?
        if (!currentSelection.equals(extractSelectedText())) {
          setSelection(-1, -1, -1, -1);
        }
      }
      // Update the observed window...
      if (selectionSnapshot != null) {
        // TODO make -1 to work!
        selectionSnapshot.setInterestWindow(0, selectionSnapshot.getHeight());
      }
    }
  }
}