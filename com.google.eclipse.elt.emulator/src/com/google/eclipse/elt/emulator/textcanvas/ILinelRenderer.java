/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.textcanvas;

import org.eclipse.swt.graphics.*;

public interface ILinelRenderer {
  int getCellWidth();

  int getCellHeight();

  void drawLine(ITextCanvasModel model, GC gc, int line, int x, int y, int firstColumn, int lastColumn);

  void onFontChange();

  void setInvertedColors(boolean invert);

  Color getDefaultBackgroundColor();

  void setColors(RGB background, RGB foreground);

  void setFont(Font font);
}
