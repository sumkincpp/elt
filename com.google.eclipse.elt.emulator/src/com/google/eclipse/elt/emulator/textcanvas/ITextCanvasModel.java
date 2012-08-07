/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.textcanvas;

import org.eclipse.swt.graphics.Point;

import com.google.eclipse.elt.emulator.model.ITerminalTextDataReadOnly;

public interface ITextCanvasModel {
  void addCellCanvasModelListener(ITextCanvasModelListener listener);

  void removeCellCanvasModelListener(ITextCanvasModelListener listener);

  ITerminalTextDataReadOnly getTerminalText();

  void setVisibleRectangle(int startLine, int startCol, int height, int width);

  /**
   * Indicates whether the cursor is shown (used for blinking cursors.)
   *
   * @return {@code true} if the cursor is shown, {@code false} otherwise.
   */
  boolean isCursorOn();

  boolean isCursorEnabled();

  /**
   * Show/Hide the cursor.
   *
   * @param visible indicates whether the cursor should be visible.
   */
  void setCursorEnabled(boolean visible);

  int getCursorLine();

  int getCursorColumn();

  Point getSelectionStart();

  Point getSelectionEnd();

  Point getSelectionAnchor();

  void setSelectionAnchor(Point anchor);

  // Negative 'startLine' clears the selection.
  void setSelection(int startLine, int endLine, int startColumn, int endColumn);

  boolean hasLineSelection(int line);

  String getSelectedText();

  void setBlinkingCursor(boolean useBlinkingCursor);
}