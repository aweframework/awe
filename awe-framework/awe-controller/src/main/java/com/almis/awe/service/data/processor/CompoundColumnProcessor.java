/*
 * Package definition
 */
package com.almis.awe.service.data.processor;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.Compound;
import com.almis.awe.model.entities.queries.Computed;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Computed column class
 */
public class CompoundColumnProcessor implements ColumnProcessor {

  private Compound compound;
  private final Map<String, QueryParameter> variableMap;
  private Map<String, ComputedColumnProcessor> computedMap;

  // Autowired services
  private final AweElements elements;
  private final BaseConfigProperties baseConfigProperties;
  private final NumericService numericService;
  private final EncodeService encodeService;

  /**
   * Compound column processor constructor
   *
   * @param elements             AWE elements
   * @param baseConfigProperties Base config properties
   * @param variableMap          Variables map
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   * @throws AWException AWE exception
   */
  public CompoundColumnProcessor(AweElements elements, BaseConfigProperties baseConfigProperties, Compound compound, Map<String, QueryParameter> variableMap, NumericService numericService, EncodeService encodeService) throws AWException {
    this.elements = elements;
    this.baseConfigProperties = baseConfigProperties;
    this.variableMap = variableMap;
    this.numericService = numericService;
    this.encodeService = encodeService;
    setCompound(compound);
  }

  /**
   * Set compound
   * @param compound Compound field
   * @return CompoundColumnProcessor
   * @throws AWException Error adding compound field
   */
  public CompoundColumnProcessor setCompound(Compound compound) throws AWException {
    this.compound = compound;
    if (compound.getComputedList() != null) {
      for (Computed computed : compound.getComputedList()) {

        // Calculate computed
        ComputedColumnProcessor computedProcessor = new ComputedColumnProcessor(elements, baseConfigProperties, computed, variableMap, numericService, encodeService);
        if (computedMap == null) {
          computedMap = new HashMap<>();
        }
        computedMap.put(computed.getIdentifier(), computedProcessor);
      }
    }
    return this;
  }

  /**
   * Retrieve column identifier
   * @return column identifier
   */
  public String getColumnIdentifier() {
    return compound.getIdentifier();
  }

  /**
   * Process row
   * @param row datalist row
   */
  public CellData process(Map<String, CellData> row) throws AWException {
    CellData compoundCell = new CellData();
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode compoundData = JsonNodeFactory.instance.objectNode();
    if (compound.getComputedList() != null) {
      for (Computed computed : compound.getComputedList()) {

        // Computed alias
        String computedIdentifier = computed.getIdentifier();

        // Calculate computed
        CellData computedData = computedMap.get(computedIdentifier).process(row);

        // Store computed data on compound
        JsonNode computedValue = mapper.valueToTree(computedData);
        compoundData.set(computedIdentifier, computedValue);
      }
    }
    compoundCell.setValue(compoundData);
    return compoundCell;
  }
}
