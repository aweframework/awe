package com.almis.awe.test.service;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Data of the cards of the dashboard (#899): fixed figures, so the dashboard always shows the same
 */
@Service
public class DashboardCardService {

  private static final List<String> WEEKS = List.of("W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8");
  private static final List<Double> ON_TIME = List.of(93.1, 93.8, 92.6, 94.9, 94.7, 95.8, 96.1, 96.8);

  /**
   * On-time deliveries card: the delta goes to the header (no icon) and a chart of the last weeks to the body
   *
   * @return Service data with the card row
   */
  public ServiceData deliveries() {
    return card(Map.of(
      "title", "DASHBOARD_CARD_DELIVERIES",
      "value", 96.8,
      "unit", "%",
      "delta", 2.4,
      "deltaUnit", "pt",
      "footer", "DASHBOARD_CARD_DELIVERIES_FOOTER"));
  }

  /**
   * On-time deliveries of the last weeks, shown in the body of the deliveries card
   *
   * @return Service data with a row per week
   */
  public ServiceData deliveriesTrend() {
    DataList dataList = new DataList();
    DataListUtil.addColumn(dataList, "week", WEEKS);
    DataListUtil.addColumn(dataList, "onTime", ON_TIME);
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Monthly revenue card: icon tile, progress towards the monthly target and delta with caption in the footer
   *
   * @return Service data with the card row
   */
  public ServiceData revenue() {
    return card(Map.of(
      "title", "DASHBOARD_CARD_REVENUE",
      "value", 412600,
      "unit", "€",
      "target", 500000,
      "delta", 6.1,
      "caption", "DASHBOARD_CARD_VS_LAST_MONTH",
      "footer", "DASHBOARD_CARD_REVENUE_FOOTER"));
  }

  /**
   * Error rate card: going down is good news (good-trend="down" in the screen), so a negative delta is a success
   *
   * @return Service data with the card row
   */
  public ServiceData errors() {
    return card(Map.of(
      "title", "DASHBOARD_CARD_ERRORS",
      "subtitle", "DASHBOARD_CARD_ERRORS_SUBTITLE",
      "value", 0.42,
      "unit", "%",
      "delta", -0.18,
      "deltaUnit", "pt",
      "caption", "DASHBOARD_CARD_VS_LAST_WEEK",
      "footer", "DASHBOARD_CARD_ERRORS_FOOTER"));
  }

  private static ServiceData card(Map<String, Object> fields) {
    DataList dataList = new DataList();
    fields.forEach((name, value) -> DataListUtil.addColumn(dataList, name, List.of(value)));
    return new ServiceData().setDataList(dataList);
  }
}
