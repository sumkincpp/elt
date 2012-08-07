/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.textcanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * A Grid based Canvas. The canvas has rows and columns. CellPainting is done with the abstract method drawCell
 */
public abstract class GridCanvas extends VirtualCanvas {
  private int cellWidth;
  private int cellHeight;

  public GridCanvas(Composite parent, int style) {
    super(parent, style);
    addListener(SWT.MouseWheel, new Listener() {
      @Override public void handleEvent(Event event) {
        if (getVerticalBar().isVisible()) {
          int delta = -cellHeight;
          if (event.count < 0) {
            delta = -delta;
          }
          scrollYDelta(delta);
        }
        event.doit = false;
      }
    });
  }

  @Override protected void paint(GC gc) {
    Rectangle clipping = gc.getClipping();
    if (clipping.width == 0 || clipping.height == 0) {
      return;
    }
    Rectangle clientArea = getScreenRectInVirtualSpace();
    // Beginning coordinates.
    int xOffset = clientArea.x;
    int yOffset = clientArea.y;
    int colFirst = virtualXToCell(xOffset + clipping.x);
    if (colFirst > getCols()) {
      colFirst = getCols();
    } else if (colFirst < 0) {
      colFirst = 0;
    }
    int rowFirst = virtualYToCell(yOffset + clipping.y);
    // End coordinates.
    int colLast = virtualXToCell(xOffset + clipping.x + clipping.width + cellWidth);
    if (colLast > getCols()) {
      colLast = getCols();
    }
    int rowLast = virtualYToCell(yOffset + clipping.y + clipping.height + cellHeight);
    if (rowLast > getRows()) {
      rowLast = getRows();
    }
    // Draw the cells.
    for (int row = rowFirst; row <= rowLast; row++) {
      int cx = colFirst * cellWidth - xOffset;
      int cy = row * cellHeight - yOffset;
      drawLine(gc, row, cx, cy, colFirst, colLast);
    }
    paintUnoccupiedSpace(gc, clipping);
  }

  abstract void drawLine(GC gc, int row, int x, int y, int colFirst, int colLast);

  abstract protected int getRows();

  abstract protected int getCols();

  protected void setCellWidth(int cellWidth) {
    this.cellWidth = cellWidth;
    getHorizontalBar().setIncrement(this.cellWidth);
  }

  public int getCellWidth() {
    return cellWidth;
  }

  protected void setCellHeight(int cellHeight) {
    this.cellHeight = cellHeight;
    getVerticalBar().setIncrement(this.cellHeight);
  }

  public int getCellHeight() {
    return cellHeight;
  }

  int virtualXToCell(int x) {
    return x / cellWidth;
  }

  int virtualYToCell(int y) {
    return y / cellHeight;
  }

  protected Point screenPointToCell(int x, int y) {
    x = screenXtoVirtual(x) / cellWidth;
    y = screenYtoVirtual(y) / cellHeight;
    return new Point(x, y);
  }

  Point screenPointToCell(Point point) {
    return screenPointToCell(point.x, point.y);
  }

  protected Point cellToOriginOnScreen(int x, int y) {
    x = virtualXtoScreen(cellWidth * x);
    y = virtualYtoScreen(cellHeight * y);
    return new Point(x, y);
  }

  Point cellToOriginOnScreen(Point cell) {
    return cellToOriginOnScreen(cell.x, cell.y);
  }

  Rectangle getCellScreenRect(Point cell) {
    return getCellScreenRect(cell.x, cell.y);
  }

  Rectangle getCellScreenRect(int x, int y) {
    x = cellWidth * virtualXtoScreen(x);
    y = cellHeight * virtualYtoScreen(y);
    return new Rectangle(x, y, cellWidth, cellHeight);
  }

  protected Rectangle getCellVirtualRect(Point cell) {
    return getCellVirtualRect(cell.x, cell.y);
  }

  Rectangle getCellVirtualRect(int x, int y) {
    x = cellWidth * x;
    y = cellHeight * y;
    return new Rectangle(x, y, cellWidth, cellHeight);
  }

  @Override protected void viewRectangleChanged(int x, int y, int width, int height) {
    int cellX = virtualXToCell(x);
    int cellY = virtualYToCell(y);
    // End coordinates
    int xE = virtualXToCell(x + width);
    // if(xE>getCols())
    // xE=getCols();
    int yE = virtualYToCell(y + height);
    // if(yE>getRows())
    // yE=getRows();
    visibleCellRectangleChanged(cellX, cellY, xE - cellX, yE - cellY);
  }

  protected void visibleCellRectangleChanged(int x, int y, int width, int height) {}

  @Override protected void setVirtualExtend(int width, int height) {
    int cellHeight = getCellHeight();
    if (cellHeight > 0) {
      height -= height % cellHeight;
    }
    super.setVirtualExtend(width, height);
  }

  @Override protected void setVirtualOrigin(int x, int y) {
    int cellHeight = getCellHeight();
    if (cellHeight > 0) {
      int remainder = y % cellHeight;
      if (remainder < 0) {
        y -= (cellHeight + remainder);
      } else {
        y -= remainder;
      }
    }
    super.setVirtualOrigin(x, y);
  }

  @Override protected void scrollY(ScrollBar vBar) {
    int vSelection = vBar.getSelection();
    Rectangle bounds = getVirtualBounds();
    int y = -vSelection;
    int cellHeight = getCellHeight();
    if (cellHeight > 0) {
      int remainder = y % cellHeight;
      if (remainder < 0) {
        y -= (cellHeight + remainder);
      } else {
        y -= remainder;
      }
    }
    int deltaY = y - bounds.y;
    if (deltaY != 0) {
      scrollSmart(0, deltaY);
      setVirtualOrigin(bounds.x, bounds.y += deltaY);
    }
    if (-bounds.y + getRows() * getCellHeight() >= bounds.height) {
      // Scrolled to bottom - need to redraw bottom area
      Rectangle clientRect = getClientArea();
      redraw(0, clientRect.height - this.cellHeight, clientRect.width, this.cellHeight, false);
    }
  }
}
