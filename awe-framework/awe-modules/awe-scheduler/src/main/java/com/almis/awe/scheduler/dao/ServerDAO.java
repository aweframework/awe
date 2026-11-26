package com.almis.awe.scheduler.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_SERVER_ROWS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_SERVER_DATA;

@Slf4j
public class ServerDAO {

  // Autowired services
  private final QueryService queryService;
  private final QueryUtil queryUtil;

  /**
   * Autowired constructor
   *
   * @param queryService    Query service
   * @param queryUtil       Query utilities
   */
  public ServerDAO(QueryService queryService, QueryUtil queryUtil) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
  }

  /**
   * Find server on database
   *
   * @param serverId Server identifier
   * @return Server
   */
  public Server findServer(Integer serverId) throws AWException {
    // Generate parameters
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put("serverId", serverId);

    // Retrieve server
    List<Server> serverList = DataListUtil.asBeanList(queryService.launchPrivateQuery(SCHEDULER_SERVER_DATA, parameters).getDataList(), Server.class);
    if (!serverList.isEmpty()) {
      return serverList.get(0);
    }

    return null;
  }

  /**
   * Find the identifiers of the active servers with a connection to test (a folder has none), read without their
   * secrets
   *
   * @return Active server identifiers, in the order of their identifiers
   * @throws AWException The servers cannot be read
   */
  public List<Integer> findActiveServerIds() throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    return queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_SERVER_ROWS, parameters).getDataList().getRows()
      .stream()
      .filter(row -> row.get("Act") != null && "1".equals(row.get("Act").getStringValue()))
      .filter(row -> row.get("Pro") == null || !"folder".equalsIgnoreCase(row.get("Pro").getStringValue()))
      .map(row -> row.get("IdeSrv").getIntegerValue())
      .toList();
  }
}
