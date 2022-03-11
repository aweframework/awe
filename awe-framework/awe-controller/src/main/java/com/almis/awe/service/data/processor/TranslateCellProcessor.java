package com.almis.awe.service.data.processor;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.queries.OutputField;

/**
 * TransformCellProcessor class
 */
public class TranslateCellProcessor implements CellProcessor {

  private final OutputField field;
  private final EnumeratedGroup translateEnumerated;

  // Autowired services
  private final AweElements elements;

  /**
   * Translate cell processor
   *
   * @param elements AWE elements
   * @param field    Output field
   * @param translateEnumerated    Translated enumerated
   */
  public TranslateCellProcessor(AweElements elements, OutputField field, EnumeratedGroup translateEnumerated) {
    this.elements = elements;
    this.field = field;
    this.translateEnumerated = translateEnumerated;
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

    // Get translated label
    String label = translateEnumerated.findLabel(value);
    cell.setValue(elements.getLocaleWithLanguage(label, elements.getLanguage()));

    // Store computed in row
    return cell;
  }
}
