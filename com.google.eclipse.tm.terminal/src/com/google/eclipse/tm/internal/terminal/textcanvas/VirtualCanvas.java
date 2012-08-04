/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.textcanvas;

import static org.eclipse.core.runtime.IStatus.*;

import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

import com.google.eclipse.tm.internal.terminal.control.impl.TerminalPlugin;

/**
 * A {@code Canvas} showing a virtual object. Virtual: the extent of the total canvas. Screen: the visible client area
 * in the screen.
 */
public abstract class VirtualCanvas extends Canvas {
  private final Rectangle virtualBounds = new Rectangle(0, 0, 0, 0);

  /** Called when the viewed part is changing. */
  private final Rectangle viewRectangle = new Rectangle(0, 0, 0, 0);

  private Rectangle clientArea;

   /** Prevent infinite loop in {@link #updateScrollbars()} */
  private boolean inUpdateScrollbars;

  private static boolean inUpdateScrollbarsLogged;

  public VirtualCanvas(Composite parent, int style) {
    super(parent, style | SWT.NO_BACKGROUND | SWT.NO_REDRAW_RESIZE);
    clientArea = getClientArea();
    addListener(SWT.Paint, new Listener() {
      @Override public void handleEvent(Event event) {
        paint(event.gc);
      }
    });
    addListener(SWT.Resize, new Listener() {
      @Override public void handleEvent(Event event) {
        clientArea = getClientArea();
        onResize();
      }
    });
    getVerticalBar().addListener(SWT.Selection, new Listener() {
      @Override public void handleEvent(Event e) {
        scrollY((ScrollBar) e.widget);
      }
    });
    getHorizontalBar().addListener(SWT.Selection, new Listener() {
      @Override public void handleEvent(Event e) {
        scrollX((ScrollBar) e.widget);
      }
    });
  }

  protected void onResize() {
    updateViewRectangle();
  }

  protected void scrollX(ScrollBar horizontalBar) {
    int selection = horizontalBar.getSelection();
    int destinationX = -selection - virtualBounds.x;
    virtualBounds.x = -selection;
    scrollSmart(destinationX, 0);
    updateViewRectangle();
  }

  protected void scrollXDelta(int delta) {
    getHorizontalBar().setSelection(-virtualBounds.x + delta);
    scrollX(getHorizontalBar());
  }

  protected void scrollY(ScrollBar vBar) {
    int vSelection = vBar.getSelection();
    int destY = -vSelection - virtualBounds.y;
    if (destY != 0) {
      virtualBounds.y = -vSelection;
      scrollSmart(0, destY);
      updateViewRectangle();
    }
  }

  protected void scrollYDelta(int delta) {
    getVerticalBar().setSelection(-virtualBounds.y + delta);
    scrollY(getVerticalBar());
  }

  protected void scrollSmart(int deltaX, int deltaY) {
    if (deltaX != 0 || deltaY != 0) {
      Rectangle rect = getBounds();
      scroll(deltaX, deltaY, 0, 0, rect.width, rect.height, false);
    }
  }

  protected void revealRect(Rectangle rect) {
    Rectangle visibleRect = getScreenRectInVirtualSpace();
    // scroll the X part
    int deltaX = 0;
    if (rect.x < visibleRect.x) {
      deltaX = rect.x - visibleRect.x;
    } else if (visibleRect.x + visibleRect.width < rect.x + rect.width) {
      deltaX = (rect.x + rect.width) - (visibleRect.x + visibleRect.width);
    }
    if (deltaX != 0) {
      getHorizontalBar().setSelection(-virtualBounds.x + deltaX);
      scrollX(getHorizontalBar());
    }
    // scroll the Y part
    int deltaY = 0;
    if (rect.y < visibleRect.y) {
      deltaY = rect.y - visibleRect.y;
    } else if (visibleRect.y + visibleRect.height < rect.y + rect.height) {
      deltaY = (rect.y + rect.height) - (visibleRect.y + visibleRect.height);
    }
    if (deltaY != 0) {
      getVerticalBar().setSelection(-virtualBounds.y + deltaY);
      scrollY(getVerticalBar());
    }
  }

  protected void repaint(Rectangle r) {
    if (isDisposed()) {
      return;
    }
    if (inClipping(r, clientArea)) {
      redraw(r.x, r.y, r.width, r.height, true);
      update();
    }
  }

  abstract protected void paint(GC gc);

  protected Color getTerminalBackgroundColor() {
    return getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
  }

  protected void paintUnoccupiedSpace(GC gc, Rectangle clipping) {
    int width = virtualBounds.width + virtualBounds.x;
    int height = virtualBounds.height + virtualBounds.y;
    int marginWidth = (clipping.x + clipping.width) - width;
    int marginHeight = (clipping.y + clipping.height) - height;
    if (marginWidth > 0 || marginHeight > 0) {
      Color background = getBackground();
      gc.setBackground(getTerminalBackgroundColor());
      if (marginWidth > 0) {
        gc.fillRectangle(width, clipping.y, marginWidth, clipping.height);
      }
      if (marginHeight > 0) {
        gc.fillRectangle(clipping.x, height, clipping.width, marginHeight);
      }
      gc.setBackground(background);
    }
  }

  protected boolean inClipping(Rectangle clipping, Rectangle r) {
    // TODO check if this is OK in all cases (the <=!)
    if (r.x + r.width <= clipping.x) {
      return false;
    }
    if (clipping.x + clipping.width <= r.x) {
      return false;
    }
    if (r.y + r.height <= clipping.y) {
      return false;
    }
    if (clipping.y + clipping.height <= r.y) {
      return false;
    }
    return true;
  }

  protected Rectangle getScreenRectInVirtualSpace() {
    return new Rectangle(
        clientArea.x - virtualBounds.x, clientArea.y - virtualBounds.y, clientArea.width, clientArea.height);
  }

  protected Rectangle getRectInVirtualSpace(Rectangle r) {
    return new Rectangle(r.x - virtualBounds.x, r.y - virtualBounds.y, r.width, r.height);
  }

  protected void setVirtualExtend(int width, int height) {
    virtualBounds.width = width;
    virtualBounds.height = height;
    updateScrollbars();
    updateViewRectangle();
  }

  protected void setVirtualOrigin(int x, int y) {
    if (virtualBounds.x != x || virtualBounds.y != y) {
      virtualBounds.x = x;
      virtualBounds.y = y;
      getHorizontalBar().setSelection(-x);
      getVerticalBar().setSelection(-y);
      updateViewRectangle();
    }
  }

  protected Rectangle getVirtualBounds() {
    return cloneRectangle(virtualBounds);
  }

  protected int virtualXtoScreen(int x) {
    return x + virtualBounds.x;
  }

  protected int virtualYtoScreen(int y) {
    return y + virtualBounds.y;
  }

  protected int screenXtoVirtual(int x) {
    return x - virtualBounds.x;
  }

  protected int screenYtoVirtual(int y) {
    return y - virtualBounds.y;
  }

  protected void updateViewRectangle() {
    if (viewRectangle.x == -virtualBounds.x && viewRectangle.y == -virtualBounds.y
        && viewRectangle.width == clientArea.width && viewRectangle.height == clientArea.height) {
      return;
    }
    viewRectangle.x = -virtualBounds.x;
    viewRectangle.y = -virtualBounds.y;
    viewRectangle.width = clientArea.width;
    viewRectangle.height = clientArea.height;
    viewRectangleChanged(viewRectangle.x, viewRectangle.y, viewRectangle.width, viewRectangle.height);
  }

  protected Rectangle getViewRectangle() {
    return cloneRectangle(viewRectangle);
  }

  private Rectangle cloneRectangle(Rectangle r) {
    return new Rectangle(r.x, r.y, r.width, r.height);
  }

  protected void viewRectangleChanged(int x, int y, int width, int height) {}

  private void updateScrollbars() {
    // don't get into infinite loops....
    if (!inUpdateScrollbars) {
      inUpdateScrollbars = true;
      try {
        doUpdateScrollbar();
      } finally {
        inUpdateScrollbars = false;
      }
    } else {
      if (!inUpdateScrollbarsLogged) {
        inUpdateScrollbarsLogged = true;
        ILog logger = TerminalPlugin.getDefault().getLog();
        logger.log(new Status(WARNING, TerminalPlugin.PLUGIN_ID, OK, "Unexpected Recursion in terminal", null));
      }
    }
  }

  private void doUpdateScrollbar() {
    Rectangle clientArea = getClientArea();
    ScrollBar horizontal = getHorizontalBar();
    // Even if setVisible was called on the scrollbar, isVisible returns false if its parent is not visible.
    if (!isVisible() || horizontal.isVisible()) {
      horizontal.setPageIncrement(clientArea.width - horizontal.getIncrement());
      int max = virtualBounds.width;
      horizontal.setMaximum(max);
      horizontal.setThumb(clientArea.width);
    }
    ScrollBar vertical = getVerticalBar();
    // even if setVisible was called on the scrollbar, isVisible returns false if its parent is not visible.
    if (!isVisible() || vertical.isVisible()) {
      vertical.setPageIncrement(clientArea.height - vertical.getIncrement());
      int max = virtualBounds.height;
      vertical.setMaximum(max);
      vertical.setThumb(clientArea.height);
    }
  }

  protected boolean isVertialBarVisible() {
    return getVerticalBar().isVisible();
  }

  protected void serVerticalBarVisible(boolean showVScrollBar) {
    ScrollBar vertical = getVerticalBar();
    vertical.setVisible(showVScrollBar);
    vertical.setSelection(0);
  }

  protected boolean isHorizontalBarVisble() {
    return getHorizontalBar().isVisible();
  }

  protected void setHorizontalBarVisible(boolean showHScrollBar) {
    ScrollBar horizontal = getHorizontalBar();
    horizontal.setVisible(showHScrollBar);
    horizontal.setSelection(0);
  }
}
