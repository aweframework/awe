package com.almis.awe.test;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Status card of the card test screen (#836): every call moves to the next status (operational, degraded, down and
 * back to operational), so the reload button always changes the chip, the icon and the availability of the card
 */
@Service
public class CardStatusService {

  private static final List<Map<String, Object>> STATUSES = List.of(
    status("CARD_TST_STATUS_OPERATIONAL", "success", "lucide:circle-check", 99.98),
    status("CARD_TST_STATUS_DEGRADED", "warning", "lucide:triangle-alert", 97.4),
    status("CARD_TST_STATUS_DOWN", "danger", "lucide:circle-x", 82.1));

  private final AtomicInteger calls = new AtomicInteger();

  /**
   * Status card row: the status of this call, the next call returns the following one
   *
   * @return Service data with the card row
   */
  public ServiceData status() {
    Map<String, Object> status = STATUSES.get(Math.floorMod(calls.getAndIncrement(), STATUSES.size()));
    DataList dataList = new DataList();
    status.forEach((name, value) -> DataListUtil.addColumn(dataList, name, List.of(value)));
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, Object> status(String chip, String tone, String icon, double availability) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("chip", chip);
    fields.put("chipTone", tone);
    fields.put("icon", icon);
    fields.put("iconTone", tone);
    fields.put("value", availability);
    fields.put("unit", "%");
    fields.put("footer", "CARD_TST_STATUS_FOOTER");
    return fields;
  }
}
