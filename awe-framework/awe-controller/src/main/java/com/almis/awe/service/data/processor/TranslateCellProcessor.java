package com.almis.awe.service.data.processor;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.queries.OutputField;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;

/**
 * TransformCellProcessor class
 */
public class TranslateCellProcessor implements CellProcessor {

  private final OutputField field;
  private final EnumeratedGroup translateEnumerated;
  private final Map<String, QueryParameter> variables;

  // Autowired services
  private final AweElements elements;

  /**
   * Translate cell processor
   *
   * @param elements            AWE elements
   * @param field               Output field
   * @param variables           Variable map
   * @param translateEnumerated Translated enumerated
   */
  public TranslateCellProcessor(AweElements elements, OutputField field, Map<String, QueryParameter> variables, EnumeratedGroup translateEnumerated) {
    this.elements = elements;
    this.field = field;
    this.variables = variables;
    this.translateEnumerated = translateEnumerated;
  }

  /**
   * Get field of translate
   *
   * @return field to be translated
   */
  public OutputField getField() {
    return field;
  }

  /**
   * Get enumerated for translate
   *
   * @return Enumerate element
   */
  public EnumeratedGroup getTranslateEnumerated() {
    return translateEnumerated;
  }

  /**
   * Retrieve column identifier
   *
   * @return column identifier
   */
  public String getColumnIdentifier() {
    return field.getIdentifier();
  }

  /**
   * Process cell
   *
   * @param cell cell data
   * @throws AWException AWE exception
   */
  public CellData process(CellData cell) throws AWException {
    // Get value
    String value = cell.getStringValue();

    // Get language
    String language = Optional.ofNullable(variables.get(AweConstants.QUERY_LANGUAGE))
      .map(QueryParameter::getValue)
      .map(JsonNode::asText)
      .orElse(elements.getLanguage());

    // Get translated label
    String label = translateEnumerated.findLabel(value);
    cell.setValue(elements.getLocaleWithLanguage(label, language));

    // Store computed in row
    return cell;
  }
}
