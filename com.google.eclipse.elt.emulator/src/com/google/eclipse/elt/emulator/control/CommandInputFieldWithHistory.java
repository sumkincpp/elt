/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.elt.emulator.control;

import static java.util.Arrays.asList;

import java.util.*;
import java.util.List;

import org.eclipse.jface.fieldassist.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

/**
 * Manages the Command History for the command line input of the terminal control.
 *
 * <p>
 * <ul>
 * <li>Navigate with ARROW_UP,ARROW_DOWN,PAGE_UP,PAGE_DOWN</li>
 * <li>ESC to cancel history editing</li>
 * <li>History can be edited (by moving up and edit) but changes are not persistent (like in bash)</li>
 * <li>If the same command is entered multiple times in a row, only one entry is kept in the history</li>
 * </ul>
 * </p>
 */
public class CommandInputFieldWithHistory implements ICommandInputField {
  final List<String> allHistory = new ArrayList<String>();

  // Keeps a modifiable history while in history editing mode.
  List<String> editedHistory;

  // The current position in the edit history.
  private int editHistoryPosition = 0;

  // The limit of the history.
  private final int maxHistorySize;

  private Text inputField;
  private Sash sash;

  public CommandInputFieldWithHistory(int maxHistorySize) {
    this.maxHistorySize = maxHistorySize;
  }

  /**
   * Add a line to the history.
   * @param line The line to be added to the history.
   */
  protected void pushLine(String line) {
    endHistoryMode();
    // Anything to remember?
    if (line == null || line.trim().length() == 0) {
      return;
    }
    allHistory.add(0, line);
    // Ignore if the same as last/
    if (allHistory.size() > 1 && line.equals(allHistory.get(1))) {
      allHistory.remove(0);
    }
    // Limit the history size.
    if (allHistory.size() >= maxHistorySize) {
      allHistory.remove(allHistory.size() - 1);
    }
  }

  public void setHistory(String history) {
    endHistoryMode();
    allHistory.clear();
    if (history == null) {
      return;
    }
    allHistory.addAll(asList(history.split("\n")));
  }

  public String getHistory() {
    StringBuilder buffer = new StringBuilder();
    boolean separate = false;
    for (String line : allHistory) {
      if (line.length() > 0) {
        if (separate) {
          buffer.append("\n");
        } else {
          separate = true;
        }
        buffer.append(line);
      }
    }
    return buffer.toString();
  }

  /**
   * Moves a line of text in history.
   * @param lineToBeMoved the line of text to be moved.
   * @param count (+1 or -1) for forward and backward movement (-1 goes back.)
   * @return the new string to be displayed in the command line or {@code null}, if the limit is reached.
   */
  public String move(String lineToBeMoved, int count) {
    if (!inHistoryMode()) {
      editedHistory = new ArrayList<String>(allHistory.size() + 1);
      editedHistory.add(lineToBeMoved);
      editedHistory.addAll(allHistory);
      editHistoryPosition = 0;
    }
    editedHistory.set(editHistoryPosition, lineToBeMoved);
    if (editHistoryPosition + count >= editedHistory.size()) {
      return null;
    }
    if (editHistoryPosition + count < 0) {
      return null;
    }
    editHistoryPosition += count;
    return editedHistory.get(editHistoryPosition);
  }

  private boolean inHistoryMode() {
    return editedHistory != null;
  }

  /**
   * Exit the history movements and go to position 0;
   * @return the string to be shown in the command line
   */
  protected String escape() {
    if (!inHistoryMode()) {
      return null;
    }
    String line = editedHistory.get(0);
    endHistoryMode();
    return line;
  }

  private void endHistoryMode() {
    editedHistory = null;
    editHistoryPosition = 0;
  }

  @Override public void createControl(final Composite parent, final ITerminalViewControl terminal) {
    sash = new Sash(parent, SWT.HORIZONTAL);
    final GridData sashLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    sashLayoutData.heightHint = 5;
    sash.setLayoutData(sashLayoutData);
    sash.addListener(SWT.Selection, new Listener() {
      @Override public void handleEvent(Event e) {
        if (e.detail == SWT.DRAG) {
          // Don't redraw during drag, it causes paint errors - bug 220971.
          return;
        }
        // No idea why this is needed.
        GridData inputFieldLayoutData = (GridData) inputField.getLayoutData();
        Rectangle sashRect = sash.getBounds();
        Rectangle containerRect = parent.getClientArea();

        int h = inputField.getLineHeight();
        // Make sure the input filed height is a multiple of the line height.
        inputFieldLayoutData.heightHint = Math.max(((containerRect.height - e.y - sashRect.height) / h) * h, h);
        // Do not show less then one line.
        e.y = Math.min(e.y, containerRect.height - h);
        inputField.setLayoutData(inputFieldLayoutData);
        parent.layout();
        // else the content assist icon will be replicated
        parent.redraw();
      }
    });
    inputField = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    GridData inputFieldLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    boolean installDecoration = true;
    if (installDecoration) {
      // The ContentAssistCommandAdapter says: "The client is responsible for ensuring that adequate space is reserved
      // for the decoration."
      // TODO: What is the "adequate space"?
      inputFieldLayoutData.horizontalIndent = 6;
    }
    inputField.setLayoutData(inputFieldLayoutData);
    inputField.setFont(terminal.getFont());
    // Register field assist *before* the key listener.
    // Otherwise the ENTER key is sent *first* to the input field and then to the field assist pop-up.
    // (https://bugs.eclipse.org/bugs/show_bug.cgi?id=211659)
    new ContentAssistCommandAdapter(
        inputField, new TextContentAdapter(), new FieldAssist(), null, null, installDecoration);
    inputField.addKeyListener(new KeyListener() {
      @Override public void keyPressed(KeyEvent e) {
        // If the field assist has handled the key already then ignore it.
        // (https://bugs.eclipse.org/bugs/show_bug.cgi?id=211659)
        if (!e.doit) {
          return;
        }
        if (e.keyCode == '\n' || e.keyCode == '\r') {
          e.doit = false;
          String line = inputField.getText();
          if (!terminal.pasteString(line + "\n")) {
            return;
          }
          pushLine(line);
          setCommand("");
        }
        if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.PAGE_UP) {
          e.doit = false;
          setCommand(move(inputField.getText(), 1));
        }
        if (e.keyCode == SWT.ARROW_DOWN || e.keyCode == SWT.PAGE_DOWN) {
          e.doit = false;
          setCommand(move(inputField.getText(), -1));
        }
        if (e.keyCode == SWT.ESC) {
          e.doit = false;
          setCommand(escape());
        }
      }

      private void setCommand(String line) {
        if (line == null) {
          return;
        }
        inputField.setText(line);
        inputField.setSelection(inputField.getCharCount());
      }

      @Override public void keyReleased(KeyEvent e) {
      }
    });
  }

  @Override public void setFont(Font font) {
    inputField.setFont(font);
    inputField.getParent().layout(true);
  }

  @Override public void dispose() {
    sash.dispose();
    sash = null;
    inputField.dispose();
    inputField = null;
  }

  private class FieldAssist implements IContentProposalProvider {
    @Override public IContentProposal[] getProposals(String contents, int position) {
      String prefix = contents.substring(0, position);
      List<IContentProposal> result = new ArrayList<IContentProposal>();
      Collection<String> seen = new HashSet<String>();
      for (String history : allHistory) {
        if (history.startsWith(prefix) && !seen.contains(history)) {
          // The content is the rest of the history item.
          String content = history.substring(prefix.length());
          result.add(new Proposal(content, history));
          // Don't add this proposal again.
          seen.add(history);
        }
      }
      return result.toArray(new IContentProposal[result.size()]);
    }
  }

  private static class Proposal implements IContentProposal {
    private final String content;
    private final String label;

    Proposal(String content, String label) {
      this.content = content;
      this.label = label;
    }

    @Override public String getContent() {
      return content;
    }

    @Override public int getCursorPosition() {
      return content.length();
    }

    @Override public String getDescription() {
      return null;
    }

    @Override public String getLabel() {
      return label;
    }
  }
}