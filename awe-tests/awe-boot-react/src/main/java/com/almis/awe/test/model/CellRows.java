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
 * Rows of users with a secondary line, a status and a list of teams, to test the grid cells with a secondary line, a
 * status and chips (issue #832)
 */
@Service
public class CellRows extends ServiceConfig {

  /**
   * Rows of the page: id, user, login (secondary line), status, tone and teams (chips). The fifth row has a tone that does
   * not exist (shown as neutral), the second one an empty team between two others and the third one a team with HTML,
   * which must show as text; the fourth one has no login, so its user cell shows only the main line
   */
  private static final List<String[]> ROWS = List.of(
    new String[]{"Javier Garc\u00eda", "jgarcia \u00b7 j.garcia@almis.example", "Locked", "danger", "Sales, Finance, Logistics, Support"},
    new String[]{"Laura P\u00e9rez", "lperez \u00b7 l.perez@almis.example", "Active", "success", "A,,B"},
    new String[]{"Marta Ruiz", "mruiz \u00b7 m.ruiz@almis.example", "Pending activation", "info", "<b>bold</b>"},
    new String[]{"Pablo D\u00edaz", "", "Inactive", "neutral", "Sales"},
    new String[]{"Ana Torres", "atorres \u00b7 a.torres@almis.example", "Under review", "bogus", "Finance, Support"},
    new String[]{"Carlos Gil", "cgil \u00b7 c.gil@almis.example", "Expiring", "warning", "Logistics"});

  /**
   * Build a row of the page
   *
   * @param index Row number, from 1
   * @return Row
   */
  public static Map<String, CellData> row(int index) {
    String[] values = ROWS.get(index - 1);
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(index));
    row.put("user", new CellData(values[0]));
    row.put("login", new CellData(values[1]));
    row.put("status", new CellData(values[2]));
    row.put("tone", new CellData(values[3]));
    row.put("teams", new CellData(values[4]));
    return row;
  }

  /**
   * Load the rows
   *
   * @return Rows of the page
   */
  public ServiceData loadCellRows() {
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
