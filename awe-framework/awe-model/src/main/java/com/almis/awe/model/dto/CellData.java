package com.almis.awe.model.dto;

import com.almis.awe.model.entities.Copyable;
import com.almis.awe.model.type.CellDataType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.DateUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static com.almis.awe.model.type.CellDataType.*;

/**
 * CellData Class
 * CellData as an standard data output
 *
 * @author Pablo GARCIA - 24/JUN/2010
 */
@Data
@Accessors(chain = true)
@Slf4j
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class CellData implements Comparable<CellData>, Copyable {

  // Printable
  private boolean printable = true;

  // Send String value (formatted)
  private boolean sendStringValue;

  // String value
  private String stringValue;

  // Object value
  private Object objectValue = null;

  // Cell type
  private CellDataType type = NULL;

  /**
   * Constructor: fast initialization
   */
  public CellData() {
    setNull();
  }

  /**
   * Constructor: fast initialization
   *
   * @param <T>   Cell value type
   * @param value Date value
   */
  @JsonCreator
  public <T> CellData(T value) {
    setValue(value);
  }

  /**
   * Constructor: for C JNI calls
   *
   * @param value Integer value
   */
  public CellData(Integer value) {
    setValue(value);
  }

  /**
   * Constructor: for C JNI calls
   *
   * @param value Float value
   */
  public CellData(Float value) {
    setValue(value);
  }

  /**
   * Constructor: for C JNI calls
   *
   * @param value Date value
   */
  public CellData(Date value) {
    setValue(value);
  }

  /**
   * Constructor: for C JNI calls
   *
   * @param value String value
   */
  public CellData(String value) {
    setValue(value);
  }

  /**
   * Retrieve the string value
   *
   * @return CellData value as String
   */
  @JsonIgnore
  public String getStringValue() {
    return stringValue.trim();
  }

  /**
   * Returns the value casted as double
   *
   * @return CellData value as Object
   */
  @JsonIgnore
  public Double getDoubleValue() {
    return switch (getType()) {
      // Get value as double
      case DOUBLE -> (Double) getObjectValue();
      // Get float value as double
      case FLOAT -> ((Float) getObjectValue()).doubleValue();
      // Get integer value as double
      case INTEGER -> ((Integer) getObjectValue()).doubleValue();
      // Get long value as double
      case LONG -> ((Long) getObjectValue()).doubleValue();
      // Get long value as double
      case DECIMAL -> ((BigDecimal) getObjectValue()).doubleValue();
      // If defaulted, set to null
      default -> null;
    };
  }

  /**
   * Returns the value casted as integer
   *
   * @return CellData value as Integer
   */
  @JsonIgnore
  public Integer getIntegerValue() {
    return switch (getType()) {
      // Get value as double
      case DOUBLE -> ((Double) getObjectValue()).intValue();
      // Get float value as double
      case FLOAT -> ((Float) getObjectValue()).intValue();
      // Get integer value as double
      case INTEGER -> (Integer) getObjectValue();
      // Get long value as double
      case LONG -> ((Long) getObjectValue()).intValue();
      // Get long value as double
      case DECIMAL -> ((BigDecimal) getObjectValue()).intValue();
      // Get integer value from json node
      case JSON -> ((JsonNode) getObjectValue()).intValue();
      // If default, set to null
      default -> null;
    };
  }

  /**
   * Returns the value casted as date
   *
   * @return CellData value as Date
   */
  @JsonIgnore
  public Date getDateValue() {
    switch (getType()) {
      // Get value as date
      case DATE:
        return (Date) getObjectValue();
      // Get value as object
      case OBJECT:
        if (getObjectValue() instanceof ByteArrayInputStream) {
          log.error("Could not parse date from {}: {}", getObjectValue(), getObjectValue().getClass().getName());
          return null;
        } else if (getObjectValue() instanceof LocalDateTime) {
          return DateUtil.asUtilDate(getObjectValue());
      } else return stringToDate(getStringValue());
        // Get value as date
      case STRING:
      default:
        return stringToDate(getStringValue());
    }
  }

  /**
   * Returns the value casted as date
   *
   * @return CellData value as Date
   */
  private Date stringToDate(String dateString) {
    return DateUtil.autoDetectDateFormat(dateString);
  }

  /**
   * Returns the value casted as string
   *
   * @return CellData value as String
   */
  private String dateToString(Date date) {
    return DateUtil.jsonDate(date);
  }

  private void setValue(Object value, CellDataType type) {
    setValue(value.toString(), value, type);
  }

  private void setValue(String stringValue, Object value, CellDataType type) {
    setStringValue(stringValue);
    setObjectValue(value);
    setType(type);
  }

  /**
   * Make object value setter as private
   *
   * @param value Object value
   */
  private void setObjectValue(Object value) {
    this.objectValue = value;
  }

  /**
   * Stores a null value
   *
   * @return this
   */
  @JsonIgnore
  public CellData setNull() {
    setValue("", null, NULL);
    return this;
  }

  /**
   * Request if cell is printable
   *
   * @return the printable
   */
  @JsonIgnore
  public boolean isEmpty() {
    return getStringValue().isEmpty();
  }

  /**
   * Compare to another cell to see which one is higher
   *
   * @param cell2 Cell to compare to
   * @return Comparison
   */
  @Override
  public int compareTo(CellData cell2) {
    return switch (getType()) {
      // Compare as date
      case DATE -> compareObjects(getObjectValue(), cell2.getObjectValue(), Date.class);
      // Compare as double
      case DOUBLE -> compareObjects(getObjectValue(), cell2.getObjectValue(), Double.class);
      // Compare as decimal
      case DECIMAL -> compareObjects(getObjectValue(), cell2.getObjectValue(), BigDecimal.class);
      // Compare as float
      case FLOAT -> compareObjects(getObjectValue(), cell2.getObjectValue(), Float.class);
      // Compare as integer
      case INTEGER -> compareObjects(getObjectValue(), cell2.getObjectValue(), Integer.class);
      // Compare as long
      case LONG -> compareObjects(getObjectValue(), cell2.getObjectValue(), Long.class);
      // Compare as string
      default -> {
        String string2 = cell2.getStringValue();
        yield getStringValue().compareTo(string2);
      }
    };
  }

  /**
   * Compare two cells
   *
   * @param object1     Object 1 to compare
   * @param object2     Object 2 to compare
   * @param objectClass Object classes
   * @return Comparison result
   */
  private int compareObjects(Object object1, Object object2, Class<?> objectClass) {
    int comparison;
    if (object2 == null) {
      comparison = 1;
    } else {
      try {
        Method compareTo = object1.getClass().getMethod("compareTo", objectClass);
        comparison = (int) compareTo.invoke(object1, object2);
      } catch (Exception exc) {
        log.warn("Can't compare classes '{}' ({}) and '{}' ({}). Comparing string values", object1, object1.getClass().getSimpleName(), object2, object2.getClass().getSimpleName(), exc);
        comparison = object1.toString().compareTo(object2.toString());
      }
    }
    return comparison;
  }

  /**
   * Returns the object value
   *
   * @return Total pages
   */
  @JsonValue
  public Object getValue() {
    if (isSendStringValue()) {
      return getStringValue();
    } else {
      return switch (getType()) {
        // Get object value
        case DOUBLE, FLOAT, INTEGER, LONG, DECIMAL, JSON, OBJECT, BOOLEAN -> getObjectValue();
        // Get json value as null
        case NULL -> null;
        // Get json value as string
        case DATE -> dateToString(getDateValue());
        default -> getStringValue();
      };
    }
  }

  /**
   * Stores an object value
   *
   * @param value Object value
   * @return this
   */
  @JsonIgnore
  public CellData setValue(Object value) {
    if (value == null) {
      setNull();
    } else if (value instanceof String strValue) {
      setStringValue(value, strValue);
    } else if (value instanceof Integer) {
      setValue(value, INTEGER);
    } else if (value instanceof Long) {
      setValue(value, LONG);
    } else if (value instanceof BigDecimal) {
      setValue(value, DECIMAL);
    } else if (value instanceof Float) {
      setValue(value, FLOAT);
    } else if (value instanceof Double) {
      setValue(value, DOUBLE);
    } else if (value instanceof Boolean) {
      setValue(value, BOOLEAN);
    } else if (value instanceof Date date) {
      String dateString = DateUtil.dat2WebTimestamp(date);
      setValue(dateString, date, DATE);
    } else if (value instanceof LocalDateTime date) {
      setValue(DateUtil.localDatetime2WebTimestamp(date), DateUtil.asUtilDate(date), DATE);
    } else if (value instanceof LocalDate date) {
      setValue(DateUtil.localDate2WebDate(date), DateUtil.asUtilDate(date), DATE);
    } else if (value instanceof JsonNode) {
      setValue(value, JSON);
    } else if (value instanceof CellData cell) {
      setValue(cell.getStringValue(), cell.getObjectValue(), cell.getType());
      setSendStringValue(cell.isSendStringValue());
      setPrintable(cell.isPrintable());
    } else {
      log.debug("CellData of type '{}'", value.getClass().getSimpleName());
      setValue(value.toString(), value, OBJECT);
    }
    return this;
  }

  /**
   * Set string value checking if is a date
   * @param value Value to set
   * @param strValue Value as string
   */
  private void setStringValue(Object value, String strValue) {
    if (DateUtil.isJsonDate(strValue)) {
      Date date = stringToDate(strValue);
      setValue(date);
    } else {
      setValue(value, STRING);
    }
  }

  @Override
  public CellData copy() {
    return new CellData(this);
  }

  private void writeObject(@NotNull ObjectOutputStream stream) throws IOException {

    stream.writeBoolean(printable);
    stream.writeBoolean(sendStringValue);
    stream.writeObject(stringValue);
    stream.writeObject(type);

    switch (getType()) {
      // Get object value
      case DOUBLE:
        stream.writeDouble((Double) objectValue);
        break;
      case FLOAT:
        stream.writeFloat((Float) objectValue);
        break;
      case INTEGER:
        stream.writeInt((Integer) objectValue);
        break;
      case LONG:
        stream.writeLong((Long) objectValue);
        break;
      case DECIMAL:
        BigDecimal bigDecimal = (BigDecimal) objectValue;
        BigInteger value = bigDecimal.unscaledValue();
        byte[] valueBytes = value.toByteArray();
        stream.writeInt(valueBytes.length);
        stream.write(valueBytes);
        stream.writeInt(bigDecimal.scale());
        break;
      case JSON:
        stream.writeUTF(DataListUtil.getMapper().writeValueAsString(objectValue));
        break;
      case STRING:
      default:
        stream.writeObject(objectValue);
    }
  }

  private void readObject(@NotNull ObjectInputStream stream) throws IOException, ClassNotFoundException {

    this.printable = stream.readBoolean();
    this.sendStringValue = stream.readBoolean();
    this.stringValue = (String) stream.readObject();
    this.type = (CellDataType) stream.readObject();

    switch (getType()) {
      // Get object value
      case DOUBLE:
        this.objectValue = stream.readDouble();
        break;
      case FLOAT:
        this.objectValue = stream.readFloat();
        break;
      case INTEGER:
        this.objectValue = stream.readInt();
        break;
      case LONG:
        this.objectValue = stream.readLong();
        break;
      case DECIMAL:
        byte[] valueBytes = new byte[stream.readInt()];
        stream.readFully(valueBytes);
        BigInteger value = new BigInteger(valueBytes);
        this.objectValue = new BigDecimal(value, stream.readInt());
        break;
      case JSON:
        this.objectValue = DataListUtil.getMapper().readTree(stream.readUTF());
        break;
      case STRING:
      default:
        this.objectValue = stream.readObject();
    }
  }
}
