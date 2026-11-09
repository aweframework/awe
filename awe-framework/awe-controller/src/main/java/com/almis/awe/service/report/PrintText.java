package com.almis.awe.service.report;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * Plain text of the cells that the screen shows as HTML (formatted-text columns): a report never prints markup
 */
final class PrintText {

  private static final Pattern ACTIVE_BLOCK = Pattern.compile("(?is)<(script|style)\\b.*?(</\\1\\s*>|$)");
  private static final Pattern LINE_BREAK = Pattern.compile("(?i)<br\\s*/?>|</(p|div|li|h[1-6]|tr)\\s*>");
  private static final Pattern TAG = Pattern.compile("(?s)<[^>]*>");
  private static final Pattern SPACES = Pattern.compile("[\\s\\u00A0]+");

  private PrintText() {
  }

  /**
   * Convert HTML to plain text for a single-line cell: scripts and styles are dropped, line breaks and block ends become a
   * space, the other tags are removed, the entities are decoded and the white space is collapsed
   *
   * @param html HTML text
   * @return Plain text
   */
  static String plainText(String html) {
    if (html == null || html.isEmpty()) {
      return "";
    }
    String text = ACTIVE_BLOCK.matcher(html).replaceAll("");
    text = LINE_BREAK.matcher(text).replaceAll(" ");
    text = TAG.matcher(text).replaceAll("");
    text = HtmlUtils.htmlUnescape(text);
    return SPACES.matcher(text).replaceAll(" ").trim();
  }
}
