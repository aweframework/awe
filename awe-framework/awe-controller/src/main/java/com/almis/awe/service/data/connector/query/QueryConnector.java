package com.almis.awe.service.data.connector.query;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.queries.Query;
import tools.jackson.databind.node.ObjectNode;

/**
 * Interface to launch query processes
 *
 * @author pvidal
 */
public interface QueryConnector {

  /**
   * Launch a full query
   *
   * @param query Query to be launched
   * @param parameters Parameters
   * @return ServiceData with complete output
   * @throws AWException Error launching query
   */
  ServiceData launch(Query query, ObjectNode parameters) throws AWException;

  /**
   * Subscribe to a queue
   *
   * @param query Query to be subscribed to
   * @param address Address of the subscribed component
   * @param parameters Parameters
   * @return Service data with execution info
   * @throws AWException Error on subscription
   */
  ServiceData subscribe(Query query, ComponentAddress address, ObjectNode parameters) throws AWException;

  /**
   * Retrieve the distinct values of a grid column (#912): the values of its filtered fields with their rows count and
   * label, with every column filter of the parameters in effect
   *
   * @param query        Query of the grid
   * @param parameters   Parameters, without pagination
   * @param columnValues Column values
   * @return Service data with the values and whether more exist
   * @throws AWException The connector does not list column values, or the query failed
   */
  default ServiceData launchColumnValues(Query query, ObjectNode parameters, ColumnValues columnValues) throws AWException {
    throw new AWEQueryException("Column values", "Column values are not supported by the connector of the query", query.getId(), null);
  }
}
