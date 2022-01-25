package com.almis.awe.scheduler.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DatabaseDAO {

  private static final String DB_ALIAS_LIST = "databaseAliasList";

  // Autowired services
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   */
  public DatabaseDAO(QueryService queryService) {
    this.queryService = queryService;
  }

  /**
   * Get database alias list
   *
   * @return Database name
   * @throws AWException AWE exception
   */
  public List<String> getDBAliasList() throws AWException {
    // Launch on the first connection
    return DataListUtil.getColumn(queryService.launchQuery(DB_ALIAS_LIST).getDataList(), "Als")
      .stream()
      .map(CellData::getStringValue)
      .collect(Collectors.toList());
  }
}
