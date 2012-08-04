/*******************************************************************************
 * Copyright (c) 2007, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Michael Scharf (Wind River) - [200541] Extract from TerminalConnectorExtension.TerminalConnectorProxy
 * Martin Oberhuber (Wind River) - [225853][api] Provide more default functionality in TerminalConnectorImpl
 * Uwe Stieber (Wind River) - [282996] [terminal][api] Add "hidden" attribute to terminal connector extension point
 *******************************************************************************/
package com.google.eclipse.tm.internal.terminal.connector;

import static com.google.eclipse.tm.internal.terminal.provisional.api.TerminalState.CLOSED;

import java.io.OutputStream;

import org.eclipse.core.runtime.*;

import com.google.eclipse.tm.internal.terminal.control.impl.TerminalMessages;
import com.google.eclipse.tm.internal.terminal.provisional.api.*;
import com.google.eclipse.tm.internal.terminal.provisional.api.provider.TerminalConnectorDelegate;

/**
 * An {@link ITerminalConnector} instance, also known as terminal connection type, for maintaining a single terminal
 * connection.
 *
 * It provides all terminal connector functions that can be provided by static markup without loading the actual
 * implementation class. The actual {@link TerminalConnectorDelegate} implementation class is lazily loaded by the provided
 * {@link TerminalConnector.Factory} interface when needed. class, and delegates to the actual implementation when
 * needed. The following methods can be called without initializing the contributed implementation class:
 * {@link #getId()}, {@link #getName()}, {@link #getSettingsSummary()}, {@link #setTerminalSize(int, int)},
 * {@link #getAdapter(Class)}.
 */
public class TerminalConnector implements ITerminalConnector {
  /**
   * Creates an instance of TerminalConnectorImpl. This is used to lazily load classed defined in extensions.
   */
  public interface Factory {
    TerminalConnectorDelegate makeConnector() throws Exception;
  }

  private final TerminalConnector.Factory terminalConnectorFactory;
  private final String name;
  private final String id;

  private TerminalConnectorDelegate connector;

  //If the initialization of the class specified in the extension fails, this variable contains the error.
  private Exception initializationException;

  /**
   * Constructor for the terminal connector.
   *
   * @param terminalConnectorFactory Factory for lazily instantiating the TerminalConnectorImpl when needed.
   * @param id terminal connector ID. The connector is publicly known under this ID.
   * @param name translatable name to display the connector in the UI.
   */
  public TerminalConnector(TerminalConnector.Factory terminalConnectorFactory, String id, String name) {
    this.terminalConnectorFactory = terminalConnectorFactory;
    this.id = id;
    this.name = name;
  }

  @Override public String getInitializationErrorMessage() {
    getConnectorDelegate();
    if (initializationException != null) {
      return initializationException.getLocalizedMessage();
    }
    return null;
  }

  @Override public String getId() {
    return id;
  }

  @Override public String getName() {
    return name;
  }

  private TerminalConnectorDelegate getConnectorDelegate() {
    if (!isInitialized()) {
      initializeConnector();
    }
    return connector;
  }

  private void initializeConnector() {
    try {
      connector = terminalConnectorFactory.makeConnector();
      connector.initialize();
    } catch (Exception e) {
      initializationException = e;
      connector = new TerminalConnectorDelegate() {
        @Override protected void connect() {
          terminalControl.setState(CLOSED);
          terminalControl.setErrorMessage(getInitializationErrorMessage());
        }

        @Override public OutputStream getTerminalToRemoteStream() {
          return null;
        }

        @Override public String getSettingsSummary() {
          return null;
        }
      };
      Logger.logException(e);
    }
  }

  @Override public boolean isInitialized() {
    return connector != null || initializationException != null;
  }

  @Override public void connect(ITerminalControl control) {
    getConnectorDelegate().connect(control);
  }

  @Override public void disconnect() {
    getConnectorDelegate().disconnect();
  }

  @Override public OutputStream getTerminalToRemoteStream() {
    return getConnectorDelegate().getTerminalToRemoteStream();
  }

  @Override public String getSettingsSummary() {
    if (connector != null) {
      return getConnectorDelegate().getSettingsSummary();
    }
    return TerminalMessages.notInitialized;
  }

  @Override public boolean isLocalEcho() {
    return getConnectorDelegate().isLocalEcho();
  }

  @Override public void setTerminalSize(int newWidth, int newHeight) {
    // We assume that setTerminalSize is called also after the terminal has been initialized, otherwise we would have to
    // cache the values.
    if (connector != null) {
      connector.setTerminalSize(newWidth, newHeight);
    }
  }

  @Override public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    TerminalConnectorDelegate connector = null;
    if (isInitialized()) {
      connector = getConnectorDelegate();
    }
    // If we cannot create the connector then we cannot adapt.
    if (connector != null) {
      // Maybe the connector is adaptable.
      if (connector instanceof IAdaptable) {
        Object result = ((IAdaptable) connector).getAdapter(adapter);
        // Not sure if the next block is needed.
        if (result == null) {
          // Defer to the platform.
          result = Platform.getAdapterManager().getAdapter(connector, adapter);
        }
        if (result != null) {
          return result;
        }
      }
      // Maybe the real adapter is what we need.
      if (adapter.isInstance(connector)) {
        return connector;
      }
    }
    // Maybe we have to be adapted.
    return Platform.getAdapterManager().getAdapter(this, adapter);
  }
}