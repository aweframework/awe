package com.almis.awe.service;

import com.almis.awe.model.dto.ServiceData;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Printer service tests
 */
class PrinterServiceTest {

  @Test
  void looksPrintersUpAgainOnlyWhenTheListExpires() throws Exception {
    AtomicInteger lookups = new AtomicInteger();
    AtomicLong now = new AtomicLong();
    PrinterService service = new PrinterService(() -> List.of("printer-" + lookups.incrementAndGet()), now::get);

    ServiceData first = service.getPrinterList();
    now.addAndGet(PrinterService.PRINTER_LIST_TTL.minus(Duration.ofMillis(1)).toNanos());
    ServiceData cached = service.getPrinterList();
    now.addAndGet(Duration.ofMillis(1).toNanos());
    ServiceData expired = service.getPrinterList();

    assertThat(lookups).hasValue(2);
    assertThat(printers(first)).containsExactly("printer-1");
    assertThat(printers(cached)).containsExactly("printer-1");
    assertThat(cached.getDataList()).isNotSameAs(first.getDataList());
    assertThat(printers(expired)).containsExactly("printer-2");
    assertThat(expired.getDataList().getRecords()).isEqualTo(1);
  }

  private static List<String> printers(ServiceData serviceData) {
    return serviceData.getDataList().getRows().stream().map(row -> row.get("printer").getStringValue()).toList();
  }
}
