/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.core;

import java.util.List;

import org.eclipse.jface.text.hyperlink.IHyperlink;

import com.google.eclipse.elt.emulator.model.Style;

public interface IVT100EmulatorBackend {
  /**
   * Erases all text from the terminal view. Including the history.
   */
  void clearAll();

  /**
   * Sets the dimensions of the addressable scroll space of the screen. Keeps the cursor position relative to the bottom
   * of the screen.
   *
   * @param lines the number of lines.
   * @param columns the number of columns.
   */
  void setDimensions(int lines, int columns);

  /**
   * Makes room for N characters on the current line at the cursor position. Text under the cursor moves right
   * without wrapping at the end of the line.
   *
   * @param charactersToInsert the number of characters to insert.
   */
  void insertCharacters(int charactersToInsert);

  /**
   * Erases from the cursor position (inclusive) to the end of screen. The cursor does not move.
   */
  void eraseToEndOfScreen();

  /**
   * Erases from the beginning of screen to the cursor position (inclusive). The cursor does not move.
   */
  void eraseToCursor();

  /**
   * Erases the complete display. All lines are erased and changed to single-width. The cursor does not move.
   */
  void eraseAll();

  /**
   * Erases the complete line.
   */
  void eraseLine();

  /**
   * Erases from the cursor position (inclusive) to the end of line.
   */
  void eraseLineToEnd();

  /**
   * Erases from beginning of line to the cursor position (inclusive).
   */
  void eraseLineToCursor();

  /**
   * Inserts n lines at line with cursor. Lines displayed below cursor are moved down. Lines moved past the bottom
   * margin are lost. This sequence is ignored when cursor is outside scrolling region.
   *
   * @param lineCount the number of lines to insert.
   */
  void insertLines(int lineCount);

  /**
   * Deletes n characters, starting with the character at the cursor position. When a character is deleted, all
   * characters to the right of cursor move left. This creates a space character at right margin. This character has
   * same character attribute as the last character moved left.
   *
   * @param charCount the number of characters to delete.
   */
  void deleteCharacters(int charCount);

  /**
   * Deletes n lines, starting at line with cursor. As lines are deleted, the lines displayed below cursor move up.
   * The lines added to bottom of screen have spaces with the same character attributes as the last line moved up.
   * This sequence is ignored when cursor is outside scrolling region.
   *
   * @param lineCount the number of lines to delete.
   */
  void deleteLines(int lineCount);

  Style getDefaultStyle();

  void setDefaultStyle(Style defaultStyle);

  Style getStyle();

  /**
   * Sets the style to use.
   *
   * @param style the new style.
   */
  void setStyle(Style style);

  /**
   * Displays a subset of the newly-received text in the terminal view, wrapping text at the right edge of the screen
   * and overwriting text when the cursor is not at the very end of the screen's text.
   *
   * <p>
   * There are never any ANSI control characters or escape sequences in the text being displayed by this method (this
   * includes newlines, carriage returns, and tabs.)
   * </p>
   *
   * @param buffer the text to append.
   */
  void appendString(String buffer);

  /**
   * Process a newline (Control-J) character. A newline (NL) character just moves the cursor to the same column on the
   * next line, creating new lines when the cursor reaches the bottom edge of the terminal. This is counter-intuitive,
   * especially to UNIX programmers who are taught that writing a single NL to a terminal is sufficient to move the
   * cursor to the first column of the next line, as if a carriage return (CR) and a NL were written.
   *
   * <p>
   * UNIX terminals typically display a NL character as a CR followed by a NL because the terminal device typically has
   * the ONLCR attribute bit set (see the termios(4) man page for details), which causes the terminal device driver to
   * translate NL to CR + NL on output. The terminal itself (i.e., a hardware terminal or a terminal emulator, like
   * xterm or this code) _always_ interprets a CR to mean "move the cursor to the beginning of the current
   * line" and a NL to mean "move the cursor to the same column on the next line".
   * </p>
   */
  void processNewline();

  /**
   * Returns the relative line number of the line containing the cursor. The returned line number is relative to the
   * top-most visible line, which has relative line number 0.
   *
   * @return the relative line number of the line containing the cursor.
   */
  int getCursorLine();

  int getCursorColumn();

  /**
   * Moves the cursor to the specified line and column.
   *
   * @param targetLine is the line number of a screen line, so it has a minimum value of 0 (the topmost screen line) and
   * a maximum value of heightInLines - 1 (the bottom-most screen line). A line does not have to contain any text to
   * move the cursor to any column in that line.
   * @param targetColumn the given column.
   */
  void setCursor(int targetLine, int targetColumn);

  void setCursorColumn(int targetColumn);

  void setCursorLine(int targetLine);

  int getLines();

  int getColumns();

  List<IHyperlink> hyperlinksAt(int line);
}