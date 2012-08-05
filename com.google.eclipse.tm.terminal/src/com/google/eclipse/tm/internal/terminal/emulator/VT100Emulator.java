/*******************************************************************************
 * Copyright (c) 2003, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.emulator;

import java.io.*;
import java.util.List;

import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.events.*;

import com.google.eclipse.tm.internal.terminal.control.impl.*;
import com.google.eclipse.tm.internal.terminal.provisional.api.*;
import com.google.eclipse.tm.terminal.model.*;

/**
 * This class processes character data received from the remote host and displays it to the user using the Terminal
 * view's StyledText widget. This class processes ANSI control characters, including NUL, backspace, carriage return,
 * line-feed, and a subset of ANSI escape sequences sufficient to allow use of screen-oriented applications, such as vi,
 * Emacs, and any GNU readline-enabled application (bash, bc, ncftp, etc.).
 * <p>
 */
public class VT100Emulator implements ControlListener {
  // This is a character processing state: Initial state.
  private static final int ANSISTATE_INITIAL = 0;

  // This is a character processing state: We've seen an escape character.
  private static final int ANSISTATE_ESCAPE = 1;

  // Character processing state: We've seen a '[' after an escape character. Expecting a parameter character or a
  // command character next.
  private static final int ANSISTATE_EXPECTING_PARAMETER_OR_COMMAND = 2;

  // Character processing state: We've seen a ']' after an escape character. We are now expecting an operating system
  // command that reprograms an intelligent terminal.
  private static final int ANSISTATE_EXPECTING_OS_COMMAND = 3;

  /**
   * This field holds the current state of the Finite Terminal State Automaton (FSA) that recognizes ANSI escape
   * sequences.
   *
   * @see #processNewText()
   */
  private int ansiState = ANSISTATE_INITIAL;

  // Holds a reference to the {@link TerminalControl} object that instantiates this class.
  private final ITerminalControlForText terminal;

  // Holds a reference to the StyledText widget that is used to display text to the user.
  private final IVT100EmulatorBackend text;

  // Holds the saved absolute line number of the cursor when processing the "ESC 7" and "ESC 8" command sequences.
  private int savedCursorLine = 0;

  // Holds the saved column number of the cursor when processing the "ESC 7" and "ESC 8" command sequences.
  private int savedCursorColumn = 0;

  // Holds an array of StringBuffer objects, each of which is one parameter from the current ANSI escape
  // sequence. For example, when parsing the escape sequence "\e[20;10H", this array holds the strings "20" and "10".
  private final StringBuffer[] parameters = new StringBuffer[16];

  // Holds the OS-specific command found in an escape sequence of the form "\e]...\u0007".
  private final StringBuffer ansiOsCommand = new StringBuffer(128);

  // Holds the index of the next unused element of the array stored in field 'parameters'.
  private int nextAnsiParameter = 0;

  private int nextChar = -1;

  private Reader reader;

  private boolean crAfterNewLine;

  /**
   * The constructor.
   */
  public VT100Emulator(ITerminalTextData data, ITerminalControlForText terminal, Reader reader) {
    super();
    this.terminal = terminal;
    for (int i = 0; i < parameters.length; ++i) {
      parameters[i] = new StringBuffer();
    }
    setInputStreamReader(reader);
    if (TerminalPlugin.isOptionEnabled("com.google.eclipse.tm.terminal/debug/log/VT100Backend")) {
      text = new VT100BackendTraceDecorator(new VT100EmulatorBackend(data), System.out);
    } else {
      text = new VT100EmulatorBackend(data);
    }
  }

  /**
   * Set the reader that this terminal gets its input from.
   *
   * The reader can be changed while the Terminal is running, but a change of the reader likely loses some characters
   * which have not yet been fully read. Changing the reader can be done in order to change the selected Encoding,
   * though. This is typically done when the Terminal is constructed, i.e. before it really starts operation; or, when
   * the user manually selects a different encoding and thus doesn't care about losing old characters.
   *
   * @param reader the new reader.
   */
  public void setInputStreamReader(Reader reader) {
    this.reader = reader;
  }

  public void setDimensions(int lines, int cols) {
    text.setDimensions(lines, cols);
    ITerminalConnector telnetConnection = getConnector();
    if (telnetConnection != null) {
      telnetConnection.setTerminalSize(text.getColumns(), text.getLines());
    }
  }

  public void dispose() {
  }

  @Override public void controlMoved(ControlEvent event) {
  }

  @Override public void controlResized(ControlEvent event) {
    adjustTerminalDimensions();
  }

  public void clearTerminal() {
    text.clearAll();
  }

  public void fontChanged() {
    if (text != null) {
      adjustTerminalDimensions();
    }
  }

  public void processText() {
    try {
      // Find the width and height of the terminal, and resize it to display an integral number of lines and columns.
      adjustTerminalDimensions();
      // Restore the caret offset, process and display the new text, then save
      // the caret offset. See the documentation for field caretOffset for
      // details.
      // ISSUE: Is this causing the scroll-to-bottom-on-output behavior?
      try {
        processNewText();
      } catch (IOException e) {
        Logger.logException(e);
      }
    } catch (RuntimeException e) {
      Logger.logException(e);
    }
  }

  /**
   * This method scans the newly received text, processing ANSI control characters and escape sequences and displaying
   * normal text.
   *
   * @throws IOException if something goes wrong.
   */
  private void processNewText() throws IOException {
    // Scan the newly received text.
    while (hasNextChar()) {
      char character = getNextChar();
      switch (ansiState) {
      case ANSISTATE_INITIAL:
        switch (character) {
        case '\u0000':
          break; // NUL character. Ignore it.
        case '\u0007':
          processBEL(); // BEL (Control-G)
          break;
        case '\b':
          processBackspace(); // Backspace
          break;
        case '\t':
          processTab(); // Tab.
          break;
        case '\n':
          processNewline(); // Newline (Control-J)
          if (crAfterNewLine) {
            processCarriageReturn(); // Carriage Return (Control-M)
          }
          break;
        case '\r':
          processCarriageReturn(); // Carriage Return (Control-M)
          break;
        case '\u001b':
          ansiState = ANSISTATE_ESCAPE; // Escape.
          break;
        default:
          processNonControlCharacters(character);
          break;
        }
        break;
      case ANSISTATE_ESCAPE:
        // We've seen an escape character. Here, we process the character immediately following the escape.
        switch (character) {
        case '[':
          ansiState = ANSISTATE_EXPECTING_PARAMETER_OR_COMMAND;
          nextAnsiParameter = 0;
          // Erase the parameter strings in preparation for optional parameter characters.
          for (int i = 0; i < parameters.length; ++i) {
            parameters[i].delete(0, parameters[i].length());
          }
          break;
        case ']':
          ansiState = ANSISTATE_EXPECTING_OS_COMMAND;
          ansiOsCommand.delete(0, ansiOsCommand.length());
          break;
        case '7':
          // Save cursor position and character attributes.
          ansiState = ANSISTATE_INITIAL;
          savedCursorLine = relativeCursorLine();
          savedCursorColumn = getCursorColumn();
          break;
        case '8':
          // Restore cursor and attributes to previously saved position.
          ansiState = ANSISTATE_INITIAL;
          moveCursor(savedCursorLine, savedCursorColumn);
          break;
        case 'c':
          // Reset the terminal.
          ansiState = ANSISTATE_INITIAL;
          resetTerminal();
          break;
        default:
          Logger.log("Unsupported escape sequence: escape '" + character + "'");
          ansiState = ANSISTATE_INITIAL;
          break;
        }
        break;
      case ANSISTATE_EXPECTING_PARAMETER_OR_COMMAND:
        // Parameters can appear after the '[' in an escape sequence, but they are optional.
        if (character == '@' || (character >= 'A' && character <= 'Z') || (character >= 'a' && character <= 'z')) {
          ansiState = ANSISTATE_INITIAL;
          processAnsiCommandCharacter(character);
        } else {
          processAnsiParameterCharacter(character);
        }
        break;
      case ANSISTATE_EXPECTING_OS_COMMAND:
        // A BEL (\u0007) character marks the end of the OSC sequence.
        if (character == '\u0007') {
          ansiState = ANSISTATE_INITIAL;
          processAnsiOsCommand();
        } else {
          ansiOsCommand.append(character);
        }
        break;
      default:
        // This should never happen! If it does happen, it means there is a bug in the FSA. For robustness, we return to
        // the initial state.
        Logger.log("INVALID ANSI FSA STATE: " + ansiState);
        ansiState = ANSISTATE_INITIAL;
        break;
      }
    }
  }

  private void resetTerminal() {
    text.eraseAll();
    text.setCursor(0, 0);
    text.setStyle(text.getDefaultStyle());
  }

  // This method is called when we have parsed an OS Command escape sequence. The only one we support is
  // "\e]0;...\u0007", which sets the terminal title.
  private void processAnsiOsCommand() {
    if (ansiOsCommand.charAt(0) != '0' || ansiOsCommand.charAt(1) != ';') {
      Logger.log("Ignoring unsupported ANSI OSC sequence: '" + ansiOsCommand + "'");
      return;
    }
    terminal.setTerminalTitle(ansiOsCommand.substring(2));
  }

  // Dispatches control to various processing methods based on the command character found in the most recently received
  // ANSI escape sequence. This method only handles command characters that follow the ANSI standard Control Sequence
  // Introducer (CSI), which is "\e[...", where "..." is an optional ';'-separated sequence of numeric parameters.
  private void processAnsiCommandCharacter(char ansiCommandCharacter) {
    // If the width or height of the terminal is ridiculously small (one line or column or less), don't even try to
    // process the escape sequence. This avoids throwing an exception (SPR 107450). The display will be messed up, but
    // what did you user expect by making the terminal so small?
    switch (ansiCommandCharacter) {
    case '@':
      // Insert character(s).
      processAnsiCommand_atsign();
      break;
    case 'A':
      // Move cursor up N lines (default 1).
      processAnsiCommand_A();
      break;
    case 'B':
      // Move cursor down N lines (default 1).
      processAnsiCommand_B();
      break;
    case 'C':
      // Move cursor forward N columns (default 1).
      processAnsiCommand_C();
      break;
    case 'D':
      // Move cursor backward N columns (default 1).
      processAnsiCommand_D();
      break;
    case 'E':
      // Move cursor to first column of Nth next line (default 1).
      processAnsiCommand_E();
      break;
    case 'F':
      // Move cursor to first column of Nth previous line (default 1).
      processAnsiCommand_F();
      break;
    case 'G':
      // Move to column N of current line (default 1).
      processAnsiCommand_G();
      break;
    case 'H':
      // Set cursor Position.
      processAnsiCommand_H();
      break;
    case 'J':
      // Erase part or all of display. Cursor does not move.
      processAnsiCommand_J();
      break;
    case 'K':
      // Erase in line (cursor does not move).
      processAnsiCommand_K();
      break;
    case 'L':
      // Insert line(s) (current line moves down).
      processAnsiCommand_L();
      break;
    case 'M':
      // Delete line(s).
      processAnsiCommand_M();
      break;
    case 'm':
      // Set Graphics Rendition (SGR).
      processAnsiCommand_m();
      break;
    case 'n':
      // Device Status Report (DSR).
      processAnsiCommand_n();
      break;
    case 'P':
      // Delete character(s).
      processAnsiCommand_P();
      break;
    case 'S':
      // Scroll up.
      // Emacs, vi, and GNU readline don't seem to use this command, so we ignore it for now.
      break;
    case 'T':
      // Scroll down.
      // Emacs, vi, and GNU readline don't seem to use this command, so we ignore it for now.
      break;
    case 'X':
      // Erase character.
      // Emacs, vi, and GNU readline don't seem to use this command, so we ignore it for now.
      break;
    case 'Z':
      // Cursor back tab.
      // Emacs, vi, and GNU readline don't seem to use this command, so we ignore it for now.
      break;

    default:
      Logger.log("Ignoring unsupported ANSI command character: '" + ansiCommandCharacter + "'");
      break;
    }
  }

  // Makes room for N characters on the current line at the cursor position. Text under the cursor moves right without
  // wrapping at the end of the line.
  private void processAnsiCommand_atsign() {
    int charactersToInsert = getAnsiParameter(0);
    text.insertCharacters(charactersToInsert);
  }

  // Moves the cursor up by the number of lines specified by the escape sequence parameter (default 1).
  private void processAnsiCommand_A() {
    moveCursorUp(getAnsiParameter(0));
  }

  // Moves the cursor down by the number of lines specified by the escape sequence parameter (default 1).
  private void processAnsiCommand_B() {
    moveCursorDown(getAnsiParameter(0));
  }

  // Moves the cursor forward by the number of columns specified by the escape sequence parameter (default 1).
  private void processAnsiCommand_C() {
    moveCursorForward(getAnsiParameter(0));
  }

  // Moves the cursor backward by the number of columns specified by the escape sequence parameter (default 1).
  private void processAnsiCommand_D() {
    moveCursorBackward(getAnsiParameter(0));
  }

  // Moves the cursor to the first column of the Nth next line, where N is specified by the ANSI parameter (default 1).
  private void processAnsiCommand_E() {
    int linesToMove = getAnsiParameter(0);
    moveCursor(relativeCursorLine() + linesToMove, 0);
  }

  // Moves the cursor to the first column of the Nth previous line, where N is specified by the ANSI parameter
  // (default 1).
  private void processAnsiCommand_F() {
    int linesToMove = getAnsiParameter(0);
    moveCursor(relativeCursorLine() - linesToMove, 0);
  }

  // Moves the cursor within the current line to the column specified by the ANSI parameter (default is column 1).
  private void processAnsiCommand_G() {
    int targetColumn = 1;
    if (parameters[0].length() > 0) {
      targetColumn = getAnsiParameter(0) - 1;
    }
    moveCursor(relativeCursorLine(), targetColumn);
  }

  // Sets the cursor to a position specified by the escape sequence parameters (default is the upper left corner of the
  // screen).
  private void processAnsiCommand_H() {
    moveCursor(getAnsiParameter(0) - 1, getAnsiParameter(1) - 1);
  }

  // Deletes some (or all) of the text on the screen without moving the cursor.
  private void processAnsiCommand_J() {
    int parameter;
    if (parameters[0].length() == 0) {
      parameter = 0;
    } else {
      parameter = getAnsiParameter(0);
    }
    switch (parameter) {
    case 0:
      text.eraseToEndOfScreen();
      break;
    case 1:
      // Erase from beginning to current position (inclusive).
      text.eraseToCursor();
      break;
    case 2:
      // Erase entire display.
      text.eraseAll();
      break;
    default:
      Logger.log("Unexpected J-command parameter: " + parameter);
      break;
    }
  }

  // Deletes some (or all) of the text in the current line without moving the cursor.
  private void processAnsiCommand_K() {
    int parameter = getAnsiParameter(0);
    switch (parameter) {
    case 0:
      // Erase from beginning to current position (inclusive).
      text.eraseLineToCursor();
      break;
    case 1:
      // Erase from current position to end (inclusive).
      text.eraseLineToEnd();
      break;
    case 2:
      // Erase entire line.
      text.eraseLine();
      break;
    default:
      Logger.log("Unexpected K-command parameter: " + parameter);
      break;
    }
  }

  // Inserts one or more blank lines. The current line of text moves down. Text that falls off the bottom of the screen
  // is deleted.
  private void processAnsiCommand_L() {
    text.insertLines(getAnsiParameter(0));
  }

  // Deletes one or more lines of text. Any lines below the deleted lines move up, which we implement by appending
  // new lines to the end of the text.
  private void processAnsiCommand_M() {
    text.deleteLines(getAnsiParameter(0));
  }

  // Sets a new graphics rendition mode, such as foreground/background color, bold/normal text, and reverse video.
  private void processAnsiCommand_m() {
    if (parameters[0].length() == 0) {
      // This a special case: when no ANSI parameter is specified, act like a single parameter equal to 0 was specified.
      parameters[0].append('0');
    }
    Style style = text.getStyle();
    // There are a non-zero number of ANSI parameters. Process each one in order.
    int parameterCount = parameters.length;
    int parameterIndex = 0;
    while (parameterIndex < parameterCount && parameters[parameterIndex].length() > 0) {
      int parameter = getAnsiParameter(parameterIndex);
      switch (parameter) {
      case 0:
        // Reset all graphics modes.
        style = text.getDefaultStyle();
        break;
      case 1:
        style = style.setBold(true);
        break;
      case 4:
        style = style.setUnderline(true);
        break;
      case 5:
        style = style.setBlink(true);
        break;
      case 7:
        style = style.setReverse(true);
        break;
      case 10: // Set primary font. Ignored.
        break;
      case 21:
      case 22:
        style = style.setBold(false);
        break;
      case 24:
        style = style.setUnderline(false);
        break;
      case 25:
        style = style.setBlink(false);
        break;
      case 27:
        style = style.setReverse(false);
        break;
      case 30:
        style = style.setForground("BLACK");
        break;
      case 31:
        style = style.setForground("RED");
        break;
      case 32:
        style = style.setForground("GREEN");
        break;
      case 33:
        style = style.setForground("YELLOW");
        break;
      case 34:
        style = style.setForground("BLUE");
        break;
      case 35:
        style = style.setForground("MAGENTA");
        break;
      case 36:
        style = style.setForground("CYAN");
        break;
      case 37:
        style = style.setForground("WHITE_FOREGROUND");
        break;
      case 40:
        style = style.setBackground("BLACK");
        break;
      case 41:
        style = style.setBackground("RED");
        break;
      case 42:
        style = style.setBackground("GREEN");
        break;
      case 43:
        style = style.setBackground("YELLOW");
        break;
      case 44:
        style = style.setBackground("BLUE");
        break;
      case 45:
        style = style.setBackground("MAGENTA");
        break;
      case 46:
        style = style.setBackground("CYAN");
        break;
      case 47:
        style = style.setBackground("WHITE");
        break;
      default:
        Logger.log("Unsupported graphics rendition parameter: " + parameter);
        break;
      }
      ++parameterIndex;
    }
    text.setStyle(style);
  }

  // Responds to an ANSI Device Status Report (DSR) command from the remote endpoint requesting the cursor position.
  // Requests for other kinds of status are ignored.
  private void processAnsiCommand_n() {
    // Do nothing if the numeric parameter was not 6 (which means report cursor position).
    if (getAnsiParameter(0) != 6) {
      return;
    }
    // Send the ANSI cursor position (which is 1-based) to the remote endpoint.
    String positionReport = "\u001b[" + (relativeCursorLine() + 1) + ";" + (getCursorColumn() + 1) + "R";
    try {
      // TODO(alruiz): use same encoding as terminal.
      OutputStreamWriter streamWriter = new OutputStreamWriter(terminal.getOutputStream(), "ISO-8859-1");
      streamWriter.write(positionReport, 0, positionReport.length());
      streamWriter.flush();
    } catch (IOException ex) {
      Logger.log("Caught IOException!");
    }
  }

  // Deletes one or more characters starting at the current cursor position. Characters on the same line and to the
  // right of the deleted characters move left. If there are no characters on the current line at or to the right of the
  // cursor column, no text is deleted.
  private void processAnsiCommand_P() {
    text.deleteCharacters(getAnsiParameter(0));
  }

  // Returns one of the numeric ANSI parameters received in the most recent escape sequence.
  private int getAnsiParameter(int parameterIndex) {
    if (parameterIndex < 0 || parameterIndex >= parameters.length) {
      // This should never happen.
      return -1;
    }
    String parameter = parameters[parameterIndex].toString();
    if (parameter.length() == 0) {
      return 1;
    }
    int parameterValue = 1;
    // Don't trust the remote endpoint to send well formed numeric parameters.
    try {
      parameterValue = Integer.parseInt(parameter);
    } catch (NumberFormatException ex) {
      parameterValue = 1;
    }
    return parameterValue;
  }

  // Processes a single parameter character in an ANSI escape sequence. Parameters are the (optional) characters
  // between the leading "\e[" and the command character in an escape sequence (e.g., in the escape sequence
  // "\e[20;10H", the parameter characters are "20;10"). Parameters are integers separated by one or more ';'s.
  private void processAnsiParameterCharacter(char ch) {
    if (ch == ';') {
      ++nextAnsiParameter;
    } else {
      if (nextAnsiParameter < parameters.length) {
        parameters[nextAnsiParameter].append(ch);
      }
    }
  }

  // Processes a contiguous sequence of non-control characters. This is a performance optimization, so that we don't
  // have to insert or append each non-control character individually to the StyledText widget. A non-control character
  // is any character that passes the condition in the below while loop.
  private void processNonControlCharacters(char character) throws IOException {
    StringBuilder buffer = new StringBuilder();
    buffer.append(character);
    // Identify a contiguous sequence of non-control characters, starting at firstNonControlCharacterIndex in newText.
    while (hasNextChar()) {
      character = getNextChar();
      if (character == '\u0000' || character == '\b' || character == '\t' || character == '\u0007' || character == '\n'
          || character == '\r' || character == '\u001b') {
        pushBackChar(character);
        break;
      }
      buffer.append(character);
    }
    // Now insert the sequence of non-control characters in the StyledText widget
    // at the location of the cursor.
    displayNewText(buffer.toString());
  }

  // Displays a subset of the newly-received text in the Terminal view, wrapping text at the right edge of the screen
  // and overwriting text when the cursor is not at the very end of the screen's text.
  // There are never any ANSI control characters or escape sequences in the text being displayed by this method (this
  // includes newlines, carriage returns, and tabs).
  private void displayNewText(String buffer) {
    text.appendString(buffer);
  }

  // Processes a BEL (Control-G) character.
  private void processBEL() {
    // TODO
    // Display.getDefault().beep();
  }

  // Processes a backspace (Control-H) character.
  private void processBackspace() {
    moveCursorBackward(1);
  }

  // Processes a tab (Control-I) character. We don't insert a tab character into the StyledText widget. Instead, we move
  // the cursor forward to the next tab stop, without altering any of the text. Tab stops are every 8 columns. The
  // cursor will never move past the rightmost column.
  private void processTab() {
    moveCursorForward(8 - (getCursorColumn() % 8));
  }

  // Processes a newline (Control-J) character. A newline (NL) character just moves the cursor to the same column on the
  // next line, creating new lines when the cursor reaches the bottom edge of the terminal. This is counter-intuitive,
  // especially to UNIX programmers who are taught that writing a single NL to a terminal is sufficient to move the
  // cursor to the first column of the next line, as if a carriage return (CR) and a NL were written.
  //
  // UNIX terminals typically display a NL character as a CR followed by a NL because the terminal device typically has
  // the ONLCR attribute bit set (see the termios(4) man page for details), which causes the terminal device driver to
  // translate NL to CR + NL on output. The terminal itself (i.e., a hardware terminal or a terminal emulator, like
  // xterm or this code) _always_ interprets a CR to mean "move the cursor to the beginning of the current
  // line" and a NL to mean "move the cursor to the same column on the next line".
  private void processNewline() {
    text.processNewline();
  }

  // Processes a Carriage Return (Control-M).
  private void processCarriageReturn() {
    text.setCursorColumn(0);
  }

  // Computes the width of the terminal in columns and its height in lines, then adjusts the width and height of the
  // view's StyledText widget so that it displays an integral number of lines and columns of text. The adjustment is
  // always to shrink the widget vertically or horizontally, because if the control were to grow, it would be clipped by
  // the edges of the view window (i.e., the view window does not become larger to accommodate its contents becoming
  // larger).
  //
  // This method must be called immediately before each time text is written to the terminal so that we can properly
  // line wrap text. Because it is called so frequently, it must be fast when there is no resizing to be done.
  private void adjustTerminalDimensions() {
    // Compute how many pixels we need to shrink the StyledText control vertically to make it display an integral number
    // of lines of text.
    //
    // TODO
    // if(text.getColumns()!=80 && text.getLines()!=80) text.setDimensions(24, 80);
    ITerminalConnector connector = getConnector();
    // TODO MSA: send only if dimensions have really changed!
    if (connector != null) {
      connector.setTerminalSize(text.getColumns(), text.getLines());
    }
  }

  private ITerminalConnector getConnector() {
    if (terminal.getTerminalConnector() != null) {
      return terminal.getTerminalConnector();
    }
    return null;
  }

  // Returns the relative line number of the line containing the cursor. The returned line number is relative to the
  // top-most visible line, which has relative line number 0.
  private int relativeCursorLine() {
    return text.getCursorLine();
  }

  // Moves the cursor to the specified line and column. Parameter <i>targetLine</i> is the line number of a screen line,
  // so it has a minimum value of 0 (the topmost screen line) and a maximum value of heightInLines - 1 (the
  // bottom-most screen line). A line does not have to contain any text to move the cursor to any column in that line.
  private void moveCursor(int targetLine, int targetColumn) {
    text.setCursor(targetLine, targetColumn);
  }

  // Moves the cursor down n lines, but won't move the cursor past the bottom of the screen. This method does not cause
  // any scrolling.
  private void moveCursorDown(int lines) {
    moveCursor(relativeCursorLine() + lines, getCursorColumn());
  }

  // Moves the cursor up n lines, but won't move the cursor past the top of the screen. This method does not cause any
  // scrolling.
  private void moveCursorUp(int lines) {
    moveCursor(relativeCursorLine() - lines, getCursorColumn());
  }

  // Method moves the cursor forward n columns, but won't move the cursor past the right edge of the screen, nor will
  // it move the cursor onto the next line. This method does not cause any scrolling.
  private void moveCursorForward(int columnsToMove) {
    moveCursor(relativeCursorLine(), getCursorColumn() + columnsToMove);
  }

  // Moves the cursor backward n columns, but won't move the cursor past the left edge of the screen, nor will it move
  // the cursor onto the previous line. This method does not cause any scrolling.
  private void moveCursorBackward(int columnsToMove) {
    moveCursor(relativeCursorLine(), getCursorColumn() - columnsToMove);
  }

  /**
   * Resets the state of the terminal text (foreground color, background color, font style and other internal state). It
   * essentially makes it ready for new input.
   */
  public void resetState() {
    ansiState = ANSISTATE_INITIAL;
    text.setStyle(text.getDefaultStyle());
  }

  private char getNextChar() throws IOException {
    int c = -1;
    if (nextChar != -1) {
      c = nextChar;
      nextChar = -1;
    } else {
      c = reader.read();
    }
    // TODO: better end of file handling
    if (c == -1) {
      c = 0;
    }
    return (char) c;
  }

  private boolean hasNextChar() throws IOException {
    if (nextChar >= 0) {
      return true;
    }
    return reader.ready();
  }

  /**
   * Put back one character to the stream. This method can push back exactly one character. The character is the next
   * character returned by {@link #getNextChar}.
   *
   * @param c the character to be pushed back.
   */
  private void pushBackChar(char c) {
    nextChar = c;
  }

  private int getCursorColumn() {
    return text.getCursorColumn();
  }

  public boolean isCrAfterNewLine() {
    return crAfterNewLine;
  }

  public void setCrAfterNewLine(boolean crAfterNewLine) {
    this.crAfterNewLine = crAfterNewLine;
  }

  public List<IHyperlink> hyperlinksAt(int line) {
    return text.hyperlinksAt(line);
  }
}
