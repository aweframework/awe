package com.almis.awe.service.data.builder;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnFilter;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.SortColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Grid column filters applied in Java on the rows of a non-SQL query (#912, S6 and S7), with the same operators as
 * the SQL ones: text ignores case, between is inclusive, empty is a missing value.
 */
class DataListBuilderColumnFilterTest {

  private static final JsonMapper MAPPER = new JsonMapper();

  private static List<ColumnFilter> filters(String json) {
    return ColumnFilter.fromJson(MAPPER.readTree(json.replace('\'', '"')));
  }

  private static DataListBuilder users() {
    return new DataListBuilder()
      .addColumn("name", Arrays.asList("50% off", "500 OFF", "Ann", "", "anna"), "STRING")
      .addColumn("email", Arrays.asList("x@a.com", "y@b.com", "ann@c.com", "z@d.com", "q@e.com"), "STRING")
      .addColumn("amount", Arrays.asList(5, 10, 15, 20, 25), "INTEGER")
      .addColumn("status", Arrays.asList("A", "B", "C", "", "A"), "STRING")
      .addColumn("created", Arrays.asList(Timestamp.valueOf("2026-10-31 23:59:59"), Timestamp.valueOf("2026-11-01 00:00:00"),
        Timestamp.valueOf("2026-11-03 23:00:00"), Timestamp.valueOf("2026-11-04 00:00:00"), Timestamp.valueOf("2026-11-02 12:00:00")), "DATE");
  }

  private static List<String> names(DataList dataList) {
    return dataList.getRows().stream().map(row -> row.get("name")).map(CellData::getStringValue).toList();
  }

  @Test
  @DisplayName("contains ignores case and matches % literally")
  void containsIgnoresCaseAndMatchesPercentLiterally() throws Exception {
    DataList output = users().columnFilter(filters("[{'id':'name','type':'text','conditions':[{'operator':'contains','value':'50% OFF'}]}]")).build();

    assertThat(names(output)).containsExactly("50% off");
  }

  @Test
  @DisplayName("values keep the listed values, plus the empty ones when empty is true")
  void valuesWithEmpty() throws Exception {
    DataList output = users().columnFilter(filters("[{'id':'status','type':'values','values':['A'],'empty':true}]")).build();

    assertThat(names(output)).containsExactly("50% off", "", "anna");
  }

  @Test
  @DisplayName("An empty list of values keeps no row, as the SQL filter")
  void emptyValuesListKeepsNoRow() throws Exception {
    assertThat(names(users().columnFilter(filters("[{'id':'status','type':'values','values':[]}]")).build())).isEmpty();
  }

  @Test
  @DisplayName("between dates is inclusive on whole days")
  void betweenDates() throws Exception {
    DataList output = users().columnFilter(filters("[{'id':'created','type':'date','conditions':"
      + "[{'operator':'between','value':'01/11/2026','to':'03/11/2026'}]}]")).build();

    assertThat(names(output)).containsExactly("500 OFF", "Ann", "anna");
  }

  @Test
  @DisplayName("Numbers are compared as numbers, two conditions joined by or")
  void numberConditionsWithOr() throws Exception {
    DataList output = users().columnFilter(filters("[{'id':'amount','type':'number','union':'or','conditions':"
      + "[{'operator':'lt','value':'6'},{'operator':'gt','value':'19.5'}]}]")).build();

    assertThat(names(output)).containsExactly("50% off", "", "anna");
  }

  @Test
  @DisplayName("empty keeps the rows without value, and two column filters are joined by and")
  void emptyAndSeveralColumns() throws Exception {
    DataList output = users().columnFilter(filters("[{'id':'name','type':'text','conditions':[{'operator':'notEmpty'}]},"
      + "{'id':'status','type':'values','values':['A']}]")).build();

    assertThat(names(output)).containsExactly("50% off", "anna");
  }

  @Test
  @DisplayName("A filter on several fields keeps the rows where any field matches")
  void severalFieldsJoinedByOr() throws Exception {
    DataList output = users().columnFilter(filters("[{'id':'name','fields':['name','email'],'type':'text','conditions':"
      + "[{'operator':'startsWith','value':'Y'}]}]")).build();

    assertThat(names(output)).containsExactly("500 OFF");
  }

  @Test
  @DisplayName("The filtered rows can be sorted: a column filter followed by a sort keeps the matching rows in order")
  void filteredRowsAreSorted() throws Exception {
    DataList output = users()
      .columnFilter(filters("[{'id':'name','type':'text','conditions':[{'operator':'contains','value':'an'}]}]"))
      .sort(List.of(new SortColumn("amount", "desc"))).build();

    assertThat(names(output)).containsExactly("anna", "Ann");
  }

  @Test
  @DisplayName("The filter runs before sort and pagination: records and pages count the matching rows")
  void filterBeforePagination() throws Exception {
    DataList output = users().setRecords(5L).setMax(1L).setPage(2L).paginate(true)
      .columnFilter(filters("[{'id':'name','type':'text','conditions':[{'operator':'contains','value':'an'}]}]")).build();

    assertThat(names(output)).containsExactly("anna");
    assertThat(output.getRecords()).isEqualTo(2L);
    assertThat(output.getTotal()).isEqualTo(2L);
  }

  @Test
  @DisplayName("Without column filters the rows are unchanged")
  void noColumnFilters() throws Exception {
    assertThat(names(users().columnFilter(null).build())).containsExactly("50% off", "500 OFF", "Ann", "", "anna");
    assertThat(names(users().columnFilter(filters("[]")).build())).containsExactly("50% off", "500 OFF", "Ann", "", "anna");
  }
}
