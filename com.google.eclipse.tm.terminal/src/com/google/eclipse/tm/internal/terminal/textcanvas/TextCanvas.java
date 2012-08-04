/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.textcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;

/**
 * A cell oriented Canvas. Maintains a list of "cells". It can either be vertically or horizontally scrolled. The
 * CellRenderer is responsible for painting the cell.
 */
public class TextCanvas extends GridCanvas {
  protected final ITextCanvasModel cellCanvasModel;
  private final ILinelRenderer cellRenderer;
  private boolean scrollLockOn;
  private Point draggingStart;
  private Point draggingEnd;
  private boolean hasSelection;
  private ResizeListener resizeListener;

  // The minSize is meant to determine the minimum size of the backing store (grid) into which remote data is rendered.
  // If the viewport is smaller than that minimum size, the backing store size remains at the minSize,and a scrollbar is
  // shown instead. In reality, this has the following issues or effects today:
  //
  // (a) Bug 281328: For very early data coming in before the widget is realized, the minSize determines into what
  // initial grid that is rendered. See also {@link #addResizeHandler(ResizeListener)}.
  //
  // (b) Bug 294468: Since we have redraw and size computation problems with horizontal scrollers, for now the
  // minColumns must be small enough to avoid a horizontal scroller appearing in most cases.
  //
  // (c) Bug 294327: Since we have problems with the vertical scroller showing the correct location, minLines must be
  // small enough to avoid a vertical scroller or new data may be rendered off-screen.
  //
  // As a compromise, we have been working with a 20x4 since the terminal inception, though many users would want a
  // 80x24 minSize and backing store.

  // Pros and cons of the small minsize:
  // + consistent "remote size==viewport size", vi works as expected
  // - dumb terminals which expect 80x24 render garbled on small viewport
  //
  // If bug 294468 were resolved, an 80 wide minSize would be preferrable since it allows switching the terminal
  // viewport small/large as needed, without destroying the backing store. For a complete solution, bug 196462 tracks
  // the request for a user-defined fixed-widow-size-mode.
  private int minColumns = 80;

  private int minLines = 4;
  private boolean cursorEnabled;
  private boolean resizing;

  /**
   * Create a new CellCanvas with the given SWT style bits. (SWT.H_SCROLL and SWT.V_SCROLL are automatically added).
   */
  public TextCanvas(Composite parent, ITextCanvasModel model, int style, ILinelRenderer cellRenderer) {
    super(parent, style | SWT.H_SCROLL | SWT.V_SCROLL);
    this.cellRenderer = cellRenderer;
    setCellWidth(cellRenderer.getCellWidth());
    setCellHeight(cellRenderer.getCellHeight());
    cellCanvasModel = model;
    cellCanvasModel.addCellCanvasModelListener(new ITextCanvasModelListener() {
      @Override public void rangeChanged(int col, int line, int width, int height) {
        repaintRange(col, line, width, height);
      }

      @Override public void dimensionsChanged(int cols, int rows) {
        calculateGrid();
      }

      @Override public void terminalDataChanged() {
        if (!isDisposed() && !resizing) {
          // scroll to end (unless scroll lock is active)
          calculateGrid();
          scrollToEnd();
        }
      }
    });
    // let the cursor blink if the text canvas gets the focus...
    addFocusListener(new FocusListener() {
      @Override public void focusGained(FocusEvent e) {
        cellCanvasModel.setCursorEnabled(cursorEnabled);
      }

      @Override public void focusLost(FocusEvent e) {
        cellCanvasModel.setCursorEnabled(false);
      }
    });
    addMouseListener(new MouseAdapter() {
      @Override public void mouseDown(MouseEvent e) {
        if (e.button == 1) { // left button
          draggingStart = screenPointToCell(e.x, e.y);
          hasSelection = false;
          if ((e.stateMask & SWT.SHIFT) != 0) {
            Point anchor = cellCanvasModel.getSelectionAnchor();
            if (anchor != null) {
              draggingStart = anchor;
            }
          } else {
            cellCanvasModel.setSelectionAnchor(draggingStart);
          }
          draggingEnd = null;
        }
      }

      @Override public void mouseUp(MouseEvent e) {
        if (e.button == 1) { // left button
          updateHasSelection(e);
          if (hasSelection) {
            setSelection(screenPointToCell(e.x, e.y));
          } else {
            cellCanvasModel.setSelection(-1, -1, -1, -1);
          }
          draggingStart = null;
        }
      }
    });
    addMouseMoveListener(new MouseMoveListener() {
      @Override public void mouseMove(MouseEvent e) {
        if (draggingStart != null) {
          updateHasSelection(e);
          setSelection(screenPointToCell(e.x, e.y));
        }
      }
    });
    serVerticalBarVisible(true);
    setHorizontalBarVisible(false);
  }

  // The user has to drag the mouse to at least one character to make a selection. Once this is done, even a one char
  // selection is OK.
  private void updateHasSelection(MouseEvent e) {
    if (draggingStart != null) {
      Point p = screenPointToCell(e.x, e.y);
      if (draggingStart.x != p.x || draggingStart.y != p.y) {
        hasSelection = true;
      }
    }
  }

  void setSelection(Point p) {
    if (draggingStart != null && !p.equals(draggingEnd)) {
      draggingEnd = p;
      if (compare(p, draggingStart) < 0) {
        // bug 219589 - make sure selection start coordinates are non-negative
        int startColumn = Math.max(0, p.x);
        int startRow = Math.max(p.y, 0);
        cellCanvasModel.setSelection(startRow, draggingStart.y, startColumn, draggingStart.x);
      } else {
        cellCanvasModel.setSelection(draggingStart.y, p.y, draggingStart.x, p.x);
      }
    }
  }

  int compare(Point p1, Point p2) {
    if (p1.equals(p2)) {
      return 0;
    }
    if (p1.y == p2.y) {
      return p1.x > p2.x ? 1 : -1;
    }
    return p1.y > p2.y ? 1 : -1;
  }

  public ILinelRenderer getCellRenderer() {
    return cellRenderer;
  }

  public int getMinColumns() {
    return minColumns;
  }

  public void setMinColumns(int minColumns) {
    this.minColumns = minColumns;
  }

  public int getMinLines() {
    return minLines;
  }

  public void setMinLines(int minLines) {
    this.minLines = minLines;
  }

  protected void onResize(boolean init) {
    if (resizeListener != null) {
      Rectangle bonds = getClientArea();
      int cellHeight = getCellHeight();
      int cellWidth = getCellWidth();
      int lines = bonds.height / cellHeight;
      int columns = bonds.width / cellWidth;
      // When the view is minimized, its size is set to 0 we don't sent this to the terminal!
      if ((lines > 0 && columns > 0) || init) {
        if (columns < minColumns) {
          if (!isHorizontalBarVisble()) {
            setHorizontalBarVisible(true);
            bonds = getClientArea();
            lines = bonds.height / cellHeight;
          }
          columns = minColumns;
        } else if (columns >= minColumns && isHorizontalBarVisble()) {
          setHorizontalBarVisible(false);
          bonds = getClientArea();
          lines = bonds.height / cellHeight;
          columns = bonds.width / cellWidth;
        }
        if (lines < minLines) {
          lines = minLines;
        }
        resizeListener.sizeChanged(lines, columns);
      }
    }
    super.onResize();
    calculateGrid();
  }

  @Override protected void onResize() {
    resizing = true;
    try {
      onResize(false);
    } finally {
      resizing = false;
    }
  }

  private void calculateGrid() {
    Rectangle virtualBounds = getVirtualBounds();
    setRedraw(false);
    try {
      setVirtualExtend(getCols() * getCellWidth(), getRows() * getCellHeight());
      getParent().layout();
      if (resizing) {
        // scroll to end if view port was near last line
        Rectangle viewRect = getViewRectangle();
        if (virtualBounds.height - (viewRect.y + viewRect.height) < getCellHeight() * 2) {
          scrollToEnd();
        }
      }
    } finally {
      setRedraw(true);
    }
  }

  void scrollToEnd() {
    if (!scrollLockOn) {
      int y = -(getRows() * getCellHeight() - getClientArea().height);
      if (y > 0) {
        y = 0;
      }
      Rectangle v = getViewRectangle();
      if (v.y != -y) {
        setVirtualOrigin(v.x, y);
      }
      // make sure the scroll area is correct.
      scrollY(getVerticalBar());
      scrollX(getHorizontalBar());
    }
  }

  public boolean isScrollLockOn() {
    return scrollLockOn;
  }

  public void setScrollLockOn(boolean on) {
    scrollLockOn = on;
  }

  protected void repaintRange(int col, int line, int width, int height) {
    Point origin = cellToOriginOnScreen(col, line);
    Rectangle r = new Rectangle(origin.x, origin.y, width * getCellWidth(), height * getCellHeight());
    repaint(r);
  }

  @Override protected void drawLine(GC gc, int line, int x, int y, int colFirst, int colLast) {
    cellRenderer.drawLine(cellCanvasModel, gc, line, x, y, colFirst, colLast);
  }

  @Override protected Color getTerminalBackgroundColor() {
    return cellRenderer.getDefaultBackgroundColor();
  }

  @Override protected void visibleCellRectangleChanged(int x, int y, int width, int height) {
    cellCanvasModel.setVisibleRectangle(y, x, height, width);
    update();
  }

  @Override protected int getCols() {
    return cellCanvasModel.getTerminalText().getWidth();
  }

  @Override protected int getRows() {
    return cellCanvasModel.getTerminalText().getHeight();
  }

  public String getSelectionText() {
    // TODO -- create a hasSelectionMethod!
    return cellCanvasModel.getSelectedText();
  }

  public void copy() {
    Clipboard clipboard = new Clipboard(getDisplay());
    clipboard.setContents(new Object[] { getSelectionText() }, new Transfer[] { TextTransfer.getInstance() });
    clipboard.dispose();
  }

  public void selectAll() {
    cellCanvasModel.setSelection(
        0, cellCanvasModel.getTerminalText().getHeight(), 0, cellCanvasModel.getTerminalText().getWidth());
    cellCanvasModel.setSelectionAnchor(new Point(0, 0));
  }

  public boolean isEmpty() {
    return false;
  }

  /**
   * Gets notified when the visible size of the terminal changes. This should update the model!
   */
  public static interface ResizeListener {
    void sizeChanged(int lines, int columns);
  }

  public void addResizeHandler(ResizeListener listener) {
    if (resizeListener != null) {
      throw new IllegalArgumentException("There can be at most one listener at the moment!");
    }
    resizeListener = listener;
    // Bug 281328: The very first few characters might be missing in the terminal control if opened and connected
    // programmatically.
    //
    // In case the terminal had not been visible yet or is too small (less than one line visible), the terminal should
    // have a minimum size to avoid RuntimeExceptions.
    Rectangle bonds = getClientArea();
    if (bonds.height < getCellHeight() || bonds.width < getCellWidth()) {
      // Widget not realized yet, or minimized to < 1 item. Just tell the listener our min size.
      resizeListener.sizeChanged(getMinLines(), getMinColumns());
    } else {
      // Widget realized: compute actual size and force telling the listener
      onResize(true);
    }
  }

  public void onFontChange() {
    cellRenderer.onFontChange();
    setCellWidth(cellRenderer.getCellWidth());
    setCellHeight(cellRenderer.getCellHeight());
    calculateGrid();
  }

  public void setInvertedColors(boolean invert) {
    cellRenderer.setInvertedColors(invert);
    redraw();
  }

  /**
   * Indicates whether the cursor is enabled (blinking.) By default the cursor is not enabled.
   *
   * @return {@code true} if the cursor is enabled, {@code false} otherwise.
   */
  public boolean isCursorEnabled() {
    return cursorEnabled;
  }

  /**
   * Enables or disables the cursor (enabling means that the cursor blinks.)
   *
   * @param enabled indicates whether the cursor should be enabled.
   */
  public void setCursorEnabled(boolean enabled) {
    if (enabled != cursorEnabled) {
      cursorEnabled = enabled;
      cellCanvasModel.setCursorEnabled(cursorEnabled);
    }
  }

  public void setColors(RGB background, RGB foreground) {
    cellRenderer.setColors(background, foreground);
    redraw();
  }

  @Override public void setFont(Font font) {
    super.setFont(font);
    cellRenderer.setFont(font);
    redraw();
  }

  @Override public Point screenPointToCell(int x, int y) {
    return super.screenPointToCell(x, y);
  }
}
