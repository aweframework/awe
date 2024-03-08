package com.almis.awe.service.data.processor;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.queries.OutputField;
import com.almis.awe.model.type.CellDataType;
import com.almis.awe.model.type.TransformType;
import com.almis.awe.model.util.data.DateUtil;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.NumericService;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Optional;

/**
 * TransformCellProcessor class
 */
public class TransformCellProcessor implements CellProcessor {

  // Autowired services
  private final OutputField field;
  private final AweElements elements;
  private final NumericService numericService;
  private final EncodeService encodeService;

  /**
   * Transform cell processor constructor
   *
   * @param elements       Awe elements
   * @param field          Output field
   * @param numericService Numeric service
   * @param encodeService  Encode service
   */
  public TransformCellProcessor(AweElements elements, OutputField field, NumericService numericService, EncodeService encodeService) {
    this.elements = elements;
    this.field = field;
    this.numericService = numericService;
    this.encodeService = encodeService;
  }

  /**
   * Retrieve column identifier
   * @return Column identifier
   */
  public String getColumnIdentifier() {
    return field.getIdentifier();
  }

  /**
   * Process cell
   * @param cell cell to be processed
   * @throws AWException AWE exception
   */
  public CellData process(CellData cell) throws AWException {
    String transformed = cell.getStringValue();
    if (transformed != null) {
      // Transform value if needed
      switch (TransformType.valueOf(field.getTransform())) {
        case DATE:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processDate).orElse(transformed);
          break;
          
        case DATE_MS:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processDateMs).orElse(transformed);
          break;
          
        case TIME:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processTime).orElse(transformed);
          break;
          
        case TIMESTAMP:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processTimestamp).orElse(transformed);
          break;

        case TIMESTAMP_MS:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processTimestampMs).orElse(transformed);
          break;
          
        case JS_DATE:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processJavascriptDate).orElse(transformed);
          break;
          
        case JS_TIMESTAMP:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processJavascriptTimestamp).orElse(transformed);
          break;
          
        case GENERIC_DATE:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processGenericDate).orElse(transformed);
          break;
          
        case DATE_RDB:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processRDBDate).orElse(transformed);
          break;

        case ELAPSED_TIME:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processElapsedTime).orElse(transformed);
          break;

        case DATE_SINCE:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processDateSince).orElse(transformed);
          break;

        case NUMBER:
          transformed = Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processNumber).orElse(transformed);
          break;

        case NUMBER_PLAIN:
          transformed =  Optional.of(cell).filter(c -> StringUtils.isNotBlank(c.getStringValue())).map(this::processNumberPlain).orElse(transformed);
          break;

        case BOOLEAN:
          transformed = processBoolean(cell);
          break;

        case TEXT_HTML:
          transformed = StringUtil.toHTMLText(transformed);
          break;

        case TEXT_UNILINE:
          transformed = StringUtil.toUnilineText(transformed);
          break;

        case TEXT_PLAIN:
          transformed = StringUtil.toPlainText(transformed);
          break;

        case MARKDOWN_HTML:
          transformed = StringUtil.evalMarkdown(transformed);
          break;

        case DECRYPT:
          transformed = encodeService.decryptRipEmd160(transformed);
          break;

        case ARRAY:
          cell.setValue(StringUtil.toArrayNode(field.getPattern(), transformed));
          break;

        case LIST:
          cell.setValue(StringUtil.toStringList(field.getPattern(), transformed));
          break;

        default:
          // Do nothing
      }

      if (transformed == null) {
        // Set string value
        cell.setNull();

        // Set no print
        cell.setPrintable(false);
      } else {
        // Set string value
        cell.setStringValue(transformed);

        // Set as printable
        cell.setPrintable(!field.isNoprint());
      }
    }

    // Store transform in row
    return cell;
  }

  /**
   * Process cell as date
   * @param cell Cell
   * @return Transformation
   */
  private String processDate(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2WebDate(date);
    }
    return transformed;
  }

  /**
   * Process cell as elapsed time
   * @param cell Cell
   * @return Transformation
   */
  private String processElapsedTime(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Long elapsed = (Long) cell.getValue();
    if (elapsed != null) {
      cell.setValue(elapsed);
      cell.setSendStringValue(true);
      transformed = DateUtil.elapsedTime(elapsed, elements);
    }
    return transformed;
  }

  /**
   * Process cell date since
   * @param cell Cell
   * @return Transformation
   */
  private String processDateSince(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dateSince(date, elements);
    }
    return transformed;
  }

  /**
   * Process cell as date in milliseconds
   * @param cell Cell
   * @return Transformation
   */
  private String processDateMs(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date.getTime());
      cell.setSendStringValue(false);
      transformed = String.valueOf(date.getTime());
    }
    return transformed;
  }

  /**
   * Process cell as time
   * @param cell Cell
   * @return Transformation
   */
  private String processTime(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2WebTime(date);
    }
    return transformed;
  }

  /**
   * Process cell as timestamp
   * @param cell Cell
   * @return Transformation
   */
  private String processTimestamp(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2WebTimestamp(date);
    }
    return transformed;
  }

  /**
   * Process cell as timestamp with milliseconds
   * @param cell Cell
   * @return Transformation
   */
  private String processTimestampMs(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2WebTimestampMs(date);
    }
    return transformed;
  }

  /**
   * Process cell as javascript date
   * @param cell Cell
   * @return Transformation
   */
  private String processJavascriptDate(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2JsDate(date);
    }
    return transformed;
  }

  /**
   * Process cell as javascript timestamp
   * @param cell Cell
   * @return Transformation
   */
  private String processJavascriptTimestamp(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2JsTimestamp(date);
    }
    return transformed;
  }

  /**
   * Process cell as a generic date
   * @param cell Cell
   * @return Transformation
   */
  private String processGenericDate(@NotNull CellData cell) {
    String transformed = DateUtil.generic2Date(cell.getStringValue(), field.getFormatFrom(), field.getFormatTo());
    cell.setValue(transformed);
    cell.setSendStringValue(true);
    return transformed;
  }

  /**
   * Process cell as rdb date
   * @param cell Cell
   * @return Transformation
   */
  private String processRDBDate(@NotNull CellData cell) {
    String transformed = cell.getStringValue();
    Date date = cell.getDateValue();
    if (date != null) {
      cell.setValue(date);
      cell.setSendStringValue(true);
      transformed = DateUtil.dat2RDBDate(date);
    }
    return transformed;
  }


  /**
   * Process cell as number
   * @param cell Cell
   * @return Transformation
   */
  private String processNumber(@NotNull CellData cell)  {
    Double numericValue = Double.parseDouble(cell.getStringValue());
    if (cell.getType().equals(CellDataType.STRING)) {
      cell.setValue(numericValue);
    }
    cell.setSendStringValue(true);
    return numericService.applyPattern(field.getPattern(), numericValue);
  }

  /**
   * Process cell as number
   * @param cell Cell
   * @return Transformation
   */
  private String processNumberPlain(@NotNull CellData cell)  {
    Double numericValue = Double.parseDouble(cell.getStringValue());
    cell.setValue(numericValue);
    return numericService.applyRawPattern(field.getPattern(), numericValue);
  }

  /**
   * Process cell as number
   * @param cell Cell
   * @return Transformation
   */
  private String processBoolean(@NotNull CellData cell)  {
    switch (cell.getType()) {
      case STRING:
        cell.setValue(Boolean.parseBoolean(cell.getStringValue()));
        break;
      case LONG, DECIMAL, FLOAT, DOUBLE, INTEGER:
        cell.setValue(cell.getIntegerValue() != 0);
        break;
      case BOOLEAN:
        break;
      default:
        cell.setValue(cell.getObjectValue() != null);
        break;
    }
    return cell.getStringValue();
  }
}
