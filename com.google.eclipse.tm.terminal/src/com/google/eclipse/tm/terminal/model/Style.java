/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.terminal.model;

import java.util.*;

// TODO add an Object for user data, use weak map to keep track of styles with associated user data.
public class Style {
  private final StyleColor forground;
  private final StyleColor background;
  private final boolean bold;
  private final boolean blink;
  private final boolean underline;
  private final boolean reverse;

  private static final Map<Style, Style> STYLES = new HashMap<Style, Style>();

  private Style(StyleColor forground, StyleColor background, boolean bold, boolean blink, boolean underline,
      boolean reverse) {
    this.forground = forground;
    this.background = background;
    this.bold = bold;
    this.blink = blink;
    this.underline = underline;
    this.reverse = reverse;
  }

  public static Style getStyle(StyleColor forground, StyleColor background, boolean bold, boolean blink,
      boolean underline, boolean reverse) {
    Style style = new Style(forground, background, bold, blink, underline, reverse);
    Style cached;
    synchronized (STYLES) {
      cached = STYLES.get(style);
      if (cached == null) {
        cached = style;
        STYLES.put(cached, cached);
      }
    }
    return cached;
  }

  public static Style getStyle(String forground, String background) {
    return getStyle(
        StyleColor.getStyleColor(forground), StyleColor.getStyleColor(background), false, false, false, false);
  }

  public static Style getStyle(StyleColor forground, StyleColor background) {
    return getStyle(forground, background, false, false, false, false);
  }

  public Style setForground(StyleColor forground) {
    return getStyle(forground, background, bold, blink, underline, reverse);
  }

  public Style setBackground(StyleColor background) {
    return getStyle(forground, background, bold, blink, underline, reverse);
  }

  public Style setForground(String colorName) {
    return getStyle(StyleColor.getStyleColor(colorName), background, bold, blink, underline, reverse);
  }

  public Style setBackground(String colorName) {
    return getStyle(forground, StyleColor.getStyleColor(colorName), bold, blink, underline, reverse);
  }

  public Style setBold(boolean bold) {
    return getStyle(forground, background, bold, blink, underline, reverse);
  }

  public Style setBlink(boolean blink) {
    return getStyle(forground, background, bold, blink, underline, reverse);
  }

  public Style setUnderline(boolean underline) {
    return getStyle(forground, background, bold, blink, underline, reverse);
  }

  public Style setReverse(boolean reverse) {
    return getStyle(forground, background, bold, blink, underline, reverse);
  }

  public StyleColor getBackground() {
    return background;
  }

  public boolean isBlink() {
    return blink;
  }

  public boolean isBold() {
    return bold;
  }

  public StyleColor getForground() {
    return forground;
  }

  public boolean isReverse() {
    return reverse;
  }

  public boolean isUnderline() {
    return underline;
  }

  @Override public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((background == null) ? 0 : background.hashCode());
    result = prime * result + (blink ? 1231 : 1237);
    result = prime * result + (bold ? 1231 : 1237);
    result = prime * result + ((forground == null) ? 0 : forground.hashCode());
    result = prime * result + (reverse ? 1231 : 1237);
    result = prime * result + (underline ? 1231 : 1237);
    return result;
  }

  @Override public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Style other = (Style) obj;
    if (background != other.background) {
      return false;
    }
    if (blink != other.blink) {
      return false;
    }
    if (bold != other.bold) {
      return false;
    }
    if (forground != other.forground) {
      return false;
    }
    if (reverse != other.reverse) {
      return false;
    }
    return underline == other.underline;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Style [forground=");
    builder.append(forground);
    builder.append(", background=");
    builder.append(background);
    builder.append(", bold=");
    builder.append(bold);
    builder.append(", blink=");
    builder.append(blink);
    builder.append(", underline=");
    builder.append(underline);
    builder.append(", reverse=");
    builder.append(reverse);
    builder.append("]");
    return builder.toString();
  }
}