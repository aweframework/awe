package com.almis.awe.model.entities.actions;

import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * @author pgarcia and pvidal
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class ComponentAddress implements Serializable {

  private static final long serialVersionUID = 5241963594540611025L;

  private String application;
  private String session;
  private String view;
  private String screen;
  private String component;
  private String row;
  private String column;

  /**
   * Copy constructor
   *
   * @param other ComponentAddress object
   */
  public ComponentAddress(ComponentAddress other) {
    this.application = other.application;
    this.session = other.session;
    this.view = other.view;
    this.screen = other.screen;
    this.component = other.component;
    this.row = other.row;
    this.column = other.column;
  }

  /**
   * Constructor
   *
   * @param view      Component view
   * @param component Component id
   * @param row       Row
   * @param column    Column
   */
  public ComponentAddress(String view, String component, String row, String column) {
    super();
    this.view = view;
    this.component = component;
    this.row = row;
    this.column = column;
  }

  /**
   * Generate component address from json value
   *
   * @param address Json address
   * @return Component address
   */
  public static ComponentAddress fromJson(JsonNode address) {
    try {
      return DataListUtil.getMapper().treeToValue(address, ComponentAddress.class);
    } catch (Exception exc) {
      log.error("Error reading component address: {}", address);
      return new ComponentAddress();
    }
  }
}
