package com.almis.awe.service.data.processor;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnValues;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.queries.Compound;
import com.almis.awe.model.entities.queries.Computed;
import com.almis.awe.model.entities.queries.Field;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queries.SqlField;
import com.almis.awe.model.entities.queries.Totalize;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.data.processor.ColumnValuesProcessor.ColumnValueGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Distinct values of a grid column (#912, S9 and S10): grouping of the rows in Java, label query, search and cap.
 */
class ColumnValuesProcessorTest {

  @Test
  @DisplayName("Rows are grouped by value with their count, the empty values form one group and sort last")
  void rowsAreGroupedAndSorted() {
    List<ColumnValueGroup> groups = ColumnValuesProcessor.group(rows("status", "B", null, "A", "B", ""),
      new ColumnValues("status", List.of("status"), ColumnFilterType.VALUES, null));

    assertThat(groups).extracting(ColumnValueGroup::value, ColumnValueGroup::count)
      .containsExactly(tuple("A", 1L), tuple("B", 2L), tuple(null, 2L));
  }

  @Test
  @DisplayName("Numbers are sorted as numbers")
  void numbersAreSortedAsNumbers() {
    List<ColumnValueGroup> groups = ColumnValuesProcessor.group(rows("amount", 10, 9, 100),
      new ColumnValues("amount", List.of("amount"), ColumnFilterType.NUMBER, null));

    assertThat(groups).extracting(ColumnValueGroup::value).containsExactly(9, 10, 100);
  }

  @Test
  @DisplayName("Dates are grouped by day")
  void datesAreGroupedByDay() {
    List<ColumnValueGroup> groups = ColumnValuesProcessor.group(rows("created", Timestamp.valueOf("2026-11-04 10:00:00"),
        Timestamp.valueOf("2026-10-31 09:00:00"), Timestamp.valueOf("2026-11-04 18:30:00")),
      new ColumnValues("created", List.of("created"), ColumnFilterType.DATE, null));

    assertThat(groups).extracting(ColumnValueGroup::value, ColumnValueGroup::count)
      .containsExactly(tuple("31/10/2026", 1L), tuple("04/11/2026", 2L));
  }

  @Test
  @DisplayName("With several fields a row counts once for each distinct value of its fields")
  void severalFieldsCountARowOncePerValue() {
    DataList dataList = new DataList();
    DataListUtil.addColumn(dataList, "owner", List.of("ann", "bob", "ann"));
    DataListUtil.addColumn(dataList, "editor", List.of("ann", "ann", "cid"));

    List<ColumnValueGroup> groups = ColumnValuesProcessor.group(dataList.getRows(),
      new ColumnValues("owner", List.of("owner", "editor"), ColumnFilterType.VALUES, null));

    assertThat(groups).extracting(ColumnValueGroup::value, ColumnValueGroup::count)
      .containsExactly(tuple("ann", 3L), tuple("bob", 1L), tuple("cid", 1L));
  }

  @Test
  @DisplayName("At most 1,000 values are returned, and the response says that more exist")
  void capAndMore() {
    List<ColumnValueGroup> groups = IntStream.range(0, 1001).mapToObj(index -> new ColumnValueGroup(index, 1L, Map.of())).toList();
    List<String> labels = groups.stream().map(group -> String.valueOf(group.value())).toList();

    ServiceData full = ColumnValuesProcessor.getResult(groups.subList(0, 1000), labels.subList(0, 1000), null, false);
    ServiceData capped = ColumnValuesProcessor.getResult(groups, labels, null, false);

    assertThat(full.getDataList().getRows()).hasSize(1000);
    assertThat(full.getVariableMap().get(AweConstants.ACTION_MORE_VALUES).getValue()).isEqualTo(false);
    assertThat(capped.getDataList().getRows()).hasSize(1000);
    assertThat(capped.getDataList().getRecords()).isEqualTo(1000L);
    assertThat(capped.getVariableMap().get(AweConstants.ACTION_MORE_VALUES).getValue()).isEqualTo(true);
  }

  @Test
  @DisplayName("The search keeps the values whose label contains the text, ignoring case")
  void searchOnLabels() {
    List<ColumnValueGroup> groups = List.of(new ColumnValueGroup("A", 2L, Map.of()), new ColumnValueGroup("B", 1L, Map.of()),
      new ColumnValueGroup(null, 1L, Map.of()));

    DataList values = ColumnValuesProcessor.getResult(groups, Arrays.asList("Active", "Blocked", null), "tiV", false).getDataList();

    assertThat(values.getRows()).hasSize(1);
    assertThat(values.getRows().get(0)).containsEntry("value", new CellData("A")).containsEntry("label", new CellData("Active"))
      .containsEntry("count", new CellData(2L));
  }

  @Test
  @DisplayName("The label is the value only for a column without processing that is its filtered field, several fields or dates")
  void labelIsTheValue() {
    Query query = labelsQuery();

    assertThat(ColumnValuesProcessor.isLabelTheValue(query, new ColumnValues("name", List.of("name"), ColumnFilterType.TEXT, null))).isTrue();
    assertThat(ColumnValuesProcessor.isLabelTheValue(query, new ColumnValues("status", List.of("status"), ColumnFilterType.VALUES, null))).isFalse();
    assertThat(ColumnValuesProcessor.isLabelTheValue(query, new ColumnValues("name", List.of("code"), ColumnFilterType.VALUES, null))).isFalse();
    assertThat(ColumnValuesProcessor.isLabelTheValue(query, new ColumnValues("name", List.of("name", "code"), ColumnFilterType.TEXT, null))).isTrue();
    assertThat(ColumnValuesProcessor.isLabelTheValue(query, new ColumnValues("status", List.of("status"), ColumnFilterType.DATE, null))).isTrue();
  }

  @Test
  @DisplayName("The label query keeps only what the column needs, following the computed references, without totalize")
  void labelQueryKeepsTheDependencies() {
    Query labelQuery = ColumnValuesProcessor.getLabelQuery(labelsQuery(), "summary");

    assertThat(labelQuery.getSqlFieldList()).extracting(SqlField::getIdentifier).containsExactly("name", "status");
    assertThat(labelQuery.getComputedList()).extracting(Computed::getIdentifier).containsExactly("statusText");
    assertThat(labelQuery.getCompoundList()).extracting(Compound::getIdentifier).containsExactly("summary");
    assertThat(labelQuery.getTotalizeList()).isNull();
  }

  private static Query labelsQuery() {
    List<SqlField> fields = new ArrayList<>(List.of(Field.builder().id("name").build(), Field.builder().id("code").build(),
      Field.builder().id("status").translate("EsStatus").build(), Field.builder().id("other").build()));
    List<Computed> computed = new ArrayList<>(List.of(Computed.builder().alias("statusText").format("[status]").build(),
      Computed.builder().alias("otherText").format("[other]").build()));
    List<Compound> compounds = new ArrayList<>(List.of(Compound.builder().alias("summary").computedList(new ArrayList<>(List.of(
      Computed.builder().alias("label").format("[name] [statusText] [lang]").build()))).build()));
    return new Query().setId("Labels").setSqlFieldList(fields).setComputedList(computed).setCompoundList(compounds)
      .setTotalizeList(new ArrayList<>(List.of(new Totalize())));
  }

  private static List<Map<String, CellData>> rows(String column, Object... values) {
    List<Map<String, CellData>> rows = new ArrayList<>();
    for (Object value : values) {
      Map<String, CellData> row = new HashMap<>();
      row.put(column, new CellData(value));
      rows.add(row);
    }
    return rows;
  }

  private static org.assertj.core.groups.Tuple tuple(Object... values) {
    return org.assertj.core.groups.Tuple.tuple(values);
  }
}
