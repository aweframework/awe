package com.almis.awe.test.service;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Customers of the advanced search test screen (#790): a fixed dataset filtered by every criterion of the screen
 */
// The surefire run of this module (Unit · Java) selects the "integration" tag (test.tags), so the tag is needed to run this unit test
@Tag("integration")
class AdvancedSearchCustomersTest {

  private static final DateTimeFormatter WEB_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final AdvancedSearchCustomers service = new AdvancedSearchCustomers();

  private List<Map<String, CellData>> rows(String name, String status, String country, String segment, String minAmount,
                                           String createdFrom, String priority) {
    ServiceData data = service.getCustomers(name, status, country, segment, minAmount, createdFrom, priority);
    assertThat(data.getDataList().getRecords()).isEqualTo(data.getDataList().getRows().size());
    return data.getDataList().getRows();
  }

  private List<Map<String, CellData>> all() {
    return rows(null, null, null, null, null, null, null);
  }

  @Test
  void shouldReturnEveryCustomerWithoutFiltersAndTheSameRowsOnEveryCall() {
    List<Map<String, CellData>> rows = all();

    assertThat(rows).hasSize(AdvancedSearchCustomers.ROW_COUNT);
    assertThat(rows.get(0)).containsOnlyKeys("id", "name", "status", "country", "segment", "amount", "created",
      "createdSort", "priority");
    assertThat(rows.stream().map(row -> row.get("name").getStringValue()).distinct()).hasSize(AdvancedSearchCustomers.ROW_COUNT);
    assertThat(all().stream().map(row -> row.get("name").getStringValue()).toList())
      .isEqualTo(rows.stream().map(row -> row.get("name").getStringValue()).toList());
  }

  @Test
  void shouldIgnoreBlankFilters() {
    assertThat(rows("", "", "", "", "", "", "")).hasSize(AdvancedSearchCustomers.ROW_COUNT);
    assertThat(rows(" ", null, null, null, null, null, "0")).hasSize(AdvancedSearchCustomers.ROW_COUNT);
  }

  @Test
  void shouldFilterByAPartOfTheNameIgnoringTheCase() {
    List<Map<String, CellData>> rows = rows("atlas", null, null, null, null, null, null);

    assertThat(rows).isNotEmpty().hasSizeLessThan(AdvancedSearchCustomers.ROW_COUNT)
      .allSatisfy(row -> assertThat(row.get("name").getStringValue()).containsIgnoringCase("atlas"));
  }

  @Test
  void shouldFilterByStatusCountryAndSegment() {
    assertThat(rows(null, "0", null, null, null, null, null)).isNotEmpty().hasSizeLessThan(AdvancedSearchCustomers.ROW_COUNT)
      .allSatisfy(row -> assertThat(row.get("status").getStringValue()).isEqualTo("0"));
    assertThat(rows(null, null, "FR", null, null, null, null)).isNotEmpty()
      .allSatisfy(row -> assertThat(row.get("country").getStringValue()).isEqualTo("FR"));
    assertThat(rows(null, null, null, "PUB", null, null, null)).isNotEmpty()
      .allSatisfy(row -> assertThat(row.get("segment").getStringValue()).isEqualTo("PUB"));
    // Every value of every list has customers
    for (String country : List.of("ES", "FR", "DE", "IT", "PT", "GB")) {
      assertThat(rows(null, null, country, null, null, null, null)).as(country).hasSizeGreaterThan(5);
    }
    for (String segment : List.of("RET", "WHO", "PUB", "ONL")) {
      assertThat(rows(null, null, null, segment, null, null, null)).as(segment).hasSizeGreaterThan(5);
    }
  }

  @Test
  void shouldFilterByAMinimumAmount() {
    List<Map<String, CellData>> rows = rows(null, null, null, null, "5000", null, null);

    assertThat(rows).isNotEmpty().hasSizeLessThan(AdvancedSearchCustomers.ROW_COUNT)
      .allSatisfy(row -> assertThat(row.get("amount").getDoubleValue()).isGreaterThanOrEqualTo(5000));
    assertThat(rows(null, null, null, null, "5000.0", null, null)).hasSameSizeAs(rows);
  }

  @Test
  void shouldFilterByTheCreationDateFromADay() {
    LocalDate from = LocalDate.of(2025, 6, 1);
    List<Map<String, CellData>> rows = rows(null, null, null, null, null, "01/06/2025", null);

    assertThat(rows).isNotEmpty().hasSizeLessThan(AdvancedSearchCustomers.ROW_COUNT)
      .allSatisfy(row -> assertThat(LocalDate.parse(row.get("created").getStringValue(), WEB_DATE)).isAfterOrEqualTo(from));
  }

  @Test
  void shouldKeepOnlyPriorityCustomersWithTheFlagAndEveryOneOfThemIsActive() {
    List<Map<String, CellData>> rows = rows(null, null, null, null, null, null, "1");

    assertThat(rows).isNotEmpty().hasSizeLessThan(AdvancedSearchCustomers.ROW_COUNT)
      .allSatisfy(row -> {
        assertThat(row.get("priority").getStringValue()).isEqualTo("1");
        assertThat(row.get("status").getStringValue()).isEqualTo("1");
      });
    assertThat(rows(null, "0", null, null, null, null, "1")).isEmpty();
  }

  @Test
  void shouldCombineEveryFilter() {
    List<Map<String, CellData>> rows = rows(null, "1", "ES", null, "1000", null, null);

    assertThat(rows).isNotEmpty().hasSizeLessThan(rows(null, null, "ES", null, null, null, null).size())
      .allSatisfy(row -> {
        assertThat(row.get("status").getStringValue()).isEqualTo("1");
        assertThat(row.get("country").getStringValue()).isEqualTo("ES");
        assertThat(row.get("amount").getDoubleValue()).isGreaterThanOrEqualTo(1000);
      });
  }
}
