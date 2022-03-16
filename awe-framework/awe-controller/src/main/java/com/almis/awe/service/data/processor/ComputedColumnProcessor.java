/*
 * Package definition
 */
package com.almis.awe.service.data.processor;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.QueryParameter;
import com.almis.awe.model.entities.queries.Computed;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.regex.Matcher;

/**
 * Computed column class
 */
public class ComputedColumnProcessor implements ColumnProcessor {
  private Computed computed;
  private final Map<String, QueryParameter> variableMap;
  private TransformCellProcessor transformProcessor;
  private TranslateCellProcessor translateProcessor;
  private String expression = null;

  // Autowired services
  private final AweElements elements;
  private final BaseConfigProperties baseConfigProperties;
  private final NumericService numericService;
  private final EncodeService encodeService;

  /**
   * Computed column processor constructor
   *
   * @param elements             AWE elements
   * @param baseConfigProperties Base config properties
   * @param computed             Computed element
   * @param variables            Query variables
   * @param numericService       Numeric service
   * @param encodeService        Encode service
   * @throws AWException AWE exception
   */
  public ComputedColumnProcessor(AweElements elements, BaseConfigProperties baseConfigProperties, Computed computed, Map<String, QueryParameter> variables, NumericService numericService, EncodeService encodeService) throws AWException {
    this.elements = elements;
    this.baseConfigProperties = baseConfigProperties;
    this.variableMap = variables;
    this.numericService = numericService;
    this.encodeService = encodeService;
    setComputed(computed);
  }

  /**
   * Set computed
   *
   * @param computed Computed field
   * @return Computed processor
   * @throws AWException AWE exception
   */
  public ComputedColumnProcessor setComputed(Computed computed) throws AWException {
    this.computed = computed;

    // Calculate transform
    if (computed.isTransform()) {
      transformProcessor = new TransformCellProcessor(elements, computed, numericService, encodeService);
    }

    // Calculate translate
    if (computed.isTranslate()) {
      translateProcessor = new TranslateCellProcessor(elements, computed, elements.getEnumerated(computed.getTranslate()));
    }

    // Generate format matcher
    expression = computed.getFormat();
    return this;
  }

  /**
   * Retrieve column identifier
   *
   * @return Column identifier
   */
  public String getColumnIdentifier() {
    return computed.getAlias();
  }

  /**
   * Process row
   *
   * @param row Data row
   * @throws AWException AWE exception
   */
  public CellData process(Map<String, CellData> row) throws AWException {

    // Replace the expression with values
    String computedExpression = computeExpression(row, expression);

    // Evaluate the expression if eval type is defined
    CellData evaluatedExpression = evaluateExpression(computedExpression);

    // Calculate transform
    if (transformProcessor != null) {
      evaluatedExpression = transformProcessor.process(evaluatedExpression);
    }

    // Calculate translate
    if (translateProcessor != null) {
      evaluatedExpression = translateProcessor.process(evaluatedExpression);
    }

    // Store computed in row
    return evaluatedExpression;
  }

  /**
   * Replace the expression with row values
   *
   * @param row   Row values
   * @param value Expression
   * @return Expression replaced
   */
  private String computeExpression(Map<String, CellData> row, String value) {
    // Create the matcher
    String computedExpression = value;
    Matcher formatMatcher = AweConstants.DATALIST_COMPUTED_WILDCARD.matcher(StringUtil.fixHTMLValue(expression));

    // Replace all expression variables
    while (formatMatcher.find()) {
      for (int matchIndex = 1, total = formatMatcher.groupCount(); matchIndex <= total; matchIndex++) {
        String variableKey = formatMatcher.group(matchIndex);
        String variableValue = "";
        CellData cell = row.get(variableKey);

        // Check if fill with variable or cell
        if (variableMap != null && variableMap.containsKey(variableKey)) {
          // Fill with variable value
          variableValue = variableMap.get(variableKey).getValue().asText();
        } else if (cell != null && !cell.getStringValue().isEmpty()) {
          // Fill with cell value
          variableValue = cell.getStringValue();
        } else if (computed.getNullValue() != null) {
          // Fill with null value
          variableValue = computed.getNullValue();
        }

        // If variable value is empty, empty the computed value
        if (variableValue.isEmpty() && baseConfigProperties.getComponent().isComputedEmptyIfNull()) {
          computedExpression = "";
        } else {
          // Replace value
          computedExpression = computedExpression.replace("[" + variableKey + "]", variableValue);
        }
      }
    }

    return computedExpression;
  }

  /**
   * Evaluate expression
   *
   * @param value Value
   * @return Evaluated expression
   * @throws AWException Error parsing value
   */
  private CellData evaluateExpression(String value) throws AWException {
    CellData evaluatedExpression = new CellData();
    if (computed.isEval()) {
      Value evaluated;
      try {
        evaluated = StringUtil.eval(value, elements.getApplicationContext().getBean(Context.class));
      } catch (Exception exc) {
        throw new AWException(elements.getLocaleWithLanguage("ERROR_TITLE_EXPRESSION_EVALUATION", elements.getLanguage()),
          elements.getLocaleWithLanguage("ERROR_MESSAGE_EXPRESSION_EVALUATION", elements.getLanguage(), value), exc);
      }
      if (evaluated == null) {
        evaluatedExpression.setNull();
      } else if (evaluated.fitsInInt()) {
        evaluatedExpression.setValue(evaluated.asInt());
      } else if (evaluated.fitsInFloat()) {
        evaluatedExpression.setValue(evaluated.asFloat());
      } else if (evaluated.fitsInLong()) {
        evaluatedExpression.setValue(evaluated.asLong());
      } else if (evaluated.fitsInDouble()) {
        evaluatedExpression.setValue(evaluated.asDouble());
      } else if (evaluated.isBoolean()) {
        evaluatedExpression.setValue(evaluated.asBoolean());
      } else if (evaluated.isNull()) {
        evaluatedExpression.setValue(null);
      } else {
        evaluatedExpression.setValue(evaluated.asString());
      }
    } else {
      evaluatedExpression.setValue(value);
    }

    return evaluatedExpression;
  }
}
