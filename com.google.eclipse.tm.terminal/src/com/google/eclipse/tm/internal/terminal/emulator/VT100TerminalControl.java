/*******************************************************************************
 * Copyright (c) 2003, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.emulator;

import static org.eclipse.core.runtime.Status.OK_STATUS;
import static org.eclipse.jface.bindings.keys.SWTKeySupport.convertEventToUnmodifiedAccelerator;

import static com.google.eclipse.tm.internal.terminal.control.impl.TerminalPlugin.isOptionEnabled;

import java.io.*;
import java.net.SocketException;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.*;
import org.eclipse.ui.keys.IBindingService;

import com.google.eclipse.tm.internal.terminal.control.*;
import com.google.eclipse.tm.internal.terminal.control.impl.*;
import com.google.eclipse.tm.internal.terminal.provisional.api.*;
import com.google.eclipse.tm.internal.terminal.textcanvas.*;
import com.google.eclipse.tm.internal.terminal.textcanvas.PipedInputStream;
import com.google.eclipse.tm.terminal.model.*;

public class VT100TerminalControl implements ITerminalControlForText, ITerminalControl, ITerminalViewControl {
  protected static final String[] LINE_DELIMITERS = { "\n" };

  private static final String DEFAULT_ENCODING =
      new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();

  // This field holds a reference to a TerminalText object that performs all ANSI text processing on data received from
  // the remote host and controls how text is displayed using the view's StyledText widget.
  private final VT100Emulator terminalText;

  private Display display;
  private TextCanvas textControl;
  private Composite rootControl;
  private Clipboard clipboard;
  private KeyListener keyHandler;
  private final ITerminalListener terminalListener;
  private String message = "";
  private FocusListener fFocusListener;
  private ITerminalConnector connector;
  private final ITerminalConnector[] connectors;
  private final PipedInputStream inputStream;
  private String encoding = DEFAULT_ENCODING;
  private InputStreamReader inputStreamReader;
  private ICommandInputField commandInputField;
  private volatile TerminalState state;
  private final ITerminalTextData terminalModel;

  volatile private Job job;

  private final EditActionAccelerators editActionAccelerators = new EditActionAccelerators();

  public VT100TerminalControl(ITerminalListener target, Composite wndParent, ITerminalConnector[] connectors) {
    this.connectors = connectors;
    terminalListener = target;
    terminalModel = TerminalTextDataFactory.makeTerminalTextData();
    terminalModel.setMaxHeight(1000);
    inputStream = new PipedInputStream(8 * 1024);
    terminalText = new VT100Emulator(terminalModel, this, null);
    try {
      // Use default Encoding as start, until setEncoding() is called.
      setEncoding(null);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      // Fall back to local platform default encoding
      encoding = DEFAULT_ENCODING;
      inputStreamReader = new InputStreamReader(inputStream);
      terminalText.setInputStreamReader(inputStreamReader);
    }
    setUpTerminal(wndParent);
  }

  @Override public void setEncoding(String encoding) throws UnsupportedEncodingException {
    if (encoding == null) {
      encoding = "ISO-8859-1";
    }
    inputStreamReader = new InputStreamReader(inputStream, encoding);
    // remember encoding if above didn't throw an exception
    this.encoding = encoding;
    terminalText.setInputStreamReader(inputStreamReader);
  }

  @Override public String getEncoding() {
    return encoding;
  }

  @Override public ITerminalConnector[] getConnectors() {
    return connectors;
  }

  @Override public void copy() {
    copy(DND.CLIPBOARD);
  }

  private void copy(int clipboardType) {
    String selection = getSelection();
    if (selection == null || selection.isEmpty()) {
      return;
    }
    Object[] data = new Object[] { selection };
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    clipboard.setContents(data, types, clipboardType);
  }

  @Override public void paste() {
    paste(DND.CLIPBOARD);
  }

  private void paste(int clipboardType) {
    TextTransfer textTransfer = TextTransfer.getInstance();
    String strText = (String) clipboard.getContents(textTransfer, clipboardType);
    pasteString(strText);
  }

  @Override public boolean pasteString(String text) {
    if (!isConnected()) {
      return false;
    }
    if (text == null) {
      return false;
    }
    if (!encoding.equals(DEFAULT_ENCODING)) {
      sendString(text);
    } else {
      // TODO I do not understand why pasteString would do this here...
      for (int i = 0; i < text.length(); i++) {
        sendChar(text.charAt(i), false);
      }
    }
    return true;
  }

  @Override public void selectAll() {
    getTextControl().selectAll();
  }

  @Override public void sendKey(char character) {
    Event event;
    KeyEvent keyEvent;
    event = new Event();
    event.widget = getTextControl();
    event.character = character;
    event.keyCode = 0;
    event.stateMask = 0;
    event.doit = true;
    keyEvent = new KeyEvent(event);
    keyHandler.keyPressed(keyEvent);
  }

  @Override public void clearTerminal() {
    getTerminalText().clearTerminal();
  }

  @Override public Clipboard getClipboard() {
    return clipboard;
  }

  @Override public String getSelection() {
    String text = textControl.getSelectionText();
    return text == null ? "" : text;
  }

  @Override public boolean setFocus() {
    return getTextControl().setFocus();
  }

  @Override public boolean isEmpty() {
    return getTextControl().isEmpty();
  }

  @Override public boolean isDisposed() {
    return getTextControl().isDisposed();
  }

  @Override public boolean isConnected() {
    return state == TerminalState.CONNECTED;
  }

  @Override public void disposeTerminal() {
    disconnectTerminal();
    clipboard.dispose();
    getTerminalText().dispose();
  }

  @Override public void connectTerminal() {
    if (getTerminalConnector() == null) {
      return;
    }
    terminalText.resetState();
    if (connector.getInitializationErrorMessage() != null) {
      showErrorMessage(NLS.bind(TerminalMessages.cannotConnectTo, connector.getName(),
          connector.getInitializationErrorMessage()));
      return;
    }
    getTerminalConnector().connect(this);
    // clean the error message
    setErrorMessage("");
    waitForConnect();
  }

  @Override public ITerminalConnector getTerminalConnector() {
    return connector;
  }

  @Override public void disconnectTerminal() {
    Logger.log("entered.");
    // Disconnect the remote side first.
    if (getState() != TerminalState.CLOSED && getTerminalConnector() != null) {
      getTerminalConnector().disconnect();
    }
    // Ensure that a new Job can be started; then clean up old Job.
    Job newJob;
    synchronized (this) {
      newJob = job;
      job = null;
    }
    if (newJob != null) {
      newJob.cancel();
      // Join job to avoid leaving job running after workbench shutdown (333613).
      // Interrupt to be fast enough; cannot close fInputStream since it is re-used (bug 348700).
      Thread t = newJob.getThread();
      if (t != null) {
        t.interrupt();
      }
      try {
        newJob.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void waitForConnect() {
    // TODO
    // Eliminate this code
    while (getState() == TerminalState.CONNECTING) {
      if (display.readAndDispatch()) {
        continue;
      }
      display.sleep();
    }
    if (getTextControl().isDisposed()) {
      disconnectTerminal();
      return;
    }
    if (!getMsg().isEmpty()) {
      showErrorMessage(getMsg());
      disconnectTerminal();
      return;
    }
    getTextControl().setFocus();
    startReaderJob();
  }

  private synchronized void startReaderJob() {
    if (job == null) {
      initializeJob();
      job.setSystem(true);
      job.schedule();
    }
  }

  private void initializeJob() {
    job = new Job("Terminal data reader") {
      @Override protected IStatus run(IProgressMonitor monitor) {
        IStatus status = OK_STATUS;
        try {
          while (true) {
            while (inputStream.available() == 0 && !monitor.isCanceled()) {
              try {
                inputStream.waitForAvailable(500);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            if (monitor.isCanceled()) {
              // Do not disconnect terminal here because another reader job may already be running.
              status = Status.CANCEL_STATUS;
              break;
            }
            try {
              // TODO: should block when no text is available!
              terminalText.processText();
            } catch (Exception e) {
              disconnectTerminal();
              status = new Status(IStatus.ERROR, TerminalPlugin.PLUGIN_ID, e.getLocalizedMessage(), e);
              break;
            }
          }
        } finally {
          // Clean the job: start a new one when the connection gets restarted.
          // Bug 208145: make sure we do not clean an other job that's already started (since it would become a zombie)
          synchronized (VT100TerminalControl.this) {
            if (job == this) {
              job = null;
            }
          }
        }
        return status;
      }
    };
  }

  private void showErrorMessage(String message) {
    String title = TerminalMessages.terminalError;
    MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
    mb.setText(title);
    mb.setMessage(message);
    mb.open();
  }

  protected void sendString(String string) {
    try {
      // Send the string after converting it to an array of bytes using the platform's default character encoding.
      //
      // TODO: Find a way to force this to use the ISO Latin-1 encoding.
      // TODO: handle Encoding Errors in a better way
      getOutputStream().write(string.getBytes(encoding));
      getOutputStream().flush();
    } catch (SocketException socketException) {
      displayTextInTerminal(socketException.getMessage());
      String strMsg = TerminalMessages.socketError + "!\n" + socketException.getMessage();
      showErrorMessage(strMsg);
      Logger.logException(socketException);
      disconnectTerminal();
    } catch (IOException ioException) {
      showErrorMessage(TerminalMessages.ioError + "!\n" + ioException.getMessage());
      Logger.logException(ioException);
      disconnectTerminal();
    }
  }

  @Override public Shell getShell() {
    return getTextControl().getShell();
  }

  protected void sendChar(char chKey, boolean altKeyPressed) {
    try {
      String text = Character.toString(chKey);
      byte[] bytes = text.getBytes(getEncoding());
      OutputStream os = getOutputStream();
      if (os == null) {
        // Bug 207785: NPE when trying to send char while no longer connected
        Logger.log("NOT sending '" + text + "' because no longer connected");
      } else {
        if (altKeyPressed) {
          // When the ALT key is pressed at the same time that a character is typed, translate it into an ESCAPE
          // followed by the character. The alternative in this case is to set the high bit of the character being
          // transmitted, but that will cause input such as ALT-f to be seen as the ISO Latin-1 character '�', which
          // can be confusing to European users running Emacs, for whom Alt-f should move forward a word instead of
          // inserting the '�' character.
          //
          // TODO: Make the ESCAPE-vs-highbit behavior user configurable.
          Logger.log("sending ESC + '" + text + "'");
          getOutputStream().write('\u001b');
          getOutputStream().write(bytes);
        } else {
          Logger.log("sending '" + text + "'");
          getOutputStream().write(bytes);
        }
        getOutputStream().flush();
      }
    } catch (SocketException socketException) {
      Logger.logException(socketException);
      displayTextInTerminal(socketException.getMessage());
      String message = TerminalMessages.socketError + "!\n" + socketException.getMessage();
      showErrorMessage(message);
      Logger.logException(socketException);
      disconnectTerminal();
    } catch (IOException ioException) {
      Logger.logException(ioException);
      displayTextInTerminal(ioException.getMessage());
      String message = TerminalMessages.ioError + "!\n" + ioException.getMessage();
      showErrorMessage(message);
      Logger.logException(ioException);
      disconnectTerminal();
    }
  }

  @Override public void setUpTerminal(Composite parent) {
    Assert.isNotNull(parent);
    state = TerminalState.CLOSED;
    setUpControls(parent);
    setupListeners();
    setupHelp(rootControl, TerminalPlugin.HELP_VIEW);
  }

  @Override public void setFont(Font font) {
    getTextControl().setFont(font);
    if (commandInputField != null) {
      commandInputField.setFont(font);
    }
    // Tell the TerminalControl singleton that the font has changed.
    textControl.onFontChange();
    getTerminalText().fontChanged();
  }

  @Override public Font getFont() {
    return getTextControl().getFont();
  }

  @Override public Control getControl() {
    return textControl;
  }

  @Override public Control getRootControl() {
    return rootControl;
  }

  protected void setUpControls(Composite parent) {
    // The Terminal view now aims to be an ANSI-conforming terminal emulator, so it can't have a horizontal scroll bar
    // (but a vertical one is ok). Also, do _not_ make the TextViewer read-only, because that prevents it from seeing a
    // TAB character when the user presses TAB (instead, the TAB causes focus to switch to another Workbench control).
    // We prevent local keyboard input from modifying the text in method TerminalVerifyKeyListener.verifyKey().
    rootControl = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 0;
    rootControl.setLayout(layout);
    ITerminalTextDataSnapshot snapshot = terminalModel.makeSnapshot();
    // TODO how to get the initial size correctly!
    snapshot.updateSnapshot(false);
    ITextCanvasModel canvasModel = new PollingTextCanvasModel(snapshot);
    textControl = new TextCanvas(rootControl, canvasModel, SWT.NONE, new TextLineRenderer(textControl, canvasModel));
    textControl.addMouseListener(new MouseAdapter() {
      @Override public void mouseUp(MouseEvent e) {
        IHyperlink hyperlink = hyperlinkAt(e);
        if (hyperlink != null) {
          hyperlink.open();
        }
      }
    });
    textControl.addMouseMoveListener(new MouseMoveListener() {
      @Override public void mouseMove(MouseEvent e) {
        IHyperlink hyperlink = hyperlinkAt(e);
        int cursorId = (hyperlink == null) ? SWT.CURSOR_IBEAM : SWT.CURSOR_HAND;
        Cursor newCursor = textControl.getDisplay().getSystemCursor(cursorId);
        if (!newCursor.equals(textControl.getCursor())) {
          textControl.setCursor(newCursor);
        }
      }
    });
    textControl.setLayoutData(new GridData(GridData.FILL_BOTH));
    textControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    textControl.addResizeHandler(new TextCanvas.ResizeListener() {
      @Override public void sizeChanged(int lines, int columns) {
        terminalText.setDimensions(lines, columns);
      }
    });
    textControl.addMouseListener(new MouseAdapter() {
      @Override public void mouseUp(MouseEvent e) {
        // update selection used by middle mouse button paste
        if (e.button == 1 && getSelection().length() > 0) {
          copy(DND.SELECTION_CLIPBOARD);
        }
      }
    });
    display = getTextControl().getDisplay();
    clipboard = new Clipboard(display);
    setFont(JFaceResources.getTextFont());
  }

  private IHyperlink hyperlinkAt(MouseEvent e) {
    Point p = textControl.screenPointToCell(e.x, e.y);
    if (p != null) {
      List<IHyperlink> hyperlinks = terminalText.hyperlinksAt(p.y);
      for (IHyperlink hyperlink : hyperlinks) {
        IRegion region = hyperlink.getHyperlinkRegion();
        int start = region.getOffset();
        int end = start + region.getLength() - 1;
        if (p.x >= start && p.x <= end) {
          return hyperlink;
        }
      }
    }
    return null;
  }

  protected void setupListeners() {
    keyHandler = new TerminalKeyHandler();
    fFocusListener = new TerminalFocusListener();
    getTextControl().addKeyListener(keyHandler);
    getTextControl().addFocusListener(fFocusListener);
  }

  /**
   * Setup all the help contexts for the controls.
   */
  protected void setupHelp(Composite parent, String id) {
    Control[] children = parent.getChildren();
    for (int nIndex = 0; nIndex < children.length; nIndex++) {
      if (children[nIndex] instanceof Composite) {
        setupHelp((Composite) children[nIndex], id);
      }
    }
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, id);
  }

  @Override public void displayTextInTerminal(String text) {
    writeToTerminal("\r\n" + text + "\r\n");
  }

  private void writeToTerminal(String text) {
    try {
      getRemoteToTerminalOutputStream().write(text.getBytes(encoding));
    } catch (UnsupportedEncodingException e) {
      // should never happen!
      e.printStackTrace();
    } catch (IOException e) {
      // should never happen!
      e.printStackTrace();
    }
  }

  @Override public OutputStream getRemoteToTerminalOutputStream() {
    if (Logger.isLogEnabled()) {
      return new LoggingOutputStream(inputStream.getOutputStream());
    }
    return inputStream.getOutputStream();
  }

  protected boolean isLogCharEnabled() {
    return isOptionEnabled(Logger.TRACE_DEBUG_LOG_CHAR);
  }

  protected boolean isLogBufferSizeEnabled() {
    return isOptionEnabled(Logger.TRACE_DEBUG_LOG_BUFFER_SIZE);
  }

  @Override public OutputStream getOutputStream() {
    if (getTerminalConnector() != null) {
      return getTerminalConnector().getTerminalToRemoteStream();
    }
    return null;
  }

  @Override public void setErrorMessage(String message) {
    this.message = message;
  }

  public String getMsg() {
    return message;
  }

  protected TextCanvas getTextControl() {
    return textControl;
  }

  public VT100Emulator getTerminalText() {
    return terminalText;
  }

  protected class TerminalFocusListener implements FocusListener {
    private IContextActivation contextActivation = null;

    @Override public void focusGained(FocusEvent event) {
      // Disable all keyboard accelerators (e.g., Control-B) so the Terminal view can see every key stroke. Without
      // this, Emacs, vi, and Bash are unusable in the terminal view.
      IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
      bindingService.setKeyFilterEnabled(false);
      // The above code fails to cause Eclipse to disable menu-activation accelerators (e.g., Alt-F for the File menu),
      // so we set the command context to be the Terminal view's command context. This enables us to override
      // menu-activation accelerators with no-op commands in our plugin.xml file, which enables the terminal view to see
      // absolutly _all_ key-presses.
      IContextService contextService = (IContextService) PlatformUI.getWorkbench().getAdapter(IContextService.class);
      contextActivation = contextService.activateContext("org.eclipse.tm.terminal.TerminalContext");
    }

    @Override public void focusLost(FocusEvent event) {
      // Enable all key bindings.
      IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
      bindingService.setKeyFilterEnabled(true);
      // Restore the command context to its previous value.
      IContextService contextService = (IContextService) PlatformUI.getWorkbench().getAdapter(IContextService.class);
      contextService.deactivateContext(contextActivation);
    }
  }

  protected class TerminalKeyHandler extends KeyAdapter {
    @Override public void keyPressed(KeyEvent event) {
      if (getState() == TerminalState.CONNECTING) {
        return;
      }
      int accelerator = convertEventToUnmodifiedAccelerator(event);
      if (editActionAccelerators.isCopyAction(accelerator)) {
        copy();
        return;
      }
      if (editActionAccelerators.isPasteAction(accelerator)) {
        paste();
        return;
      }
      // We set the event.doit to false to prevent any further processing of this key event. The only reason this is
      // here is because I was seeing the F10 key both send an escape sequence (due to this method) and switch focus to
      // the Workbench File menu (forcing the user to click in the terminal view again to continue entering text). This
      // fixes that.
      event.doit = false;
      char character = event.character;
      if (state == TerminalState.CLOSED) {
        // Pressing ENTER while not connected causes us to connect.
        if (character == '\r') {
          connectTerminal();
          return;
        }
        // Ignore all other keyboard input when not connected.
        // Allow other key handlers (such as Ctrl+F1) do their work.
        event.doit = true;
        return;
      }
      // Manage the Del key
      if (event.keyCode == 0x000007f) {
        sendString("\u001b[3~");
        return;
      }
      // If the event character is NUL ('\u0000'), then a special key was pressed (e.g., PageUp, PageDown, an arrow key,
      // a function key, Shift, Alt, Control, etc.). The one exception is when the user presses Control-@, which sends a
      // NUL character, in which case we must send the NUL to the remote endpoint. This is necessary so that Emacs will
      // work correctly, because Control-@ (i.e., NUL) invokes Emacs' set-mark-command when Emacs is running on a
      // terminal. When the user presses Control-@, the keyCode is 50.
      if (character == '\u0000' && event.keyCode != 50) {
        // A special key was pressed. Figure out which one it was and send the appropriate ANSI escape sequence.
        //
        // IMPORTANT: Control will not enter this method for these special keys unless certain <keybinding> tags are
        // present in the plugin.xml file for the Terminal view. Do not delete those tags.
        switch (event.keyCode) {
        case 0x1000001: // Up arrow.
          sendString("\u001b[A");
          break;
        case 0x1000002: // Down arrow.
          sendString("\u001b[B");
          break;
        case 0x1000003: // Left arrow.
          sendString("\u001b[D");
          break;
        case 0x1000004: // Right arrow.
          sendString("\u001b[C");
          break;
        case 0x1000005: // PgUp key.
          sendString("\u001b[5~");
          break;
        case 0x1000006: // PgDn key.
          sendString("\u001b[6~");
          break;
        case 0x1000007: // Home key.
          sendString("\u001b[H");
          break;
        case 0x1000008: // End key.
          sendString("\u001b[F");
          break;
        case 0x1000009: // Insert.
          sendString("\u001b[2~");
          break;
        case 0x100000a: // F1 key.
          if ((event.stateMask & SWT.CTRL) != 0) {
            // Allow Ctrl+F1 to act locally as well as on the remote, because it is typically non-intrusive
            event.doit = true;
          }
          sendString("\u001b[M");
          break;
        case 0x100000b: // F2 key.
          sendString("\u001b[N");
          break;
        case 0x100000c: // F3 key.
          sendString("\u001b[O");
          break;
        case 0x100000d: // F4 key.
          sendString("\u001b[P");
          break;
        case 0x100000e: // F5 key.
          sendString("\u001b[Q");
          break;
        case 0x100000f: // F6 key.
          sendString("\u001b[R");
          break;
        case 0x1000010: // F7 key.
          sendString("\u001b[S");
          break;
        case 0x1000011: // F8 key.
          sendString("\u001b[T");
          break;
        case 0x1000012: // F9 key.
          sendString("\u001b[U");
          break;
        case 0x1000013: // F10 key.
          sendString("\u001b[V");
          break;
        case 0x1000014: // F11 key.
          sendString("\u001b[W");
          break;
        case 0x1000015: // F12 key.
          sendString("\u001b[X");
          break;
        default:
          // Ignore other special keys. Control flows through this case when the user presses SHIFT, CONTROL, ALT, and
          // any other key not handled by the above cases.
          break;
        }
        // It's ok to return here, because we never locally echo special keys.
        return;
      }
      // To fix SPR 110341, we consider the Alt key to be pressed only when the Control key is _not_ also pressed. This
      // works around a bug in SWT where, on European keyboards, the AltGr key being pressed appears to us as Control
      // + Alt being pressed simultaneously.
      Logger.log("stateMask = " + event.stateMask);
      boolean altKeyPressed = (((event.stateMask & SWT.ALT) != 0) && ((event.stateMask & SWT.CTRL) == 0));
      if (!altKeyPressed && (event.stateMask & SWT.CTRL) != 0 && character == ' ') {
        // Send a NUL character -- many terminal emulators send NUL when Control-Space is pressed. This is used to set
        // the mark in Emacs.
        character = '\u0000';
      }
      sendChar(character, altKeyPressed);
      // Special case: When we are in a TCP connection and echoing characters locally, send a LF after sending a CR.
      // ISSUE: Is this absolutely required?
      if (character == '\r' && getTerminalConnector() != null && isConnected() && getTerminalConnector().isLocalEcho()) {
        sendChar('\n', false);
      }
      // Now decide if we should locally echo the character we just sent. We do _not_ locally echo the character if any
      // of these conditions are true:
      //
      // * This is a serial connection.
      // * This is a TCP connection (i.e., m_telnetConnection is not null) and the remote endpoint is not a TELNET
      //   server.
      // * The ALT (or META) key is pressed.
      // * The character is any of the first 32 ISO Latin-1 characters except Control-I or Control-M.
      // * The character is the DELETE character.
      if (getTerminalConnector() == null || getTerminalConnector().isLocalEcho() == false || altKeyPressed
          || (character >= '\u0001' && character < '\t') || (character > '\t' && character < '\r')
          || (character > '\r' && character <= '\u001f') || character == '\u007f') {
        // No local echoing.
        return;
      }
      // Locally echo the character.
      StringBuilder charBuffer = new StringBuilder();
      charBuffer.append(character);
      // If the character is a carriage return, we locally echo it as a CR + LF combination.
      if (character == '\r') {
        charBuffer.append('\n');
      }
      writeToTerminal(charBuffer.toString());
    }
  }

  @Override public void setTerminalTitle(String title) {
    terminalListener.setTerminalTitle(title);
  }

  @Override public TerminalState getState() {
    return state;
  }

  @Override public void setState(TerminalState state) {
    this.state = state;
    terminalListener.setState(state);
    // enable the (blinking) cursor if the terminal is connected
    runAsyncInDisplayThread(new Runnable() {
      @Override public void run() {
        if (textControl != null && !textControl.isDisposed()) {
          textControl.setCursorEnabled(isConnected());
        }
      }
    });
  }

  private void runAsyncInDisplayThread(Runnable runnable) {
    if (Display.findDisplay(Thread.currentThread()) != null) {
      runnable.run();
    } else if (PlatformUI.isWorkbenchRunning()) {
      PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
      // else should not happen and we ignore it...
    }
  }

  @Override public String getSettingsSummary() {
    if (getTerminalConnector() != null) {
      return getTerminalConnector().getSettingsSummary();
    }
    return "";
  }

  @Override public void setConnector(ITerminalConnector connector) {
    this.connector = connector;
  }

  @Override public ICommandInputField getCommandInputField() {
    return commandInputField;
  }

  @Override public void setCommandInputField(ICommandInputField inputField) {
    if (commandInputField != null) {
      commandInputField.dispose();
    }
    commandInputField = inputField;
    if (commandInputField != null) {
      commandInputField.createControl(rootControl, this);
    }
    if (rootControl.isVisible()) {
      rootControl.layout(true);
    }
  }

  @Override public int getBufferLineLimit() {
    return terminalModel.getMaxHeight();
  }

  @Override public void setBufferLineLimit(int bufferLineLimit) {
    if (bufferLineLimit <= 0) {
      return;
    }
    synchronized (terminalModel) {
      if (terminalModel.getHeight() > bufferLineLimit) {
        terminalModel.setDimensions(bufferLineLimit, terminalModel.getWidth());
      }
      terminalModel.setMaxHeight(bufferLineLimit);
    }
  }

  @Override public boolean isScrollLockOn() {
    return textControl.isScrollLockOn();
  }

  @Override public void setScrollLockOn(boolean on) {
    textControl.setScrollLockOn(on);
  }

  @Override public void setInvertedColors(boolean invert) {
    textControl.setInvertedColors(invert);
  }

  public void setColors(RGB background, RGB foreground) {
    textControl.setColors(background, foreground);
  }

  public void setBlinkingCursor(boolean useBlinkingCursor) {
    textControl.setBlinkingCursor(useBlinkingCursor);
  }
}
