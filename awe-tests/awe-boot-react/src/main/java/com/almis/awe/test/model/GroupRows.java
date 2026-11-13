package com.almis.awe.test.model;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sales of stores by region, country and city, to test the grid row grouping with subtotals (issue #833)
 */
@Service
public class GroupRows extends ServiceConfig {

  /**
   * Rows of the grid: region, country, city, sales, units and price. The totals are round so the browser tests can add
   * them up: Europe 850 (Spain 500, France 350), America 1600 (Canada 800, Mexico 550, USA 250), 2450 in all. Sorted by
   * their sales, the regions and the countries of America go in the reverse order of their names
   */
  private static final List<Object[]> ROWS = List.of(
    new Object[]{"Europe", "Spain", "Madrid", 120, 12, 10},
    new Object[]{"Europe", "Spain", "Madrid", 100, 10, 20},
    new Object[]{"Europe", "Spain", "Madrid", 80, 8, 30},
    new Object[]{"Europe", "Spain", "Barcelona", 90, 9, 40},
    new Object[]{"Europe", "Spain", "Barcelona", 60, 6, 50},
    new Object[]{"Europe", "Spain", "Barcelona", 50, 5, 60},
    new Object[]{"Europe", "France", "Paris", 150, 15, 15},
    new Object[]{"Europe", "France", "Paris", 70, 7, 25},
    new Object[]{"Europe", "France", "Paris", 30, 3, 35},
    new Object[]{"Europe", "France", "Lyon", 40, 4, 20},
    new Object[]{"Europe", "France", "Lyon", 60, 6, 30},
    new Object[]{"America", "USA", "New York", 60, 6, 20},
    new Object[]{"America", "USA", "New York", 40, 4, 20},
    new Object[]{"America", "USA", "New York", 50, 5, 20},
    new Object[]{"America", "USA", "Chicago", 50, 5, 20},
    new Object[]{"America", "USA", "Chicago", 50, 5, 20},
    new Object[]{"America", "Mexico", "Monterrey", 200, 20, 20},
    new Object[]{"America", "Mexico", "Monterrey", 150, 15, 20},
    new Object[]{"America", "Mexico", "Guadalajara", 100, 10, 20},
    new Object[]{"America", "Mexico", "Guadalajara", 100, 10, 20},
    new Object[]{"America", "Canada", "Toronto", 300, 30, 20},
    new Object[]{"America", "Canada", "Toronto", 200, 20, 20},
    new Object[]{"America", "Canada", "Vancouver", 150, 15, 20},
    new Object[]{"America", "Canada", "Vancouver", 150, 15, 20});

  /**
   * Build a row of the grid
   *
   * @param index Row number, from 1
   * @return Row
   */
  public static Map<String, CellData> row(int index) {
    Object[] values = ROWS.get(index - 1);
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(index));
    row.put("region", new CellData((String) values[0]));
    row.put("country", new CellData((String) values[1]));
    row.put("city", new CellData((String) values[2]));
    row.put("store", new CellData("Store " + index));
    row.put("sales", new CellData((Integer) values[3]));
    row.put("units", new CellData((Integer) values[4]));
    row.put("price", new CellData((Integer) values[5]));
    return row;
  }

  /**
   * Load the rows
   *
   * @return Rows of the grid
   */
  public ServiceData loadGroupRows() {
    DataList dataList = new DataList();
    for (int i = 1; i <= ROWS.size(); i++) {
      dataList.addRow(row(i));
    }
    dataList.setRecords(ROWS.size());
    ServiceData serviceData = new ServiceData();
    serviceData.setDataList(dataList);
    return serviceData;
  }
}
