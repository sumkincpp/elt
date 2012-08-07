/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 *******************************************************************************/
package com.google.eclipse.elt.emulator.model;

public class LineSegment {
  private final int column;
  private final String text;
  private final Style style;

  public LineSegment(int column, String text, Style style) {
    this.column = column;
    this.text = text;
    this.style = style;
  }

  public int getColumn() {
    return column;
  }

  public Style getStyle() {
    return style;
  }

  public String getText() {
    return text;
  }

  @Override public String toString() {
    return "LineSegment(" + column + ", \"" + text + "\"," + style + ")";
  }
}