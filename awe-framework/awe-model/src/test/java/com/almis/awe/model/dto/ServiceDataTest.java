package com.almis.awe.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link ServiceData#setData(DataList, Integer)}.
 * <p>
 * {@code DataListUtil.getRow} returns {@code null} when the requested row does not exist,
 * so {@code setData} must not dereference its result blindly.
 */
class ServiceDataTest {

  @Test
  @DisplayName("setData stores the requested row values as an array")
  void setDataStoresRequestedRowValues() {
    DataList dataList = dataListOf(
      row("id", "USR-1", "name", "Alice"),
      row("id", "USR-2", "name", "Bob")
    );

    ServiceData serviceData = new ServiceData().setData(dataList, 1);

    Object[] data = (Object[]) serviceData.getData();
    assertAll(
      () -> assertInstanceOf(Object[].class, serviceData.getData()),
      () -> assertArrayEquals(new Object[]{"Bob", "USR-2"}, sorted(data))
    );
  }

  @Test
  @DisplayName("setData on an empty data list leaves an empty data array instead of throwing")
  void setDataOnEmptyDataListDoesNotThrow() {
    DataList dataList = new DataList();

    ServiceData serviceData = assertDoesNotThrow(() -> new ServiceData().setData(dataList, 0));

    assertArrayEquals(new Object[0], (Object[]) serviceData.getData());
  }

  @Test
  @DisplayName("setData with a row index beyond the last row leaves an empty data array instead of throwing")
  void setDataWithRowIndexOutOfRangeDoesNotThrow() {
    DataList dataList = dataListOf(row("id", "USR-1"));

    ServiceData serviceData = assertDoesNotThrow(() -> new ServiceData().setData(dataList, 5));

    assertArrayEquals(new Object[0], (Object[]) serviceData.getData());
  }

  @Test
  @DisplayName("setData with a null row number keeps data untouched")
  void setDataWithNullRowNumberKeepsDataUntouched() {
    DataList dataList = dataListOf(row("id", "USR-1"));

    ServiceData serviceData = new ServiceData().setData(dataList, (Integer) null);

    assertNull(serviceData.getData());
  }

  private DataList dataListOf(Map<String, CellData> row) {
    DataList dataList = new DataList();
    dataList.addRow(new HashMap<>(row));
    return dataList;
  }

  private DataList dataListOf(Map<String, CellData> first, Map<String, CellData> second) {
    DataList dataList = dataListOf(first);
    dataList.addRow(new HashMap<>(second));
    return dataList;
  }

  private Map<String, CellData> row(Object... columns) {
    Map<String, CellData> row = new HashMap<>();
    for (int index = 0; index < columns.length; index += 2) {
      row.put((String) columns[index], new CellData().setValue(columns[index + 1]));
    }
    return row;
  }

  /**
   * Row columns are stored in a {@link HashMap}, so iteration order is not guaranteed.
   * Sorting keeps the assertion deterministic without asserting on map ordering.
   */
  private Object[] sorted(Object[] data) {
    Object[] copy = data.clone();
    Arrays.sort(copy);
    return copy;
  }
}
