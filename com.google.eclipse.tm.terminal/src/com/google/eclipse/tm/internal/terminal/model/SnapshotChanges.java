/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.model;

import org.eclipse.core.runtime.Assert;

import com.google.eclipse.tm.terminal.model.ITerminalTextData;

/**
 * Collects the changes of the {@link ITerminalTextData}.
 */
public class SnapshotChanges implements ISnapshotChanges {
  private int firstChangedLine;
  private int lastChangedLine;
  private int scrollWindowStartLine;
  private int scrollWindowSize;
  private int scrollWindowShift;
  private boolean dontTrackScrolling;
  private boolean[] changedLines;
  private int interestWindowSize;
  private int interestWindowStartLine;
  private boolean fDimensionsChanged;
  private boolean fTerminalHasChanged;
  private boolean cursorHasChanged;

  public SnapshotChanges(int windowStart, int windowSize) {
    this(windowStart + windowSize);
    interestWindowStartLine = windowStart;
    interestWindowSize = windowSize;
  }

  public SnapshotChanges(int changedLineCount) {
    setChangedLineCount(changedLineCount);
    firstChangedLine = Integer.MAX_VALUE;
    lastChangedLine = -1;
  }

  // Indicates whether the range overlaps with the interest window.
  private boolean isInInterestWindow(int line, int size) {
    if (interestWindowSize <= 0) {
      return true;
    }
    if (line + size <= interestWindowStartLine || line >= interestWindowStartLine + interestWindowSize) {
      return false;
    }
    return true;
  }

  private boolean isInInterestWindow(int line) {
    if (interestWindowSize <= 0) {
      return true;
    }
    if (line < interestWindowStartLine || line >= interestWindowStartLine + interestWindowSize) {
      return false;
    }
    return true;
  }

  private int fitLineToWindow(int line) {
    if (interestWindowSize <= 0) {
      return line;
    }
    if (line < interestWindowStartLine) {
      return interestWindowStartLine;
    }
    return line;
  }

  /**
   * The result is only defined if {@link #isInInterestWindow(int, int)} returns {@code true}.
   * <p>
   * Note:{@link #fitLineToWindow(int)} has to be called on the line to move the window correctly.
   * </p>
   * @param line the line <b>before</b> {@link #fitLineToWindow(int)} has been called.
   * @param size
   * @return the adjusted size.
   */
  private int fitSizeToWindow(int line, int size) {
    if (interestWindowSize <= 0) {
      return size;
    }
    if (line < interestWindowStartLine) {
      size -= interestWindowStartLine - line;
      line = interestWindowStartLine;
    }
    if (line + size > interestWindowStartLine + interestWindowSize) {
      size = interestWindowStartLine + interestWindowSize - line;
    }
    return size;
  }

  @Override public void markLineChanged(int line) {
    if (!isInInterestWindow(line)) {
      return;
    }
    line = fitLineToWindow(line);
    if (line < firstChangedLine) {
      firstChangedLine = line;
    }
    if (line > lastChangedLine) {
      lastChangedLine = line;
    }
    // In case the terminal got resized we expand don't remember the changed line because there is nothing to copy.
    if (line < getChangedLineCount()) {
      setChangedLine(line, true);
    }
  }

  @Override public void markLinesChanged(int line, int size) {
    if (size <= 0 || !isInInterestWindow(line, size)) {
      return;
    }
    // Do not exceed the bounds of changedLines. The terminal might have been resized and we can only keep changes for
    // the size of the previous terminal.
    size = fitSizeToWindow(line, size);
    line = fitLineToWindow(line);
    int m = Math.min(line + size, getChangedLineCount());
    for (int i = line; i < m; i++) {
      setChangedLine(i, true);
    }
    // this sets firstChangedLine as well
    markLineChanged(line);
    // this sets lastChangedLine as well
    markLineChanged(line + size - 1);
  }

  @Override public void markCursorChanged() {
    cursorHasChanged = true;
  }

  @Override public void convertScrollingIntoChanges() {
    markLinesChanged(scrollWindowStartLine, scrollWindowSize);
    scrollWindowStartLine = 0;
    scrollWindowSize = 0;
    scrollWindowShift = 0;
  }

  @Override public boolean hasChanged() {
    if (firstChangedLine != Integer.MAX_VALUE || lastChangedLine > 0 || scrollWindowShift != 0 || fDimensionsChanged
        || cursorHasChanged) {
      return true;
    }
    return false;
  }

  @Override public void markDimensionsChanged() {
    fDimensionsChanged = true;
  }

  @Override public boolean hasDimensionsChanged() {
    return fDimensionsChanged;
  }

  @Override public boolean hasTerminalChanged() {
    return fTerminalHasChanged;
  }

  @Override public void setTerminalChanged() {
    fTerminalHasChanged = true;
  }

  @Override public void scroll(int startLine, int size, int shift) {
    size = fitSizeToWindow(startLine, size);
    startLine = fitLineToWindow(startLine);
    // Let's track only negative shifts.
    if (dontTrackScrolling) {
      // We are in a state where we cannot track scrolling so let's simply mark the scrolled lines as changed.
      markLinesChanged(startLine, size);
    } else if (shift >= 0) {
      // We cannot handle positive scroll forget about clever caching of scroll events.
      doNotTrackScrollingAnymore();
      // Mark all lines inside the scroll region as changed.
      markLinesChanged(startLine, size);
    } else {
      // We have already scrolled.
      if (scrollWindowShift < 0) {
        // We have already scrolled.
        if (scrollWindowStartLine == startLine && scrollWindowSize == size) {
          // We are scrolling the same region again?
          scrollWindowShift += shift;
          scrollChangesLinesWithNegativeShift(startLine, size, shift);
        } else {
          // Mark all lines in the old scroll region as changed.
          doNotTrackScrollingAnymore();
          markLinesChanged(startLine, size);
        }
      } else {
        // First scroll in this change -- we just notify it
        scrollWindowStartLine = startLine;
        scrollWindowSize = size;
        scrollWindowShift = shift;
        scrollChangesLinesWithNegativeShift(startLine, size, shift);
      }
    }
  }

  // Some incompatible scrolling occurred. We cannot do the scroll optimization anymore.
  private void doNotTrackScrollingAnymore() {
    if (scrollWindowSize > 0) {
      // Convert the current scrolling into changes.
      markLinesChanged(scrollWindowStartLine, scrollWindowSize);
      scrollWindowStartLine = 0;
      scrollWindowSize = 0;
      scrollWindowShift = 0;
    }
    // Don't be clever on scrolling anymore.
    dontTrackScrolling = true;
  }

  private void scrollChangesLinesWithNegativeShift(int line, int n, int shift) {
    Assert.isTrue(shift < 0);
    // Scroll the region. Don't run out of bounds!
    int m = Math.min(line + n + shift, getChangedLineCount() + shift);
    for (int i = line; i < m; i++) {
      setChangedLine(i, hasLineChanged(i - shift));
      // Move the first changed line up. We don't have to move the maximum down, because with a shift scroll, the max is
      // moved by the next loop in this method
      if (i < firstChangedLine && hasLineChanged(i)) {
        firstChangedLine = i;
      }
    }
    // Mark the "opened" lines as changed.
    for (int i = Math.max(0, line + n + shift); i < line + n; i++) {
      markLineChanged(i);
    }
  }

  @Override public void setAllChanged(int height) {
    scrollWindowStartLine = 0;
    scrollWindowSize = 0;
    scrollWindowShift = 0;
    firstChangedLine = fitLineToWindow(0);
    lastChangedLine = firstChangedLine + fitSizeToWindow(0, height) - 1;
    // No need to keep an array of changes anymore.
    setChangedLineCount(0);
  }

  @Override public int getFirstChangedLine() {
    return firstChangedLine;
  }

  @Override public int getLastChangedLine() {
    return lastChangedLine;
  }

  @Override public int getScrollWindowStartLine() {
    return scrollWindowStartLine;
  }

  @Override public int getScrollWindowSize() {
    return scrollWindowSize;
  }

  @Override public int getScrollWindowShift() {
    return scrollWindowShift;
  }

  @Override public void copyChangedLines(ITerminalTextData destination, ITerminalTextData source) {
    int n = Math.min(lastChangedLine + 1, source.getHeight());
    for (int i = firstChangedLine; i < n; i++) {
      if (hasLineChanged(i)) {
        destination.copyLine(source, i, i);
      }
    }
  }

  @Override public int getInterestWindowSize() {
    return interestWindowSize;
  }

  @Override public int getInterestWindowStartLine() {
    return interestWindowStartLine;
  }

  @Override public void setInterestWindow(int startLine, int size) {
    int oldStartLine = interestWindowStartLine;
    int oldSize = interestWindowSize;
    interestWindowStartLine = startLine;
    interestWindowSize = size;
    if (oldSize > 0) {
      int shift = oldStartLine - startLine;
      if (shift == 0) {
        if (size > oldSize) {
          // add lines to the end
          markLinesChanged(oldStartLine + oldSize, size - oldSize);
        }
        // else no lines within the window have changed

      } else if (Math.abs(shift) < size) {
        if (shift < 0) {
          // we can scroll
          scroll(startLine, oldSize, shift);
          // mark the lines at the end as new
          for (int i = oldStartLine + oldSize; i < startLine + size; i++) {
            markLineChanged(i);
          }
        } else {
          // we cannot shift positive -- mark all changed
          markLinesChanged(startLine, size);
        }
      } else {
        // no scrolling possible
        markLinesChanged(startLine, size);
      }
    }
  }

  @Override public boolean hasLineChanged(int line) {
    if (line < getChangedLineCount()) {
      return changedLines[line];
    }
    // since the height of the terminal could have changed but we have tracked only changes of the previous terminal
    // height, any line outside the the range of the previous height has changed
    return isInInterestWindow(line);
  }

  private int getChangedLineCount() {
    return changedLines.length;
  }

  private void setChangedLine(int line, boolean changed) {
    changedLines[line] = changed;
  }

  void setChangedLineCount(int length) {
    changedLines = new boolean[length];
  }
}