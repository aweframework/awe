package com.almis.awe.model.util.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Query Util class
 *
 * @author pgarcia
 */
class QueryUtilTest {

  private QueryUtil queryUtil;

  @BeforeEach
  void setUp() {
    queryUtil = new QueryUtil(null, null, null, null);
  }

  /**
   * Test null get parameters
   */
  @Test
  void testNullGetParameters() {
    assertThrows(NullPointerException.class, () -> queryUtil.getParameters(null, null, null, null));
  }

  /**
   * Test null get parameters
   */
  @Test
  void testNullVariableIsList() {
    assertThrows(NullPointerException.class, () -> queryUtil.variableIsList(null, null));
  }

  /**
   * Test full SQL keeps long strings untouched
   */
  @Test
  void testGetFullSqlDoesNotTruncateLongStringParameters() {
    String longValue = "This is a very long SQL parameter value that should remain complete in logs";

    String fullSql = queryUtil.getFullSQL("select * from table where field = ?", Collections.singletonList(longValue));

    assertEquals("select * from table where field = 'This is a very long SQL parameter value that should remain complete in logs'", fullSql);
  }

  /**
   * Test full SQL escapes apostrophes in strings
   */
  @Test
  void testGetFullSqlEscapesApostrophesInStringParameters() {
    String value = "O'Brien";

    String fullSql = queryUtil.getFullSQL("select * from table where field = ?", Collections.singletonList(value));

    assertEquals("select * from table where field = 'O''Brien'", fullSql);
  }

  /**
   * Test full SQL escapes apostrophes in long string values
   */
  @Test
  void testGetFullSqlEscapesApostrophesInLongStringValues() {
    String value = "long value with 'quotes' and more than twenty five chars";

    String fullSql = queryUtil.getFullSQL("select * from table where field = ?", Collections.singletonList(value));

    assertEquals("select * from table where field = 'long value with ''quotes'' and more than twenty five chars'", fullSql);
  }

  /**
   * Test full SQL renders null bindings as SQL null
   */
  @Test
  void testGetFullSqlRendersNullBindingsAsSqlNull() {
    String fullSql = queryUtil.getFullSQL("select * from table where field = ?", Collections.singletonList(null));

    assertEquals("select * from table where field = null", fullSql);
  }

  /**
   * Test full SQL keeps parameter order with mixed null and non-null bindings
   */
  @Test
  void testGetFullSqlHandlesMixedNullAndNonNullBindings() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where first = ? and second = ? and third = ?",
        java.util.Arrays.asList(null, "O'Brien", 7));

    assertEquals("select * from table where first = null and second = 'O''Brien' and third = 7", fullSql);
  }

  /**
   * Test full SQL renders boolean bindings as SQL boolean literals
   */
  @Test
  void testGetFullSqlRendersBooleanBindingsAsSqlBooleanLiterals() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where enabled = ? and deleted = ?",
        java.util.Arrays.asList(Boolean.TRUE, Boolean.FALSE));

    assertEquals("select * from table where enabled = TRUE and deleted = FALSE", fullSql);
  }

  /**
   * Test full SQL renders LocalDate bindings as SQL date literals
   */
  @Test
  void testGetFullSqlRendersLocalDateBindingsAsSqlDateLiterals() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where created_on = ?",
        Collections.singletonList(LocalDate.of(2026, 4, 24)));

    assertEquals("select * from table where created_on = (date '2026-04-24')", fullSql);
  }

  /**
   * Test full SQL renders LocalDateTime bindings as SQL timestamp literals
   */
  @Test
  void testGetFullSqlRendersLocalDateTimeBindingsAsSqlTimestampLiterals() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where created_at = ?",
        Collections.singletonList(LocalDateTime.of(2026, 4, 24, 10, 30, 45, 123000000)));

    assertEquals("select * from table where created_at = (timestamp '2026-04-24 10:30:45.123')", fullSql);
  }

  /**
   * Test full SQL renders collection bindings for IN clauses
   */
  @Test
  void testGetFullSqlRendersCollectionBindingsForInClauses() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where id in (?)",
        Collections.singletonList(Arrays.asList(1, 2, 3)));

    assertEquals("select * from table where id in (1, 2, 3)", fullSql);
  }

  /**
   * Test full SQL renders empty collection bindings explicitly for IN clauses
   */
  @Test
  void testGetFullSqlRendersEmptyCollectionBindingsExplicitlyForInClauses() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where id in (?)",
        Collections.singletonList(Collections.emptyList()));

    assertEquals("select * from table where id in (/* empty */)", fullSql);
  }

  /**
   * Test full SQL renders array bindings recursively for IN clauses
   */
  @Test
  void testGetFullSqlRendersArrayBindingsRecursivelyForInClauses() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where value in (?)",
        Collections.singletonList(new Object[]{"O'Brien", LocalDate.of(2026, 4, 24), null, Boolean.TRUE}));

    assertEquals("select * from table where value in ('O''Brien', (date '2026-04-24'), null, TRUE)", fullSql);
  }

  /**
   * Test full SQL renders empty array bindings explicitly for IN clauses
   */
  @Test
  void testGetFullSqlRendersEmptyArrayBindingsExplicitlyForInClauses() {
    String fullSql = queryUtil.getFullSQL(
        "select * from table where value in (?)",
        Collections.singletonList(new Object[]{}));

    assertEquals("select * from table where value in (/* empty */)", fullSql);
  }
}
