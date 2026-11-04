package com.almis.awe.model.util.data;

import com.almis.awe.exception.AWEQueryException;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.entities.queries.Computed;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.type.ColumnFilterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static com.almis.awe.model.util.data.ColumnFilterTestFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Request parameter {@code columnValues} of the column values action (#912, S8): parsed into a closed contract and
 * checked against the printed fields of the query before any data is read.
 */
class QueryUtilColumnValuesTest {

  private QueryUtil queryUtil;
  private Query query;

  @BeforeEach
  void setUp() {
    queryUtil = ColumnFilterTestFixtures.queryUtil();
    List<SqlField> fields = new ArrayList<>(List.of(Field.builder().id("EvtTyp").build(), Field.builder().id("Pwd").noprint(true).build()));
    List<Computed> computed = new ArrayList<>(List.of(Computed.builder().alias("TypTxt").format("[EvtTyp]").build()));
    query = new Query().setId("EvtLst").setSqlFieldList(fields).setComputedList(computed);
  }

  @Test
  @DisplayName("Column values are parsed with their fields, type and search; fields default to the column")
  void columnValuesAreParsed() throws Exception {
    ColumnValues typed = queryUtil.getColumnValues(json("{'id':'TypTxt','fields':['EvtTyp'],'type':'values','search':'Ab'}"));
    ColumnValues plain = queryUtil.getColumnValues(json("{'id':'EvtTyp','type':'text','search':' '}"));

    assertThat(typed).isEqualTo(new ColumnValues("TypTxt", List.of("EvtTyp"), ColumnFilterType.VALUES, "Ab"));
    assertThat(plain).isEqualTo(new ColumnValues("EvtTyp", List.of("EvtTyp"), ColumnFilterType.TEXT, null));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "null|columnValues must be an object",
    "[]|columnValues must be an object",
    "{'type':'values'}|columnValues needs an id",
    "{'id':'EvtTyp','type':'list'}|unknown type in column values EvtTyp",
    "{'id':'EvtTyp','type':'values','fields':[]}|fields must be a list of field ids",
    "{'id':'EvtTyp','type':'values','search':5}|search must be a text in column values EvtTyp"
  })
  @DisplayName("Malformed column values are rejected")
  void malformedColumnValuesAreRejected(String columnValues, String reason) {
    assertThatThrownBy(() -> queryUtil.getColumnValues(json(columnValues))).isInstanceOf(AWEQueryException.class)
      .hasMessageContaining(reason);
  }

  @Test
  @DisplayName("The listed fields follow the column filter rules and the column must be a printed field of the query")
  void fieldsAndColumnAreChecked() throws Exception {
    assertThat(queryUtil.getColumnValuesFields(query, new ColumnValues("TypTxt", List.of("EvtTyp"), ColumnFilterType.VALUES, null)))
      .extracting(SqlField::getIdentifier).containsExactly("EvtTyp");
    assertThatThrownBy(() -> queryUtil.getColumnValuesFields(query, new ColumnValues("TypTxt", List.of("Pwd"), ColumnFilterType.VALUES, null)))
      .isInstanceOf(AWEQueryException.class).hasMessageContaining("field Pwd of column filter TypTxt is not printed");
    assertThatThrownBy(() -> queryUtil.getColumnValuesFields(query, new ColumnValues("Pwd", List.of("EvtTyp"), ColumnFilterType.VALUES, null)))
      .isInstanceOf(AWEQueryException.class).hasMessageContaining("column Pwd of column values is not printed");
    assertThatThrownBy(() -> queryUtil.getColumnValuesFields(query, new ColumnValues("Other", List.of("EvtTyp"), ColumnFilterType.VALUES, null)))
      .isInstanceOf(AWEQueryException.class).hasMessageContaining("unknown column Other in column values");
  }
}
