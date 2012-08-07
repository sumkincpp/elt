/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.view.preferences;

import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.preference.ColorSelector.PROP_COLORCHANGE;
import static org.eclipse.jface.preference.PreferenceConverter.*;
import static org.eclipse.jface.resource.JFaceResources.TEXT_FONT;
import static org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn;

import static com.google.eclipse.elt.view.Activator.*;
import static com.google.eclipse.elt.view.preferences.Messages.*;
import static com.google.eclipse.elt.view.preferences.PreferenceNames.*;

import java.io.InputStream;
import java.util.Scanner;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
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
  private Button btnUseTextFont;
  private Button btnUseCustomFont;
  private Label lblFontData;
  private Button btnChangeFont;

  private FontData fontData;

  private IPropertyChangeListener textFontChangeListener;

  @Override public void init(IWorkbench workbench) {
    setPreferenceStore(preferenceStore());
  }

  @Override protected Control createContents(Composite parent) {
    Composite contents = new Composite(parent, SWT.NONE);
    contents.setLayout(new GridLayout(1, false));

    Label lblDescription = new Label(contents, SWT.NONE);
    lblDescription.setText(colorsAndFontsTitle);

    GridDataFactory gridDataFactory = fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(1, 1);

    Composite controls = new Composite(contents, SWT.NONE);
    gridDataFactory.applyTo(controls);
    controls.setLayout(new GridLayout(1, false));

    Group grpColors = new Group(controls, SWT.NONE);
    gridDataFactory.applyTo(grpColors);
    grpColors.setLayout(new GridLayout(2, false));

    Label lblBackground = new Label(grpColors, SWT.NONE);
    lblBackground.setText(backgroundPrompt);

    backgroundColorSelector = new ColorSelector(grpColors);

    Label lblForeground = new Label(grpColors, SWT.NONE);
    lblForeground.setText(foregroundPrompt);

    foregroundColorSelector = new ColorSelector(grpColors);

    Group grpFont = new Group(controls, SWT.NONE);
    gridDataFactory.applyTo(grpFont);
    grpFont.setLayout(new GridLayout(2, false));

    SelectionListener fontButtonSelectionListener = new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        boolean useTextFont = btnUseTextFont.getSelection();
        btnChangeFont.setEnabled(!useTextFont);
        if (useTextFont) {
          updateFontDataWithTextFont();
        }
      }
    };

    btnUseTextFont = new Button(grpFont, SWT.RADIO);
    gridDataFactory.span(2, 1).applyTo(btnUseTextFont);
    btnUseTextFont.setText(useTextFont);
    btnUseTextFont.addSelectionListener(fontButtonSelectionListener);

    Link changeTextFontLink = new Link(grpFont, SWT.NONE);
    changeTextFontLink.setText(textFontLink);
    GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
    gridData.widthHint = 150; // only expand further if anyone else requires it
    gridData.horizontalSpan = 2;
    changeTextFontLink.setLayoutData(gridData);
    changeTextFontLink.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        if ("org.eclipse.ui.preferencePages.ColorsAndFonts".equals(e.text)) {
          createPreferenceDialogOn(getShell(), e.text, null, TEXT_FONT);
        }
      }
    });

    btnUseCustomFont = new Button(grpFont, SWT.RADIO);
    gridDataFactory.applyTo(btnUseCustomFont);
    btnUseCustomFont.setText(useCustomFont);
    btnUseCustomFont.addSelectionListener(fontButtonSelectionListener);

    lblFontData = new Label(grpFont, SWT.NONE);
    gridDataFactory.span(1, 1).applyTo(lblFontData);
    lblFontData.setText("");

    btnChangeFont = new Button(grpFont, SWT.NONE);
    btnChangeFont.setText(change);
    btnChangeFont.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        FontDialog fontDialog = new FontDialog(getShell());
        fontDialog.setFontList(new FontData[] { fontData });
        FontData newFontData = fontDialog.open();
        if (newFontData != null) {
          updateFontData(newFontData);
        }
      }
    });

    Label lblPreview = new Label(contents, SWT.NONE);
    lblPreview.setText(previewPrompt);

    previewer = new ProjectionViewer(contents, null, null, false, SWT.V_SCROLL | SWT.H_SCROLL);
    previewer.setEditable(false);
    previewer.setDocument(new Document(loadContentsFrom("ColorSettingPreviewText.txt")));

    StyledText previewerText = previewer.getTextWidget();
    gridDataFactory.align(SWT.FILL, SWT.FILL).grab(true, true);
    gridDataFactory.applyTo(previewerText);
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

    textFontChangeListener = new IPropertyChangeListener() {
      @Override public void propertyChange(PropertyChangeEvent event) {
        if (TEXT_FONT.equals(event.getProperty()) && btnUseTextFont.getSelection()) {
          updateFontDataWithTextFont();
        }
      }
    };
    JFaceResources.getFontRegistry().addListener(textFontChangeListener);

    updateContents();
    return contents;
  }

  private String loadContentsFrom(String fileName) {
    String lineSeparator = System.getProperty("line.separator");
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

  private void updateFontDataWithTextFont() {
    updateFontData(JFaceResources.getTextFont().getFontData()[0]);
  }

  private void updateFontData(FontData newValue) {
    fontData = newValue;
    displayFont();
  }

  private void updateContents() {
    RGB background = getColor(getPreferenceStore(), BACKGROUND_COLOR);
    RGB foreground = getColor(getPreferenceStore(), FOREGROUND_COLOR);
    fontData = getFontData(getPreferenceStore(), CUSTOM_FONT_DATA);
    boolean useCustomFont = getPreferenceStore().getBoolean(USE_CUSTOM_FONT);
    updateContents(background, foreground, useCustomFont);
  }

  @Override public boolean performOk() {
    setValue(getPreferenceStore(), BACKGROUND_COLOR, backgroundColorSelector.getColorValue());
    setValue(getPreferenceStore(), FOREGROUND_COLOR, foregroundColorSelector.getColorValue());
    preferenceStore().setValue(USE_CUSTOM_FONT, btnUseCustomFont.getSelection());
    setValue(getPreferenceStore(), CUSTOM_FONT_DATA, fontData);
    return true;
  }

  @Override protected void performDefaults() {
    RGB background = getDefaultColor(getPreferenceStore(), BACKGROUND_COLOR);
    RGB foreground = getDefaultColor(getPreferenceStore(), FOREGROUND_COLOR);
    fontData = getDefaultFontData(getPreferenceStore(), CUSTOM_FONT_DATA);
    boolean useCustomFont = getPreferenceStore().getDefaultBoolean(USE_CUSTOM_FONT);
    updateContents(background, foreground, useCustomFont);
  }

  private void updateContents(RGB background, RGB foreground, boolean useCustomFont) {
    backgroundColorSelector.setColorValue(background);
    foregroundColorSelector.setColorValue(foreground);
    btnUseTextFont.setSelection(!useCustomFont);
    btnUseCustomFont.setSelection(useCustomFont);
    btnChangeFont.setEnabled(useCustomFont);
    previewer.getTextWidget().setBackground(newColor(background));
    previewer.setTextColor(newColor(foreground));
    displayFont();
  }

  private void displayFont() {
    lblFontData.setText(StringConverter.asString(fontData));
    previewer.getTextWidget().setFont(new Font(display(), fontData));
  }

  private Color newColor(RGB rgb) {
    return new Color(display(), rgb);
  }

  private Display display() {
    return getShell().getDisplay();
  }

  @Override public void dispose() {
    if (textFontChangeListener != null) {
      JFaceResources.getFontRegistry().removeListener(textFontChangeListener);
    }
    super.dispose();
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
