/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.core;

import java.io.PrintStream;
import java.util.List;

import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.google.eclipse.elt.emulator.model.Style;

public class VT100BackendTraceDecorator implements IVT100EmulatorBackend {
  final IVT100EmulatorBackend backend;
  final PrintStream out;

  public VT100BackendTraceDecorator(IVT100EmulatorBackend backend, PrintStream out) {
    this.backend = backend;
    this.out = out;
  }

  @Override public void appendString(String buffer) {
    out.println("appendString(\"" + buffer + "\")");
    backend.appendString(buffer);
  }

  @Override public void clearAll() {
    out.println("clearAll()");
    backend.clearAll();
  }

  @Override public void deleteCharacters(int n) {
    out.println("deleteCharacters(" + n + ")");
    backend.deleteCharacters(n);
  }

  @Override public void deleteLines(int n) {
    out.println("deleteLines(" + n + ")");
    backend.deleteLines(n);
  }

  @Override public void eraseAll() {
    out.println("eraseAll()");
    backend.eraseAll();
  }

  @Override public void eraseLine() {
    out.println("eraseLine()");
    backend.eraseLine();
  }

  @Override public void eraseLineToCursor() {
    out.println("eraseLineToCursor()");
    backend.eraseLineToCursor();
  }

  @Override public void eraseLineToEnd() {
    out.println("eraseLineToEnd()");
    backend.eraseLineToEnd();
  }

  @Override public void eraseToCursor() {
    out.println("eraseToCursor()");
    backend.eraseToCursor();
  }

  @Override public void eraseToEndOfScreen() {
    out.println("eraseToEndOfScreen()");
    backend.eraseToEndOfScreen();
  }

  @Override public int getColumns() {
    return backend.getColumns();
  }

  @Override public int getCursorColumn() {
    return backend.getCursorColumn();
  }

  @Override public int getCursorLine() {
    return backend.getCursorLine();
  }

  @Override public Style getDefaultStyle() {
    return backend.getDefaultStyle();
  }

  @Override public int getLines() {
    return backend.getLines();
  }

  @Override public Style getStyle() {
    return backend.getStyle();
  }

  @Override public void insertCharacters(int charactersToInsert) {
    out.println("insertCharacters(" + charactersToInsert + ")");
    backend.insertCharacters(charactersToInsert);
  }

  @Override public void insertLines(int n) {
    out.println("insertLines(" + n + ")");
    backend.insertLines(n);
  }

  @Override public void processNewline() {
    out.println("processNewline()");
    backend.processNewline();
  }

  @Override public void setCursor(int targetLine, int targetColumn) {
    out.println("setCursor(" + targetLine + ", " + targetColumn + ")");
    backend.setCursor(targetLine, targetColumn);
  }

  @Override public void setCursorColumn(int targetColumn) {
    out.println("setCursorColumn(" + targetColumn + ")");
    backend.setCursorColumn(targetColumn);
  }

  @Override public void setCursorLine(int targetLine) {
    out.println("setCursorLine(" + targetLine + ")");
    backend.setCursorLine(targetLine);
  }

  @Override public void setDefaultStyle(Style defaultStyle) {
    out.println("setDefaultStyle(" + defaultStyle + ")");
    backend.setDefaultStyle(defaultStyle);
  }

  @Override public void setDimensions(int lines, int cols) {
    out.println("setDimensions(" + lines + "," + cols + ")");
    backend.setDimensions(lines, cols);
  }

  @Override public void setStyle(Style style) {
    out.println("setStyle(" + style + ")");
    backend.setStyle(style);
  }

  @Override public List<IHyperlink> hyperlinksAt(int line) {
    return backend.hyperlinksAt(line);
  }
}
