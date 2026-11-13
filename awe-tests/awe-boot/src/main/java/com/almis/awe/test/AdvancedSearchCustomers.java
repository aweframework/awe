package com.almis.awe.test;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Customers of the advanced search test screen (#790): a fixed dataset, the same on every call, filtered by every
 * criterion of the screen (a blank filter is ignored). The query paginates and sorts the result.
 */
@Service
public class AdvancedSearchCustomers extends ServiceConfig {

  /**
   * Number of customers
   */
  public static final int ROW_COUNT = 120;

  private static final String[] PREFIXES = {"Atlas", "Boreal", "Cobalt", "Delta", "Ember", "Fjord", "Granite", "Harbor",
    "Iris", "Juniper", "Krypton", "Lumen"};
  private static final String[] SUFFIXES = {"Logistics", "Foods", "Labs", "Retail", "Motors", "Energy", "Textiles",
    "Systems", "Pharma", "Media"};
  private static final String[] COUNTRIES = {"ES", "FR", "DE", "IT", "PT", "GB"};
  private static final String[] SEGMENTS = {"RET", "WHO", "PUB", "ONL"};
  private static final String ACTIVE = "1";
  private static final String INACTIVE = "0";
  private static final LocalDate FIRST_DAY = LocalDate.of(2025, 1, 1);
  private static final DateTimeFormatter WEB_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Customer of the dataset
   *
   * @param index Customer number, from 0
   * @return Customer
   */
  private static Map<String, CellData> row(int index) {
    boolean active = index % 4 != 3;
    LocalDate created = FIRST_DAY.plusDays((index * 37L) % 540);
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(index + 1));
    // Every prefix with every suffix once
    row.put("name", new CellData(PREFIXES[index % PREFIXES.length] + " "
      + SUFFIXES[(index / PREFIXES.length + index % PREFIXES.length) % SUFFIXES.length]));
    row.put("status", new CellData(active ? ACTIVE : INACTIVE));
    row.put("country", new CellData(COUNTRIES[(index * 5 + index / 24) % COUNTRIES.length]));
    row.put("segment", new CellData(SEGMENTS[(index * 3 + index / 12) % SEGMENTS.length]));
    row.put("amount", new CellData(250 + ((index * 7919L) % 975000) / 100.0));
    row.put("created", new CellData(created.format(WEB_DATE)));
    row.put("createdSort", new CellData(created.toString()));
    // Only active customers are priority
    row.put("priority", new CellData(active && index % 5 == 0 ? ACTIVE : INACTIVE));
    return row;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /**
   * Customers that pass every filter in use
   *
   * @param name        Part of the name, ignoring the case
   * @param status      Status (1 active, 0 inactive)
   * @param country     Country code
   * @param segment     Segment code
   * @param minAmount   Minimum amount
   * @param createdFrom First creation day (dd/MM/yyyy)
   * @param priority    1 to keep only priority customers
   * @return Customers
   */
  public ServiceData getCustomers(String name, String status, String country, String segment, String minAmount,
                                  String createdFrom, String priority) {
    List<Predicate<Map<String, CellData>>> filters = new ArrayList<>();
    if (!isBlank(name)) {
      String part = name.trim().toLowerCase(Locale.ROOT);
      filters.add(row -> row.get("name").getStringValue().toLowerCase(Locale.ROOT).contains(part));
    }
    addEqualsFilter(filters, "status", status);
    addEqualsFilter(filters, "country", country);
    addEqualsFilter(filters, "segment", segment);
    if (!isBlank(minAmount)) {
      double minimum = Double.parseDouble(minAmount.trim());
      filters.add(row -> row.get("amount").getDoubleValue() >= minimum);
    }
    if (!isBlank(createdFrom)) {
      LocalDate from = LocalDate.parse(createdFrom.trim(), WEB_DATE);
      filters.add(row -> !LocalDate.parse(row.get("created").getStringValue(), WEB_DATE).isBefore(from));
    }
    if (ACTIVE.equals(priority)) {
      filters.add(row -> ACTIVE.equals(row.get("priority").getStringValue()));
    }

    DataList dataList = new DataList();
    for (int i = 0; i < ROW_COUNT; i++) {
      Map<String, CellData> row = row(i);
      if (filters.stream().allMatch(filter -> filter.test(row))) {
        dataList.addRow(row);
      }
    }
    dataList.setRecords(dataList.getRows().size());
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(dataList);
    return serviceData;
  }

  private static void addEqualsFilter(List<Predicate<Map<String, CellData>>> filters, String column, String value) {
    if (!isBlank(value)) {
      String expected = value.trim();
      filters.add(row -> expected.equals(row.get(column).getStringValue()));
    }
  }
}
