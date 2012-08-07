/*
 * Copyright (c) 2012 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.elt.emulator.hyperlink;

import java.util.*;
import java.util.regex.*;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlink;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
public class HttpHyperlinkFactory implements IHyperlinkFactory {
  private static Pattern URL_PATTERN =
      Pattern.compile("http(s)?:\\/{2}[\\d\\w-]+(\\.[\\d\\w-]+)*(?:(?:\\/[^\\s/]*))*(\\:[\\d]+)?");

  @Override public List<IHyperlink> hyperlinksIn(int column, String text) {
    if (text == null) {
      return NO_HYPERLINKS;
    }
    List<IHyperlink> hyperlinks = new ArrayList<IHyperlink>();
    Matcher matcher = URL_PATTERN.matcher(text);
    int start = 0;
    while (matcher.find(start)) {
      String url = matcher.group().trim();
      if (url.isEmpty()) {
        continue;
      }
      IRegion region = new Region(column + matcher.start(), url.length());
      hyperlinks.add(new UrlHyperlink(region, url));
      start = matcher.end();
    }
    return hyperlinks.isEmpty() ? NO_HYPERLINKS : hyperlinks;
  }
}
