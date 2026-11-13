package com.almis.awe.test;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Status card of the card test screen (#836): the status cycles on every call, so the browser test can rely on a change
 */
// The surefire run of this module (Unit · Java) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class CardStatusServiceTest {

  private final CardStatusService service = new CardStatusService();

  @Test
  void shouldCycleOperationalDegradedDownAndBackToOperational() {
    assertStatus(service.status(), "CARD_TST_STATUS_OPERATIONAL", "success", "lucide:circle-check", 99.98);
    assertStatus(service.status(), "CARD_TST_STATUS_DEGRADED", "warning", "lucide:triangle-alert", 97.4);
    assertStatus(service.status(), "CARD_TST_STATUS_DOWN", "danger", "lucide:circle-x", 82.1);
    assertStatus(service.status(), "CARD_TST_STATUS_OPERATIONAL", "success", "lucide:circle-check", 99.98);
  }

  private static void assertStatus(ServiceData data, String chip, String tone, String icon, double availability) {
    assertThat(data.getDataList().getRows()).hasSize(1);
    Map<String, CellData> row = data.getDataList().getRows().get(0);
    assertThat(row.get("chip").getStringValue()).isEqualTo(chip);
    assertThat(row.get("chipTone").getStringValue()).isEqualTo(tone);
    assertThat(row.get("icon").getStringValue()).isEqualTo(icon);
    assertThat(row.get("iconTone").getStringValue()).isEqualTo(tone);
    assertThat(row.get("value").getDoubleValue()).isEqualTo(availability);
    assertThat(row.get("unit").getStringValue()).isEqualTo("%");
    assertThat(row.get("footer").getStringValue()).isEqualTo("CARD_TST_STATUS_FOOTER");
  }
}
