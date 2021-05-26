package com.almis.awe.model.dto;

import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.util.data.DateUtil;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CellData DTO tests
 *
 * @author pgarcia
 */
class CellDataTest {

  // Prepare
  Double doubleValue = 23.0D;
  Integer integerValue = 23;
  Long longValue = 23L;
  Float floatValue = 23.0F;
  BigDecimal decimal = new BigDecimal(23);
  BigDecimal bigDecimal = new BigDecimal(234234223);
  Date dateValue = new GregorianCalendar(1978, Calendar.OCTOBER, 23).getTime();
  Date bigDateValue = new GregorianCalendar(2022, Calendar.JANUARY, 11).getTime();
  Boolean booleanValue = true;
  Boolean nullBoolean = null;
  Integer nullInteger = null;
  String textValue = "tutu";
  String anotherTextValue = "lala";
  ArrayNode jsonValue = JsonNodeFactory.instance.arrayNode()
    .add(longValue)
    .add(integerValue)
    .add(textValue);
  ObjectNode anotherJsonValue = JsonNodeFactory.instance.objectNode()
    .put("long", longValue)
    .put("integer", integerValue)
    .put("text", textValue)
    .putPOJO("pojo", new CellData(longValue));

  /**
   * Test of cell data null
   *
   * @throws Exception Test error
   */
  @Test
  void testCellDataNull() throws Exception {
    // Run
    CellData decimalData = new CellData(decimal);
    CellData nullData = new CellData();
    CellData anotherNullData = nullData.copy();
    CellData nullDataBoolean = new CellData(booleanValue);
    nullDataBoolean.setValue(nullBoolean);
    CellData nullDataInteger = new CellData(integerValue);
    nullDataInteger.setValue(nullInteger);

    // Assert
    assertEquals(nullData, anotherNullData);
    assertNull(nullData.getValue());
    assertNull(nullData.getObjectValue());
    assertNull(nullData.getDoubleValue());
    assertNull(nullData.getIntegerValue());
    assertEquals(nullData, SerializationUtils.deserialize(SerializationUtils.serialize(nullData)));
    assertEquals(0, nullData.compareTo(anotherNullData));
    assertEquals(0, nullDataInteger.compareTo(nullDataBoolean));
    assertTrue(nullDataInteger.compareTo(decimalData) < 0);

    assertTrue(nullData.isEmpty());
    assertTrue(nullDataInteger.isEmpty());
  }

  /**
   * Test of cell data
   */
  @Test
  void testCellDataBigDecimal() {
    // Run
    CellData decimalData = new CellData(decimal);
    CellData bigDecimalData = new CellData(bigDecimal);

    // Assert
    assertNotEquals(decimalData, bigDecimalData);
    assertEquals(decimal, decimalData.getValue());
    assertEquals(decimal, decimalData.getObjectValue());
    assertEquals(doubleValue, decimalData.getDoubleValue(), 0.00001);
    assertEquals(integerValue, decimalData.getIntegerValue());
    assertEquals(decimalData, SerializationUtils.deserialize(SerializationUtils.serialize(decimalData)));
    assertTrue(decimalData.compareTo(bigDecimalData) < 0);
    assertFalse(decimalData.isEmpty());
  }

  /**
   * Test of cell data float
   */
  @Test
  void testCellDataFloat() {
    // Run
    CellData floatData = new CellData(floatValue);
    CellData anotherFloatData = new CellData(decimal.floatValue());
    CellData nullData = new CellData();

    // Assert
    assertEquals(floatData, anotherFloatData);
    assertEquals(floatValue, floatData.getValue());
    assertEquals(floatValue, floatData.getObjectValue());
    assertEquals(doubleValue, floatData.getDoubleValue(), 0.00001);
    assertEquals(integerValue, floatData.getIntegerValue());
    assertEquals(floatData, SerializationUtils.deserialize(SerializationUtils.serialize(floatData)));
    assertEquals(0, floatData.compareTo(anotherFloatData));
    assertTrue(floatData.compareTo(nullData) > 0);
    assertFalse(floatData.isEmpty());
  }

  /**
   * Test of cell data long
   */
  @Test
  void testCellDataLong() {
    // Run
    CellData longData = new CellData(longValue);
    CellData bigLongData = new CellData(bigDecimal.longValue());

    // Assert
    assertNotEquals(longData, bigLongData);
    assertEquals(longValue, longData.getValue());
    assertEquals(longValue, longData.getObjectValue());
    assertEquals(doubleValue, longData.getDoubleValue(), 0.00001);
    assertEquals(integerValue, longData.getIntegerValue());
    assertEquals(longData, SerializationUtils.deserialize(SerializationUtils.serialize(longData)));
    assertTrue(bigLongData.compareTo(longData) > 0);
    assertFalse(bigLongData.isEmpty());
  }

  /**
   * Test of cell data double
   */
  @Test
  void testCellDataDouble() {
    // Run
    CellData doubleData = new CellData(doubleValue);
    CellData bigDoubleData = new CellData(bigDecimal.doubleValue());

    // Assert
    assertNotEquals(doubleData, bigDoubleData);
    assertEquals(doubleValue, doubleData.getValue());
    assertEquals(doubleValue, doubleData.getObjectValue());
    assertEquals(doubleValue, doubleData.getDoubleValue(), 0.00001);
    assertEquals(integerValue, doubleData.getIntegerValue());
    assertEquals(doubleData, SerializationUtils.deserialize(SerializationUtils.serialize(doubleData)));
    assertTrue(doubleData.compareTo(bigDoubleData) < 0);
    assertFalse(doubleData.isEmpty());
  }

  /**
   * Test of cell data integer
   */
  @Test
  void testCellDataInteger() {
    // Run
    CellData integerData = new CellData(integerValue);
    CellData bigIntegerData = new CellData(bigDecimal.intValue());
    CellData nullData = new CellData();

    // Assert
    assertNotEquals(integerData, bigIntegerData);
    assertEquals(integerValue, integerData.getValue());
    assertEquals(integerValue, integerData.getObjectValue());
    assertEquals(doubleValue, integerData.getDoubleValue(), 0.00001);
    assertEquals(integerValue, integerData.getIntegerValue());
    assertEquals(integerData, SerializationUtils.deserialize(SerializationUtils.serialize(integerData)));
    assertTrue(bigIntegerData.compareTo(integerData) > 0);
    assertTrue(nullData.compareTo(integerData) < 0);
    assertFalse(bigIntegerData.isEmpty());
    assertFalse(bigIntegerData.compareTo(new CellData(textValue)) > 0);
  }

  /**
   * Test of cell data string
   */
  @Test
  void testCellDataString() {
    // Run
    CellData textData = new CellData(textValue);
    CellData anotherTextData = new CellData(anotherTextValue);

    // Assert
    assertNotEquals(textData, anotherTextData);
    assertEquals(textValue, textData.getValue());
    assertEquals(textValue, textData.getObjectValue());
    assertEquals(textValue, textData.getStringValue());
    assertNull(textData.getDoubleValue());
    assertNull(textData.getIntegerValue());
    assertEquals(textData, SerializationUtils.deserialize(SerializationUtils.serialize(textData)));
    assertTrue(textData.compareTo(anotherTextData) > 0);
    assertFalse(textData.isEmpty());
  }

  /**
   * Test of cell data string
   */
  @Test
  void testCellDataJson() {
    // Run
    CellData jsonData = new CellData(jsonValue);
    CellData anotherJsonData = new CellData(anotherJsonValue);

    // Assert
    assertNotEquals(jsonData, anotherJsonData);
    assertEquals(jsonValue, jsonData.getValue());
    assertEquals(jsonValue, jsonData.getObjectValue());
    assertEquals(jsonValue.toString(), jsonData.getStringValue());
    assertNull(jsonData.getDoubleValue());
    assertSame(0, jsonData.getIntegerValue());
    assertEquals(0, jsonData.compareTo((CellData) SerializationUtils.deserialize(SerializationUtils.serialize(jsonData))));
    assertEquals(0, anotherJsonData.compareTo((CellData) SerializationUtils.deserialize(SerializationUtils.serialize(anotherJsonData))));
    assertTrue(jsonData.compareTo(anotherJsonData) < 0);
    assertFalse(jsonData.isEmpty());
  }

  /**
   * Test of cell data date
   */
  @Test
  void testCellDataDate() {
    // Run
    CellData dateData = new CellData(dateValue);
    CellData bigDateData = new CellData(bigDateValue);
    CellData dateTextData = new CellData(DateUtil.jsonDate(dateValue));
    CellData anotherDateTextData = new CellData(DateUtil.dat2WebTimestamp(dateValue));

    // Assert
    assertEquals(dateData, dateTextData);
    assertEquals(DateUtil.jsonDate(dateValue), dateData.getValue());
    assertEquals(dateValue, dateData.getObjectValue());
    assertEquals(DateUtil.dat2WebTimestamp(dateValue), dateData.getStringValue());
    assertEquals(dateValue, dateData.getDateValue());
    assertNull(dateData.getDoubleValue());
    assertNull(dateData.getIntegerValue());
    assertEquals(dateData, SerializationUtils.deserialize(SerializationUtils.serialize(dateData)));
    assertEquals(dateTextData, SerializationUtils.deserialize(SerializationUtils.serialize(dateTextData)));

    assertEquals(DateUtil.jsonDate(dateValue), dateTextData.getValue());
    assertEquals(dateValue, dateTextData.getObjectValue());
    assertEquals(DateUtil.dat2WebTimestamp(dateValue), dateTextData.getStringValue());
    assertEquals(dateValue, dateTextData.getDateValue());
    assertEquals(dateValue, anotherDateTextData.getDateValue());
    assertNull(dateTextData.getDoubleValue());
    assertNull(dateTextData.getIntegerValue());

    assertTrue(dateData.compareTo(bigDateData) < 0);
    assertFalse(dateData.isEmpty());
  }

  /**
   * Test of cell data date
   */
  @Test
  void testCellDataCellData() {
    // Run
    CellData dateData = new CellData(dateValue);
    CellData dateDataCopy = new CellData(dateData);

    // Assert
    assertEquals(dateData, dateDataCopy);
    assertEquals(DateUtil.jsonDate(dateValue), dateDataCopy.getValue());
    assertEquals(dateValue, dateDataCopy.getObjectValue());
    assertEquals(DateUtil.dat2WebTimestamp(dateValue), dateDataCopy.getStringValue());
    assertEquals(dateValue, dateDataCopy.getDateValue());
    assertNull(dateDataCopy.getDoubleValue());
    assertNull(dateDataCopy.getIntegerValue());
    assertEquals(dateDataCopy, SerializationUtils.deserialize(SerializationUtils.serialize(dateDataCopy)));
    assertEquals(0, dateData.compareTo(dateDataCopy));
  }

  /**
   * Test of cell data integer
   */
  @Test
  void testCellDataIntegerTransform() {
    // Run
    CellData integerData = new CellData(integerValue);
    integerData.setSendStringValue(true);
    integerData.setStringValue(integerValue.toString());

    // Assert
    assertEquals(integerValue.toString(), integerData.getValue());
    assertEquals(integerValue, integerData.getObjectValue());
    assertEquals(doubleValue, integerData.getDoubleValue(), 0.00001);
    assertEquals(integerValue, integerData.getIntegerValue());
    assertEquals(integerData, SerializationUtils.deserialize(SerializationUtils.serialize(integerData)));
    assertFalse(integerData.isEmpty());
  }

  /**
   * Test of cell data double
   *
   * @throws Exception Test error
   */
  @Test
  void testCellDataObject() throws Exception {
    // Run
    Column column = Column.builder()
            .name("campo1")
            .charLength(20)
            .build();
    CellData columnData = new CellData(column);
    CellData anotherColumnData = columnData.copy();

    // Assert
    assertEquals(columnData, anotherColumnData);
    assertSame(column, columnData.getValue());
    assertSame(column, columnData.getObjectValue());
    assertNull(columnData.getDoubleValue());
    assertNull(columnData.getIntegerValue());
    assertEquals(0, columnData.compareTo((CellData) SerializationUtils.deserialize(SerializationUtils.serialize(columnData))));
    assertEquals(0, columnData.compareTo(anotherColumnData));
    assertFalse(columnData.isEmpty());
  }

}