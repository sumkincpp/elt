/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.core.connector;

import static com.google.eclipse.terminal.local.Activator.*;
import static com.google.eclipse.terminal.local.core.connector.Messages.*;
import static com.google.eclipse.terminal.local.core.connector.PseudoTerminal.isPlatformSupported;
import static com.google.eclipse.terminal.local.util.Platform.*;
import static org.eclipse.cdt.utils.Platform.*;
import static org.eclipse.core.runtime.IStatus.*;
import static org.eclipse.tm.internal.terminal.provisional.api.TerminalState.*;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.internal.core.StreamsProxy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tm.internal.terminal.connector.TerminalConnector;
import org.eclipse.tm.internal.terminal.provisional.api.*;
import org.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorImpl;

/**
 * Connector to local terminal.
 *
 * @author alruiz@google.com (Alex Ruiz)
 */
@SuppressWarnings("restriction") // StreamsProxy is internal API
public class LocalTerminalConnector extends TerminalConnectorImpl implements LifeCycleListener {
  private static final String ID = "com.google.eclipse.terminal.local.core.connector";

  public static final String ENCODING = "UTF-8";

  private IPath workingDirectory;
  private PseudoTerminal pseudoTerminal;

  private StreamsProxy streamsProxy;
  private OutputStream terminalToRemoteStream;

  public static ITerminalConnector createLocalTerminalConnector() {
    TerminalConnector.Factory factory = new TerminalConnector.Factory(){
      @Override public TerminalConnectorImpl makeConnector() {
        return new LocalTerminalConnector();
      }
    };
    TerminalConnector connector = new TerminalConnector(factory, ID, localTerminalName, false);
    String errorMessage = connector.getInitializationErrorMessage();
    if (errorMessage != null) {
      throw new IllegalStateException(errorMessage);
    }
    return connector;
  }

  private LocalTerminalConnector() {}

  /**
   * Verifies that PTY support is available on this platform.
   * @throws CoreException if PTY support is <strong>not</strong> available on this platform.
   * @see TerminalConnectorImpl#initialize()
   */
  @Override public void initialize() throws CoreException {
    if (!isPlatformSupported()) {
      String message = NLS.bind(errorNoPseudoTerminalSupport, getOS(), getOSArch());
      throw new CoreException(new Status(WARNING, PLUGIN_ID, message));
    }
  }

  @Override public void connect(ITerminalControl control) {
    super.connect(control);
    control.setState(CONNECTING);
    File workingDirectory = workingDirectory();
    pseudoTerminal = new PseudoTerminal(workingDirectory);
    pseudoTerminal.addLifeCycleListener(this);
    try {
      pseudoTerminal.launch();
      streamsProxy = new StreamsProxy(pseudoTerminal.systemProcess(), ENCODING);
      terminalToRemoteStream = new BufferedOutputStream(new TerminalOutputStream(streamsProxy, ENCODING), 1024);
      setUpOutput(control, streamsProxy.getOutputStreamMonitor());
      setUpOutput(control, streamsProxy.getErrorStreamMonitor());
      if (streamsProxy != null) {
        control.setState(CONNECTED);
        return;
      }
    } catch (Throwable t) {
      log(new Status(INFO, PLUGIN_ID, OK, "Unable to start terminal", t));
    }
    control.setState(CLOSED);
  }

  private File workingDirectory() {
    IPath path = (workingDirectory != null) ? workingDirectory : userHomeDirectory();
    if (path == null) {
      return null;
    }
    File file = path.toFile();
    return (file.isDirectory()) ? file : null;
  }

  private void setUpOutput(ITerminalControl control, IStreamMonitor outputMonitor) throws UnsupportedEncodingException {
    TerminalOutputListener outputListener = new TerminalOutputListener(control);
    outputMonitor.addListener(outputListener);
    outputListener.streamAppended(outputMonitor.getContents(), outputMonitor);
  }

  /** {@inheritDoc} */
  @Override public OutputStream getTerminalToRemoteStream() {
    return terminalToRemoteStream;
  }

  /**
   * Returns the system's default shell location as the settings summary.
   * @return the system's default shell location as the settings summary.
   */
  @Override public String getSettingsSummary() {
    return defaultShell().toString();
  }

  /**
   * Notifies the pseudo-terminal that the size of the terminal has changed.
   * @param newWidth the new terminal width (in columns.)
   * @param newHeight the new terminal height (in lines.)
   */
  @Override public void setTerminalSize(int newWidth, int newHeight) {
    if (pseudoTerminal != null) {
      pseudoTerminal.updateSize(newWidth, newHeight);
    }
  }

  public void setWorkingDirectory(IPath workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  @Override protected void doDisconnect() {
    pseudoTerminal.disconnect();
  }

  @Override public void executionFinished() {
    fControl.setState(CLOSED);
    if (streamsProxy != null) {
      streamsProxy.close();
    }
  }

  public void addLifeCycleListener(LifeCycleListener listener) {
    pseudoTerminal.addLifeCycleListener(listener);
  }
}
