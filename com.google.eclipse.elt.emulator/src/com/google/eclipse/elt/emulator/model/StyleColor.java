/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.model;

import java.util.concurrent.*;

public class StyleColor {
  private static final ConcurrentMap<String, StyleColor> STYLE_COLORS = new ConcurrentHashMap<String, StyleColor>();

  private final String name;

  public static StyleColor getStyleColor(String name) {
    StyleColor result = new StyleColor(name);
    return STYLE_COLORS.putIfAbsent(name, result);
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