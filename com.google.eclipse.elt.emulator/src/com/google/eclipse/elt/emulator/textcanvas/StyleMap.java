/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.textcanvas;

import java.util.*;

import org.eclipse.jface.resource.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import com.google.eclipse.elt.emulator.model.*;

public class StyleMap {
  private static final String BLACK = "black";
  private static final String WHITE = "white";
  private static final String WHITE_FOREGROUND = "white_foreground";
  private static final String GRAY = "gray";
  private static final String MAGENTA = "magenta";
  private static final String CYAN = "cyan";
  private static final String YELLOW = "yellow";
  private static final String BLUE = "blue";
  private static final String GREEN = "green";
  private static final String RED = "red";

  private static final String PREFIX = "org.eclipse.tm.internal.";

  private final Map<StyleColor, Color> colorMapForeground = new HashMap<StyleColor, Color>();
  private final Map<StyleColor, Color> colorMapBackground = new HashMap<StyleColor, Color>();
  private final Map<StyleColor, Color> colorMapIntense = new HashMap<StyleColor, Color>();

  private Point charSize;

  private boolean invertColors;
  private boolean proportional;

  private final int[] offsets = new int[256];

  private Color background = getColor(new RGB(0, 0, 0));
  private Color foreground = getColor(new RGB(229, 229, 229));

  private Font font = JFaceResources.getFontRegistry().get("org.eclipse.jface.textfont");

  StyleMap() {
    initColors();
    updateFont();
  }

  private void initColors() {
    initForegroundColors();
    initBackgroundColors();
    initIntenseColors();
  }

  private void initForegroundColors() {
    if (invertColors) {
      setColor(colorMapForeground, WHITE, 0, 0, 0);
      setColor(colorMapForeground, WHITE_FOREGROUND, 50, 50, 50);
      setColor(colorMapForeground, BLACK, 229, 229, 229);
    } else {
      setColor(colorMapForeground, WHITE, 255, 255, 255);
      setColor(colorMapForeground, WHITE_FOREGROUND, 229, 229, 229);
      setColor(colorMapForeground, BLACK, 50, 50, 50);
    }
    setColor(colorMapForeground, RED, 205, 0, 0);
    setColor(colorMapForeground, GREEN, 0, 205, 0);
    setColor(colorMapForeground, BLUE, 0, 0, 238);
    setColor(colorMapForeground, YELLOW, 205, 205, 0);
    setColor(colorMapForeground, CYAN, 0, 205, 205);
    setColor(colorMapForeground, MAGENTA, 205, 0, 205);
    setColor(colorMapForeground, GRAY, 229, 229, 229);
  }

  private void initBackgroundColors() {
    if (invertColors) {
      setColor(colorMapBackground, WHITE, 0, 0, 0);
      setColor(colorMapBackground, WHITE_FOREGROUND, 50, 50, 50); // only used when colors are inverse
      setColor(colorMapBackground, BLACK, 255, 255, 255); // cursor color
    } else {
      setColor(colorMapBackground, WHITE, 255, 255, 255);
      setColor(colorMapBackground, WHITE_FOREGROUND, 229, 229, 229);
      setColor(colorMapBackground, BLACK, 0, 0, 0);
    }
    setColor(colorMapBackground, RED, 205, 0, 0);
    setColor(colorMapBackground, GREEN, 0, 205, 0);
    setColor(colorMapBackground, BLUE, 0, 0, 238);
    setColor(colorMapBackground, YELLOW, 205, 205, 0);
    setColor(colorMapBackground, CYAN, 0, 205, 205);
    setColor(colorMapBackground, MAGENTA, 205, 0, 205);
    setColor(colorMapBackground, GRAY, 229, 229, 229);
  }

  private void initIntenseColors() {
    if (invertColors) {
      setColor(colorMapIntense, WHITE, 127, 127, 127);
      setColor(colorMapIntense, WHITE_FOREGROUND, 0, 0, 0); // only used when colors are inverse
      setColor(colorMapIntense, BLACK, 255, 255, 255);
    } else {
      setColor(colorMapIntense, WHITE, 255, 255, 255);
      setColor(colorMapIntense, WHITE_FOREGROUND, 255, 255, 255);
      setColor(colorMapIntense, BLACK, 0, 0, 0);
    }
    setColor(colorMapIntense, RED, 255, 0, 0);
    setColor(colorMapIntense, GREEN, 0, 255, 0);
    setColor(colorMapIntense, BLUE, 92, 92, 255);
    setColor(colorMapIntense, YELLOW, 255, 255, 0);
    setColor(colorMapIntense, CYAN, 0, 255, 255);
    setColor(colorMapIntense, MAGENTA, 255, 0, 255);
    setColor(colorMapIntense, GRAY, 255, 255, 255);
  }

  private void setColor(Map<StyleColor, Color> colorMap, String name, int r, int g, int b) {
    Color color = getColor(new RGB(r, g, b));
    setColor(colorMap, color, StyleColor.getStyleColor(name));
    setColor(colorMap, color, StyleColor.getStyleColor(name.toUpperCase()));
  }

  private void setColor(Map<StyleColor, Color> colorMap, Color color, StyleColor styleColor) {
    if (styleColor != null) {
      colorMap.put(styleColor, color);
    }
  }

  public Color getForegroundColor(Style style) {
    if (style == null) {
      return foreground;
    }
    StyleColor color = style.isReverse() ? style.getBackground() : style.getForeground();
    Map<StyleColor, Color> map = style.isBold() ? colorMapIntense : colorMapForeground;
    Color actualColor = map.get(color);
    if (actualColor == null) {
      actualColor = foreground;
    }
    return actualColor;
  }

  public Color getBackgroundColor(Style style) {
    if (style == null) {
      return background;
    }
    StyleColor color = style.isReverse() ? style.getForeground() : style.getBackground();
    Color actualColor = colorMapBackground.get(color);
    if (actualColor == null) {
      actualColor = background;
    }
    return actualColor;
  }

  public void setInvertedColors(boolean invert) {
    if (invert == invertColors) {
      return;
    }
    invertColors = invert;
    initColors();
  }

  public Font getFont(Style style) {
    if (style == null) {
      return font;
    }
    FontData fontDatas[] = font.getFontData();
    FontData data = fontDatas[0];
    if (style.isBold()) {
      return new Font(font.getDevice(), data.getName(), data.getHeight(), data.getStyle() | SWT.BOLD);
    }
    if (style.isUnderline()) {
      return new Font(font.getDevice(), data.getName(), data.getHeight(), data.getStyle() | SWT.ITALIC);
    }
    return font;
  }

  public Font getFont() {
    return font;
  }

  public int getFontWidth() {
    return charSize.x;
  }

  public int getFontHeight() {
    return charSize.y;
  }

  public void updateFont() {
    Display display = Display.getCurrent();
    GC gc = new GC(display);
    gc.setFont(font);
    charSize = gc.textExtent("W");
    proportional = false;
    for (char c = ' '; c <= '~'; c++) {
      // Consider only the first 128 chars for deciding if a font is proportional.
      if (measureChar(gc, c, true)) {
        proportional = true;
      }
    }
    // TODO should we also consider the upper 128 chars?
    for (char c = ' ' + 128; c <= '~' + 128; c++) {
      measureChar(gc, c, false);
    }
    if (proportional) {
      charSize.x -= 2; // Works better on small fonts.
    }
    for (int i = 0; i < offsets.length; i++) {
      offsets[i] = (charSize.x - offsets[i]) / 2;
    }
    gc.dispose();
  }

  private boolean measureChar(GC gc, char c, boolean updateMax) {
    boolean proportional = false;
    Point extent = gc.textExtent(String.valueOf(c));
    if (extent.x > 0 && extent.y > 0 && (charSize.x != extent.x || charSize.y != extent.y)) {
      proportional = true;
      if (updateMax) {
        charSize.x = Math.max(charSize.x, extent.x);
        charSize.y = Math.max(charSize.y, extent.y);
      }
    }
    offsets[c] = extent.x;
    return proportional;
  }

  public boolean isFontProportional() {
    return proportional;
  }

  /**
   * Return the offset in pixels required to center a given character.
   *
   * @param c the character to measure.
   * @return the offset in x direction to center this character.
   */
  public int getCharOffset(char c) {
    if (c >= offsets.length) {
      return 0;
    }
    return offsets[c];
  }

  public void setColors(RGB background, RGB foreground) {
    this.background = getColor(background);
    this.foreground = getColor(foreground);
  }

  private Color getColor(RGB colorData) {
    String name = PREFIX + colorData.red + "-" + colorData.green + "-" + colorData.blue;
    ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
    Color color = colorRegistry.get(name);
    if (color == null) {
      colorRegistry.put(name, colorData);
      color = colorRegistry.get(name);
    }
    return color;
  }

  public void setFont(Font font) {
    this.font = font;
    updateFont();
  }
}
