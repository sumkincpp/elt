/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.tm.internal.terminal.hyperlink;

import static java.util.Collections.emptyList;

import java.util.List;

import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public interface IHyperlinkFactory {
  List<IHyperlink> NO_HYPERLINKS = emptyList();

  List<IHyperlink> hyperlinksIn(int column, String text);
}
