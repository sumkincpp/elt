/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.tm.internal.terminal.hyperlink;

import java.net.*;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.URLHyperlink;
import org.eclipse.ui.*;
import org.eclipse.ui.browser.*;

import com.google.eclipse.tm.internal.terminal.control.impl.TerminalPlugin;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class UrlHyperlink extends URLHyperlink {
  public UrlHyperlink(IRegion region, String urlString) {
    super(region, urlString);
  }

  @Override public void open() {
    // Create the browser
    IWorkbenchBrowserSupport support= PlatformUI.getWorkbench().getBrowserSupport();
    IWebBrowser browser;
    try {
      browser= support.createBrowser(null);
    } catch (PartInitException e) {
      logErrorOpeningUrl(e);
      return;
    }
    URL url = null;
    try {
      url = new URL(getURLString());
      browser.openURL(url);
    } catch (PartInitException e) {
      openInExternalBrowser(url);
    } catch (MalformedURLException e) {
      logErrorOpeningUrl(e);
    }
  }

  private void openInExternalBrowser(URL url) {
    try {
      PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
    } catch (Exception e) {
      logErrorOpeningUrl(e);
    }
  }

  private void logErrorOpeningUrl(Exception e) {
    e.printStackTrace();
    String format = "Unable to open URL '%s'";
    TerminalPlugin.log(String.format(format, getURLString()), e);
  }
}
