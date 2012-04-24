/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.view;

import static com.google.eclipse.terminal.local.Activator.*;
import static com.google.eclipse.terminal.local.ui.preferences.ColorsAndFontsPreferences.*;
import static com.google.eclipse.terminal.local.ui.preferences.GeneralPreferences.closeViewOnExit;
import static com.google.eclipse.terminal.local.ui.util.Displays.runInDisplayThread;
import static com.google.eclipse.terminal.local.ui.view.Messages.defaultViewTitle;
import static com.google.eclipse.terminal.local.util.Platform.userHomeDirectory;
import static org.eclipse.core.runtime.Path.fromOSString;
import static org.eclipse.jface.resource.JFaceResources.TEXT_FONT;
import static org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.*;
import org.eclipse.tm.internal.terminal.control.ITerminalListener;
import org.eclipse.tm.internal.terminal.provisional.api.TerminalState;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;

import com.google.eclipse.terminal.local.core.connector.LifeCycleListener;
import com.google.eclipse.terminal.local.ui.preferences.AbstractColorsAndFontsPreferencesChangeListener;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class TerminalView extends ViewPart implements LifeCycleListener {
  private static final String TITLE_STATE_TYPE = "title";
  private static final String WORKING_DIRECTORY_STATE_TYPE = "workingDirectory";

  private static final String VIEW_ID = "com.google.eclipse.terminal.local.localTerminalView";

  private IPropertyChangeListener colorPreferencesChangeListener;
  private IPropertyChangeListener textFontChangeListener;
  private IMemento savedState;
  private TerminalWidget terminalWidget;
  private IPath workingDirectory;

  public static void openTerminalView(IPath workingDirectory) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      String directoryName = workingDirectory.lastSegment();
      TerminalView view = (TerminalView) page.showView(VIEW_ID, directoryName, VIEW_ACTIVATE);
      view.setPartName(directoryName);
      view.open(workingDirectory);
    } catch (PartInitException e) {
      log("Unable to create Terminal View", e);
    }
  }

  @Override public void executionFinished() {
    if (closeViewOnExit() && terminalWidget != null && !terminalWidget.isDisposed()) {
      // must run in UI thread.
      terminalWidget.getDisplay().asyncExec(new Runnable() {
        @Override public void run() {
          IWorkbenchPartSite site = getSite();
          site.getPage().hideView((IViewPart) site.getPart());
        }
      });
    }
  }

  @Override public void init(IViewSite site, IMemento memento) throws PartInitException {
    super.init(site, memento);
    savedState = memento;
  }

  @Override public void saveState(IMemento memento) {
    saveState(memento, TITLE_STATE_TYPE, getPartName());
    saveState(memento, WORKING_DIRECTORY_STATE_TYPE, workingDirectory.toOSString());
  }

  private void saveState(IMemento memento, String type, String data) {
    IMemento child = memento.createChild(type);
    child.putTextData(data);
  }

  @Override public void createPartControl(Composite parent) {
    terminalWidget = new TerminalWidget(parent, SWT.NONE);
    terminalWidget.setLifeCycleListener(this);
    terminalWidget.setTerminalListener(new ITerminalListener() {
      @Override public void setTerminalTitle(final String title) {
        runInDisplayThread(new Runnable() {
          @Override public void run() {
            setPartName(title);
          }
        });
      }

      @Override public void setState(TerminalState state) {
      }
    });
    IViewSite viewSite = getViewSite();
    terminalWidget.setUpGlobalEditActionHandlers(viewSite.getActionBars());
    colorPreferencesChangeListener = new AbstractColorsAndFontsPreferencesChangeListener() {
      @Override protected void onColorChanged() {
        updateColors();
      }

      @Override protected void onFontChanged() {
        updateFont();
      }
    };
    preferenceStore().addPropertyChangeListener(colorPreferencesChangeListener);
    updateColors();
    textFontChangeListener = new IPropertyChangeListener() {
      @Override public void propertyChange(PropertyChangeEvent event) {
        if (TEXT_FONT.equals(event.getProperty())) {
          if (!useCustomFont()) {
            setFont(JFaceResources.getTextFont());
          }
        }
      }
    };
    JFaceResources.getFontRegistry().addListener(textFontChangeListener);
    updateFont();
    if (savedState != null) {
      connectUsingSavedState();
      return;
    }
    if (viewSite.getSecondaryId() == null) {
      setPartName(defaultViewTitle);
      open(userHomeDirectory());
    }
  }

  private void updateColors() {
    terminalWidget.setColors(background(), foreground());
  }

  private void updateFont() {
    setFont(terminalFont());
  }

  private Font terminalFont() {
    if (useCustomFont()) {
      return new Font(Display.getDefault(), customFontData());
    }
    return JFaceResources.getTextFont();
  }

  private void setFont(Font font) {
    terminalWidget.setFont(font);
  }

  private void connectUsingSavedState() {
    String title = savedState(TITLE_STATE_TYPE);
    setPartName(title);
    String savedWorkingDirectory = savedState(WORKING_DIRECTORY_STATE_TYPE);
    if (savedWorkingDirectory != null) {
      open(fromOSString(savedWorkingDirectory));
    }
  }

  private String savedState(String type) {
    IMemento child = savedState.getChild(type);
    return (child != null) ? child.getTextData() : null;
  }

  private void open(IPath workingDirectory) {
    if (terminalWidget.isConnected()) {
      return;
    }
    this.workingDirectory = workingDirectory;
    terminalWidget.setWorkingDirectory(workingDirectory);
    terminalWidget.connect();
  }

  @Override public void setFocus() {
    terminalWidget.setFocus();
  }

  @Override public void dispose() {
    if (colorPreferencesChangeListener != null) {
      preferenceStore().removePropertyChangeListener(colorPreferencesChangeListener);
    }
    if (textFontChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(textFontChangeListener);
    }
    super.dispose();
  }
}
