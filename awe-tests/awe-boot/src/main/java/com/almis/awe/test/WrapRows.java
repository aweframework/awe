package com.almis.awe.test;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Rows with texts of very different lengths, to test the grid columns that wrap over several lines (issue #825)
 */
@Service
public class WrapRows extends ServiceConfig {

  /**
   * Number of rows of the page
   */
  public static final int ROW_COUNT = 100;

  private static final String[] WORDS = {"alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel", "india",
    "juliet", "kilo", "lima"};

  /**
   * Build a text of consecutive words
   *
   * @param start Index of the first word
   * @param count Number of words
   * @return Text
   */
  public static String words(int start, int count) {
    StringBuilder text = new StringBuilder();
    for (int i = 0; i < count; i++) {
      text.append(i == 0 ? "" : " ").append(WORDS[(start + i) % WORDS.length]);
    }
    return text.toString();
  }

  /**
   * Build the row of the page
   *
   * @param index Row number, from 1
   * @return Row
   */
  public static Map<String, CellData> row(int index) {
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(index));
    row.put("code", new CellData(String.format("WR-%03d", index)));
    row.put("title", new CellData(words(index, 3 + index % 9)));
    row.put("description", new CellData(words(index * 7, 6 + (index % 6) * 14)));
    row.put("notes", new CellData(words(index * 3, 8)));
    return row;
  }

  /**
   * Load the rows
   *
   * @return Rows of the page
   */
  public ServiceData loadWrapRows() {
    DataList dataList = new DataList();
    for (int i = 1; i <= ROW_COUNT; i++) {
      dataList.addRow(row(i));
    }
    dataList.setRecords(ROW_COUNT);
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(dataList);
    return serviceData;
  }
}
