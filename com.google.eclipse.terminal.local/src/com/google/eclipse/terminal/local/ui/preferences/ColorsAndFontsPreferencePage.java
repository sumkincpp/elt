/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.terminal.local.ui.preferences;

import static com.google.eclipse.terminal.local.Activator.*;
import static com.google.eclipse.terminal.local.ui.preferences.Messages.*;
import static com.google.eclipse.terminal.local.ui.preferences.PreferenceNames.*;
import static org.eclipse.jface.preference.ColorSelector.PROP_COLORCHANGE;

import java.io.InputStream;
import java.util.Scanner;

import org.eclipse.jface.preference.*;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class ColorsAndFontsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  private ProjectionViewer previewer;
  private ColorSelector foregroundColorSelector;
  private ColorSelector backgroundColorSelector;

  @Override public void init(IWorkbench workbench) {
    setPreferenceStore(preferenceStore());
  }

  @Override protected Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    contents.setLayout(new GridLayout(1, false));
    Label lblDescription = new Label(contents, SWT.NONE);
    lblDescription.setText(colorsAndFontsTitle);
    new Label(contents, SWT.NONE);
    Composite controls = new Composite(contents, SWT.NONE);
    controls.setLayout(new GridLayout(2, false));
    Label lblBackground = new Label(controls, SWT.NONE);
    lblBackground.setText(backgroundPrompt);
    backgroundColorSelector = new ColorSelector(controls);
    Label lblForeground = new Label(controls, SWT.NONE);
    lblForeground.setText(foregroundPrompt);
    foregroundColorSelector = new ColorSelector(controls);
    new Label(contents, SWT.NONE);
    Label lblPreview = new Label(contents, SWT.NONE);
    lblPreview.setText(previewPrompt);
    previewer = new ProjectionViewer(contents, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
    previewer.setEditable(false);
    previewer.setDocument(new Document(loadContentsFrom("ColorSettingPreviewText.txt"))); //$NON-NLS-1$
    StyledText previewerText = previewer.getTextWidget();
    previewerText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
    Cursor arrowCursor = previewerText.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
    previewerText.setCursor(arrowCursor);
    backgroundColorSelector.addListener(new ColorChangeListener() {
      @Override void onColorChanged(RGB newValue) {
        validateInput();
        if (isValid()) {
          previewer.getTextWidget().setBackground(newColor(newValue));
        }
      }
    });
    foregroundColorSelector.addListener(new ColorChangeListener() {
      @Override void onColorChanged(RGB newValue) {
        validateInput();
        if (isValid()) {
          previewer.setTextColor(newColor(newValue));
        }
      }
    });
    updateContents();
    return contents;
  }

  private String loadContentsFrom(String fileName) {
    String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
    StringBuilder buffer = new StringBuilder();
    Scanner scanner = null;
    try {
      InputStream inputStream = getClass().getResourceAsStream(fileName);
      scanner = new Scanner(inputStream);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        buffer.append(line).append(lineSeparator);
      }
    } catch (RuntimeException e) {
      log(unableToLoadPreviewContent, e);
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }
    return buffer.toString();
  }

  private void validateInput() {
    if (backgroundColorSelector.getColorValue().equals(foregroundColorSelector.getColorValue())) {
      setErrorMessage(backgroundAndForegroundCannotBeTheSame);
      setValid(false);
      return;
    }
    pageIsValid();
  }

  private void pageIsValid() {
    setErrorMessage(null);
    setValid(true);
  }

  private void updateContents() {
    displayValue(BACKGROUND_COLOR, backgroundColorSelector);
    displayValue(FOREGROUND_COLOR, foregroundColorSelector);
    updatePreview();
  }

  private void displayValue(String preferenceName, ColorSelector colorSelector) {
    RGB color = PreferenceConverter.getColor(getPreferenceStore(), preferenceName);
    colorSelector.setColorValue(color);
  }

  private Color newColor(RGB rgb) {
    return new Color(getShell().getDisplay(), rgb);
  }

  @Override public boolean performOk() {
    storeValue(BACKGROUND_COLOR, backgroundColorSelector);
    storeValue(FOREGROUND_COLOR, foregroundColorSelector);
    return true;
  }

  private void storeValue(String preferenceName, ColorSelector colorSelector) {
    PreferenceConverter.setValue(getPreferenceStore(), preferenceName, colorSelector.getColorValue());
  }

  @Override protected void performDefaults() {
    displayDefaultValue(BACKGROUND_COLOR, backgroundColorSelector);
    displayDefaultValue(FOREGROUND_COLOR, foregroundColorSelector);
    updatePreview();
  }

  private void displayDefaultValue(String preferenceName, ColorSelector colorSelector) {
    RGB rgb = PreferenceConverter.getDefaultColor(getPreferenceStore(), preferenceName);
    colorSelector.setColorValue(rgb);
  }

  private void updatePreview() {
    previewer.getTextWidget().setBackground(newColor(backgroundColorSelector.getColorValue()));
    previewer.setTextColor(newColor(foregroundColorSelector.getColorValue()));
  }

  private static abstract class ColorChangeListener implements IPropertyChangeListener {
    @Override public final void propertyChange(PropertyChangeEvent event) {
      if (PROP_COLORCHANGE.equals(event.getProperty())) {
        RGB rgb = (RGB) event.getNewValue();
        onColorChanged(rgb);
      }
    }

    abstract void onColorChanged(RGB newValue);
  }
}
