package com.almis.awe.service.report;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Template of the row detail of a grid: the text of the `<row-detail>` element, where each `{name}` placeholder is
 * replaced with the text of the field `name` of the row. It is the same rule as the clients (renderRowDetail): what is
 * not a valid placeholder is kept as text, the template is read once from left to right and the result is plain text.
 */
final class RowDetailText {

  private static final Pattern FIELD_NAME = Pattern.compile("^[\\w.$-]+$");

  private RowDetailText() {
  }

  /**
   * Render the template with the data of a row
   *
   * @param template  Template with `{name}` placeholders
   * @param fieldText Text of a field of the row (empty when the row has not the field)
   * @return Plain text of the detail
   */
  static String render(String template, UnaryOperator<String> fieldText) {
    String text = template == null ? "" : template;
    StringBuilder result = new StringBuilder();
    int position = 0;
    while (position < text.length()) {
      int start = text.indexOf('{', position);
      int end = start < 0 ? -1 : text.indexOf('}', start);
      if (end < 0) {
        break;
      }
      // The last opening brace before the closing one is the start of the placeholder
      int innerStart = text.lastIndexOf('{', end);
      String name = text.substring(innerStart + 1, end);
      if (FIELD_NAME.matcher(name).matches()) {
        result.append(text, position, innerStart).append(fieldText.apply(name));
      } else {
        result.append(text, position, end + 1);
      }
      position = end + 1;
    }
    return result.append(text.substring(position)).toString();
  }
}
