/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.core;

import static java.util.Collections.emptyList;

import java.util.*;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.google.eclipse.elt.emulator.hyperlink.*;
import com.google.eclipse.elt.emulator.model.*;

public class VT100EmulatorBackend implements IVT100EmulatorBackend {
  // This field holds the number of the column in which the cursor is logically positioned. The left-most column on the
  // screen is column 0, and column numbers increase to the right. The maximum value of this field is
  // widthInColumns - 1. We track the cursor column using this field to avoid having to recompute it repeatly using
  // StyledText method calls.
  //
  // The StyledText widget that displays text has a vertical bar (called the "caret") that appears _between_ character
  // cells, but ANSI terminals have the concept of a cursor that appears _in_ a character cell, so we need a convention
  // for which character cell the cursor logically occupies when the caret is physically between two cells. The
  // convention used in this class is that the cursor is logically in column N when the caret is physically positioned
  // immediately to the _left_ of column N.
  //
  // When cursorColumn is N, the next character output to the terminal appears in column N. When a character is output
  // to the rightmost column on a given line (column widthInColumns - 1), the cursor moves to column 0 on the next line
  // after the character is drawn (this is how line wrapping is implemented). If the cursor is in the bottom-most line
  // when line wrapping occurs, the topmost visible line is scrolled off the top edge of the screen.
  private int cursorColumn;

  private int cursorLine;
  private Style style;
  private int lines;
  private int columns;

  private final ITerminalTextData terminal;

  private final IHyperlinkFactory httpHyperlinkFactory = new HttpHyperlinkFactory();
  private final Map<Integer, List<IHyperlink>> hyperlinks = new HashMap<Integer, List<IHyperlink>>();

  public VT100EmulatorBackend(ITerminalTextData terminal) {
    this.terminal = terminal;
  }

  @Override public void clearAll() {
    synchronized (terminal) {
      // clear the history
      int n = terminal.getHeight();
      for (int line = 0; line < n; line++) {
        terminal.cleanLine(line);
      }
      terminal.setDimensions(lines, terminal.getWidth());
      setStyle(null);
      setCursor(0, 0);
    }
  }

  @Override public void setDimensions(int lines, int columns) {
    synchronized (terminal) {
      if (lines == this.lines && columns == this.columns) {
        return; // nothing to do
      }
      // relative cursor line
      int cursorLine = getCursorLine();
      int cursorColumn = getCursorColumn();
      int height = terminal.getHeight();
      // absolute cursor line
      int absoluteCursorLine = cursorLine + height - this.lines;
      int newLines = Math.max(lines, height);
      if (lines < this.lines) {
        if (height == this.lines) {
          // if the terminal has no history, then resize by setting the size to the new size.
          // TODO We are assuming that cursor line points at end of text.
          newLines = Math.max(lines, cursorLine + 1);
        }
      }
      this.lines = lines;
      this.columns = columns;
      // Make the terminal at least as high as we need lines.
      terminal.setDimensions(newLines, this.columns);
      // Compute relative cursor line.
      cursorLine = absoluteCursorLine - (newLines - this.lines);
      setCursor(cursorLine, cursorColumn);
    }
  }

  int toAbsoluteLine(int line) {
    synchronized (terminal) {
      return terminal.getHeight() - this.lines + line;
    }
  }

  @Override public void insertCharacters(int charactersToInsert) {
    synchronized (terminal) {
      int line = toAbsoluteLine(cursorLine);
      int n = charactersToInsert;
      for (int column = columns - 1; column >= cursorColumn + n; column--) {
        char c = terminal.getChar(line, column - n);
        Style style = terminal.getStyle(line, column - n);
        terminal.setChar(line, column, c, style);
      }
      int last = Math.min(cursorColumn + n, columns);
      for (int col = cursorColumn; col < last; col++) {
        terminal.setChar(line, col, '\000', null);
      }
    }
  }

  @Override public void eraseToEndOfScreen() {
    synchronized (terminal) {
      eraseLineToEnd();
      for (int line = toAbsoluteLine(cursorLine + 1); line < toAbsoluteLine(lines); line++) {
        terminal.cleanLine(line);
      }
    }
  }

  @Override public void eraseToCursor() {
    synchronized (terminal) {
      for (int line = toAbsoluteLine(0); line < toAbsoluteLine(cursorLine); line++) {
        terminal.cleanLine(line);
      }
      eraseLineToCursor();
    }
  }

  @Override public void eraseAll() {
    synchronized (terminal) {
      for (int line = toAbsoluteLine(0); line < toAbsoluteLine(lines); line++) {
        terminal.cleanLine(line);
      }
    }
  }

  @Override public void eraseLine() {
    synchronized (terminal) {
      terminal.cleanLine(toAbsoluteLine(cursorLine));
    }
  }

  @Override public void eraseLineToEnd() {
    synchronized (terminal) {
      int line = toAbsoluteLine(cursorLine);
      for (int col = cursorColumn; col < columns; col++) {
        terminal.setChar(line, col, '\000', null);
      }
    }
  }

  @Override public void eraseLineToCursor() {
    synchronized (terminal) {
      int line = toAbsoluteLine(cursorLine);
      for (int col = 0; col <= cursorColumn; col++) {
        terminal.setChar(line, col, '\000', null);
      }
    }
  }

  @Override public void insertLines(int lineCount) {
    synchronized (terminal) {
      if (!isCursorInScrollingRegion()) {
        return;
      }
      Assert.isTrue(lineCount > 0);
      int line = toAbsoluteLine(cursorLine);
      int nLines = terminal.getHeight() - line;
      terminal.scroll(line, nLines, lineCount);
    }
  }

  @Override public void deleteCharacters(int charCount) {
    synchronized (terminal) {
      int line = toAbsoluteLine(cursorLine);
      for (int col = cursorColumn + charCount; col < columns; col++) {
        char c = terminal.getChar(line, col);
        Style style = terminal.getStyle(line, col);
        terminal.setChar(line, col - charCount, c, style);
      }
      int first = Math.max(cursorColumn, columns - charCount);
      for (int col = first; col < columns; col++) {
        terminal.setChar(line, col, '\000', null);
      }
    }
  }

  @Override public void deleteLines(int lineCount) {
    synchronized (terminal) {
      if (!isCursorInScrollingRegion()) {
        return;
      }
      Assert.isTrue(lineCount > 0);
      int line = toAbsoluteLine(cursorLine);
      int currentLineCount = terminal.getHeight() - line;
      terminal.scroll(line, currentLineCount, -lineCount);
    }
  }

  private boolean isCursorInScrollingRegion() {
    return true;
  }

  @Override public Style getStyle() {
    synchronized (terminal) {
      return style;
    }
  }

  @Override public void setStyle(Style style) {
    synchronized (terminal) {
      this.style = style;
    }
  }

  @Override public void appendString(String buffer) {
    synchronized (terminal) {
      char[] chars = buffer.toCharArray();
      int line = toAbsoluteLine(cursorLine);
      int originalLine = line;
      List<IHyperlink> found = emptyList();
      if (buffer != null) {
        found = httpHyperlinkFactory.hyperlinksIn(cursorColumn, buffer);
        hyperlinks.put(new Integer(line), found);
      }
      int i = 0;
      while (i < chars.length) {
        int n = Math.min(columns - cursorColumn, chars.length - i);
        terminal.setChars(line, cursorColumn, chars, i, n, style);
        int col = cursorColumn + n;
        i += n;
        // wrap needed?
        if (col >= columns) {
          doNewline();
          line = toAbsoluteLine(cursorLine);
          setCursorColumn(0);
        } else {
          setCursorColumn(col);
        }
      }
      drawHyperlinks(found, originalLine);
    }
  }

  private void drawHyperlinks(List<IHyperlink> hyperlinks, int line) {
    for (IHyperlink hyperlink : hyperlinks) {
      IRegion region = hyperlink.getHyperlinkRegion();
      int start = region.getOffset();
      int end = start + region.getLength();
      for (int column = start; column < end; column++) {
        Style style = terminal.getStyle(line, column);
        if (style != null) {
          style = style.setUnderline(true);
          terminal.setChar(line, column, terminal.getChar(line, column), style);
        }
      }
    }
  }

  // MUST be called from a synchronized block!
  private void doNewline() {
    if (cursorLine + 1 >= lines) {
      int h = terminal.getHeight();
      terminal.addLine();
      if (h != terminal.getHeight()) {
        setCursorLine(cursorLine + 1);
      }
    } else {
      setCursorLine(cursorLine + 1);
    }
  }

  @Override public void processNewline() {
    synchronized (terminal) {
      doNewline();
    }
  }

  @Override public int getCursorLine() {
    synchronized (terminal) {
      return cursorLine;
    }
  }

  @Override public int getCursorColumn() {
    synchronized (terminal) {
      return cursorColumn;
    }
  }

  @Override public void setCursor(int targetLine, int targetColumn) {
    synchronized (terminal) {
      setCursorLine(targetLine);
      setCursorColumn(targetColumn);
    }
  }

  @Override public void setCursorColumn(int targetColumn) {
    synchronized (terminal) {
      if (targetColumn < 0) {
        targetColumn = 0;
      } else if (targetColumn >= columns) {
        targetColumn = columns - 1;
      }
      cursorColumn = targetColumn;
      // We make the assumption that nobody is changing the
      // terminal cursor except this class!
      // This assumption gives a huge performance improvement
      terminal.setCursorColumn(targetColumn);
    }
  }

  @Override public void setCursorLine(int targetLine) {
    synchronized (terminal) {
      if (targetLine < 0) {
        targetLine = 0;
      } else if (targetLine >= lines) {
        targetLine = lines - 1;
      }
      cursorLine = targetLine;
      // We make the assumption that nobody is changing the terminal cursor except this class!
      // This assumption gives a huge performance improvement.
      terminal.setCursorLine(toAbsoluteLine(targetLine));
    }
  }

  @Override public int getLines() {
    synchronized (terminal) {
      return lines;
    }
  }

  @Override public int getColumns() {
    synchronized (terminal) {
      return columns;
    }
  }

  @Override public List<IHyperlink> hyperlinksAt(int line) {
    List<IHyperlink> found = hyperlinks.get(new Integer(line));
    if (found == null) {
      return emptyList();
    }
    return found;
  }
}