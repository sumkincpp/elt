/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.terminal.model;

import java.util.*;

public class StyleColor {
  private static final Map<String, StyleColor> STYLE_COLORS = new HashMap<String, StyleColor>();

  private final String name;

  public static StyleColor getStyleColor(String name) {
    StyleColor result;
    synchronized (STYLE_COLORS) {
      result = STYLE_COLORS.get(name);
      if (result == null) {
        result = new StyleColor(name);
        STYLE_COLORS.put(name, result);
      }
    }
    return result;
  }

  private StyleColor(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override public String toString() {
    return name;
  }
}