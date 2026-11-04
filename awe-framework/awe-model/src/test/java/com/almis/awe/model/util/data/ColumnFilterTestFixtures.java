package com.almis.awe.model.util.data;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.model.component.PrototypeRequestBeanHolder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared fixtures of the grid column autofilter tests (#912): query utilities whose localization answers the key and
 * JSON written with single quotes
 */
public final class ColumnFilterTestFixtures {

  /**
   * JSON mapper of the query utilities
   */
  public static final JsonMapper MAPPER = new JsonMapper();

  private ColumnFilterTestFixtures() {
  }

  /**
   * Query utilities whose localization answers the key, followed by its parameters joined by commas
   *
   * @return Query utilities
   */
  public static QueryUtil queryUtil() {
    return new QueryUtil(new BaseConfigProperties(), new DatabaseConfigProperties(), MAPPER, new PrototypeRequestBeanHolder()) {
      @Override
      public String getLocale(String locale) {
        return locale;
      }

      @Override
      public String getLocale(String locale, String... parameters) {
        return locale + " " + String.join(",", parameters);
      }
    };
  }

  /**
   * Parse JSON written with single quotes
   *
   * @param text JSON with single quotes instead of double quotes
   * @return JSON node
   */
  public static JsonNode json(String text) {
    return MAPPER.readTree(text.replace('\'', '"'));
  }
}
