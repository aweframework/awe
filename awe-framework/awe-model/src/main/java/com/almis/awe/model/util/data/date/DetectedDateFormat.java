package com.almis.awe.model.util.data.date;

/**
 * Date text formats recognized by {@link DateFormatDetector#detect(String)}, in detection order
 */
public enum DetectedDateFormat {
  /**
   * {@code yyyy-MM-dd@HH:mm:ss.SSSZ}
   */
  JSON_DATE,
  /**
   * {@code yyyy-MM-dd HH:mm:ss.SSS}
   */
  SQL_TIMESTAMP,
  /**
   * {@code dd/MM/yyyy}
   */
  WEB_DATE,
  /**
   * {@code yyyy-MM-dd}
   */
  WBS_DATE,
  /**
   * {@code dd/MM/yyyy HH:mm:ss[.SSS]}
   */
  WEB_TIMESTAMP
}
