package com.almis.awe.security.audit;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
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
}
