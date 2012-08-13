/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.textcanvas;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import com.google.eclipse.elt.emulator.model.*;

public class TextLineRenderer implements ILineRenderer {
  private final ITextCanvasModel model;
  private final StyleMap styleMap = new StyleMap();

  public TextLineRenderer(TextCanvas c, ITextCanvasModel model) {
    this.model = model;
  }

  @Override public int getCellWidth() {
    return styleMap.getFontWidth();
  }

  @Override public int getCellHeight() {
    return styleMap.getFontHeight();
  }

  @Override public void drawLine(
      ITextCanvasModel model, GC gc, int line, int x, int y, int firstColumn, int lastColumn) {
    if (line < 0 || line >= getTerminalText().getHeight() || firstColumn >= getTerminalText().getWidth()
        || firstColumn - lastColumn == 0) {
      fillBackground(gc, x, y, getCellWidth() * (lastColumn - firstColumn), getCellHeight());
      return;
    }
    lastColumn = Math.min(lastColumn, getTerminalText().getWidth());
    LineSegment[] segments = getTerminalText().getLineSegments(line, firstColumn, lastColumn - firstColumn);
    for (int i = 0; i < segments.length; i++) {
      LineSegment segment = segments[i];
      Style style = segment.getStyle();
      setupGC(gc, style);
      String text = segment.getText();
      drawText(gc, x, y, firstColumn, segment.getColumn(), text);
      drawCursor(model, gc, line, x, y, firstColumn);
    }
    if (this.model.hasLineSelection(line)) {
      Display display = Display.getCurrent();
      gc.setForeground(display.getSystemColor(COLOR_LIST_SELECTION_TEXT));
      gc.setBackground(display.getSystemColor(COLOR_LIST_SELECTION));
      Point start = model.getSelectionStart();
      Point end = model.getSelectionEnd();
      char[] chars = model.getTerminalText().getChars(line);
      if (chars == null) {
        return;
      }
      int offset = 0;
      if (start.y == line) {
        offset = start.x;
      }
      offset = Math.max(offset, firstColumn);
      int len;
      if (end.y == line) {
        len = end.x - offset + 1;
      } else {
        len = chars.length - offset + 1;
      }
      len = Math.min(len, chars.length - offset);
      if (len > 0) {
        String text = new String(chars, offset, len);
        drawText(gc, x, y, firstColumn, offset, text);
      }
    }
  }

  private void fillBackground(GC gc, int x, int y, int width, int height) {
    Color bg = gc.getBackground();
    gc.setBackground(getDefaultBackgroundColor());
    gc.fillRectangle(x, y, width, height);
    gc.setBackground(bg);
  }

  @Override public Color getDefaultBackgroundColor() {
    // null == default style
    return styleMap.getBackgroundColor(null);
  }

  private void drawCursor(ITextCanvasModel model, GC gc, int row, int x, int y, int colFirst) {
    if (!model.isCursorOn()) {
      return;
    }
    int cursorLine = model.getCursorLine();
    if (row == cursorLine) {
      int cursorColumn = model.getCursorColumn();
      if (cursorColumn < getTerminalText().getWidth()) {
        Style style = getTerminalText().getStyle(row, cursorColumn);
        if (style != null) {
          style = style.setReverse(!style.isReverse());
          setupGC(gc, style);
        } else {
          setBackground(gc, styleMap.getForegroundColor(null));
          setForeground(gc, styleMap.getBackgroundColor(null));
        }
        String text = String.valueOf(getTerminalText().getChar(row, cursorColumn));
        drawText(gc, x, y, colFirst, cursorColumn, text);
      }
    }
  }

  private void drawText(GC gc, int x, int y, int colFirst, int col, String text) {
    int offset = (col - colFirst) * getCellWidth();
    if (styleMap.isFontProportional()) {
      // Draw the background.
      // TODO why does this not work?
      // gc.fillRectangle(x, y, styleMap.getFontWidth() * text.length(), styleMap.getFontHeight());
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        int fontWidth = styleMap.getFontWidth();
        int newX = x + offset + i * fontWidth;
        // TODO why do I have to draw the background character by character?
        gc.fillRectangle(newX, y, fontWidth, styleMap.getFontHeight());
        if (c != ' ' && c != '\000') {
          gc.drawString(String.valueOf(c), styleMap.getCharOffset(c) + newX, y, true);
        }
      }
    } else {
      text = text.replace('\000', ' ');
      gc.drawString(text, x + offset, y, false);
    }
  }

  private void setupGC(GC gc, Style style) {
    setForeground(gc, styleMap.getForegroundColor(style));
    setBackground(gc, styleMap.getBackgroundColor(style));
    Font font = styleMap.getFont(style);
    if (font != gc.getFont()) {
      gc.setFont(font);
    }
  }

  private void setForeground(GC gc, Color color) {
    if (color != gc.getForeground()) {
      gc.setForeground(color);
    }
  }

  private void setBackground(GC gc, Color color) {
    if (color != gc.getBackground()) {
      gc.setBackground(color);
    }
  }

  ITerminalTextDataReadOnly getTerminalText() {
    return model.getTerminalText();
  }

  @Override public void onFontChange() {
    styleMap.updateFont();
  }

  @Override public void setInvertedColors(boolean invert) {
    styleMap.setInvertedColors(invert);

  }

  @Override public void setColors(RGB background, RGB foreground) {
    styleMap.setColors(background, foreground);
  }

  @Override public void setFont(Font font) {
    styleMap.setFont(font);
  }
}
