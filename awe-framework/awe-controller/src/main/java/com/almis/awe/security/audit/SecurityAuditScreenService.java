package com.almis.awe.security.audit;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Services of the security audit screen
 */
public class SecurityAuditScreenService {

  /** Days shown by default on the security audit screen */
  public static final int DEFAULT_DAYS = 7;

  private final Clock clock;

  /**
   * Constructor
   *
   * @param clock Clock
   */
  public SecurityAuditScreenService(Clock clock) {
    this.clock = clock;
  }

  /**
   * Default start date of the screen: today minus {@value #DEFAULT_DAYS} days, so the first load stays small and fast
   *
   * @return Service data with the date in the value column
   */
  public ServiceData getDefaultDateFrom() {
    LocalDate from = LocalDate.now(clock).minusDays(DEFAULT_DAYS);
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(new DataList());
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "value",
      Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    return serviceData;
  }

  /**
   * Date range of a preset of the time range picker: the lower bound is now minus the preset, the upper bound is now
   *
   * @param preset Preset code: 15m, 1h, 6h, 24h, 7d or 30d. Anything else falls back to the default range
   * @return Service data with the two dates in the CrtFro and CrtTo columns
   */
  public ServiceData getRange(String preset) {
    Instant now = clock.instant();
    String code = preset == null || preset.isBlank() ? "7d" : preset.trim().toLowerCase();
    long seconds = switch (code) {
      case "15m" -> 15L * 60;
      case "1h" -> 60L * 60;
      case "6h" -> 6L * 60 * 60;
      case "24h" -> 24L * 60 * 60;
      case "30d" -> 30L * 24 * 60 * 60;
      default -> (long) DEFAULT_DAYS * 24 * 60 * 60;
    };
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(new DataList());
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "CrtFro", Date.from(now.minusSeconds(seconds)));
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "CrtTo", Date.from(now));
    ZoneId zone = clock.getZone();
    // The date criteria keep only the day: the exact bounds travel apart, so a range of hours filters by the hour
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "CrtFroTs", BOUND_FORMAT.format(now.minusSeconds(seconds).atZone(zone)));
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "CrtToTs", BOUND_FORMAT.format(now.atZone(zone)));
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "CrtRngTxt",
      RANGE_FORMAT.format(now.minusSeconds(seconds).atZone(zone)) + " - " + RANGE_FORMAT.format(now.atZone(zone)));
    return serviceData;
  }

  /** Format of the range label: date and time, so a range of hours is not two equal dates */
  private static final DateTimeFormatter RANGE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  /** Format of the exact bounds of a preset: the web timestamp the TIMESTAMP query variables parse */
  private static final DateTimeFormatter BOUND_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  /**
   * Label of the time range picker: the two dates in force, with their time
   *
   * @param from Lower bound
   * @param to Upper bound
   * @return Service data with the text in the CrtRngTxt column
   */
  public ServiceData formatRange(Date from, Date to) {
    ZoneId zone = clock.getZone();
    String text = format(from, zone) + " - " + format(to, zone);
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(new DataList());
    DataListUtil.addColumnWithOneRow(serviceData.getDataList(), "CrtRngTxt", text);
    return serviceData;
  }

  private static String format(Date date, ZoneId zone) {
    return date == null ? "" : RANGE_FORMAT.format(date.toInstant().atZone(zone));
  }
}
