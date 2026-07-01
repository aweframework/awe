package com.almis.awe.model.util.data;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataListUtilTest {

  @Test
  void shouldKeepUniqueRowsInFirstSeenOrder() {
    DataList first = dataListOf(
      row("id", "B", "name", "Beta"),
      row("id", "A", "name", "Alpha")
    );
    DataList second = dataListOf(
      row("id", "C", "name", "Gamma")
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), first, second);

    assertAll(
      () -> assertEquals(3, merged.getRows().size()),
      () -> assertEquals("B", merged.getRows().get(0).get("id").getStringValue()),
      () -> assertEquals("A", merged.getRows().get(1).get("id").getStringValue()),
      () -> assertEquals("C", merged.getRows().get(2).get("id").getStringValue())
    );
  }

  @Test
  void shouldUnionColumnsForDuplicateKeys() {
    DataList first = dataListOf(
      row("id", "USR-1", "name", "Alice")
    );
    DataList second = dataListOf(
      row("id", "USR-1", "department", "Sales", "country", "ES")
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), first, second);

    assertAll(
      () -> assertEquals(1, merged.getRows().size()),
      () -> assertEquals("Alice", merged.getRows().get(0).get("name").getStringValue()),
      () -> assertEquals("Sales", merged.getRows().get(0).get("department").getStringValue()),
      () -> assertEquals("ES", merged.getRows().get(0).get("country").getStringValue())
    );
  }

  @Test
  void shouldAvoidCompositeKeyCollisionsWhenPartsConcatenateTheSameWay() {
    DataList first = dataListOf(
      row("left", "a", "right", "bc", "value", "first")
    );
    DataList second = dataListOf(
      row("left", "ab", "right", "c", "value", "second")
    );

    DataList merged = DataListUtil.mergeByKey(List.of("left", "right"), first, second);

    assertAll(
      () -> assertEquals(2, merged.getRows().size()),
      () -> assertEquals("first", merged.getRows().get(0).get("value").getStringValue()),
      () -> assertEquals("second", merged.getRows().get(1).get("value").getStringValue())
    );
  }

  @Test
  void shouldFillMissingValueFromLaterSourceWithoutChangingRowPosition() {
    DataList first = dataListOf(
      row("id", "USR-1", "name", "Alice", "email", null)
    );
    DataList second = dataListOf(
      row("id", "USR-1", "email", "alice@example.com")
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), first, second);

    assertAll(
      () -> assertEquals(1, merged.getRows().size()),
      () -> assertEquals("USR-1", merged.getRows().get(0).get("id").getStringValue()),
      () -> assertEquals("alice@example.com", merged.getRows().get(0).get("email").getStringValue())
    );
  }

  @Test
  void shouldNotOverwriteExistingNonEmptyValueWithLaterConflict() {
    DataList first = dataListOf(
      row("id", "USR-1", "email", "first@example.com")
    );
    DataList second = dataListOf(
      row("id", "USR-1", "email", "second@example.com")
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), first, second);

    assertEquals("first@example.com", merged.getRows().get(0).get("email").getStringValue());
  }

  @Test
  void shouldFailWhenRowIsMissingADeclaredKeyColumn() {
    DataList source = dataListOf(
      row("tenant", "AWE", "name", "Alice")
    );

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
      () -> DataListUtil.mergeByKey(List.of("tenant", "id"), source));

    assertTrue(error.getMessage().contains("id"));
  }

  @Test
  void shouldFailWhenDeclaredKeyValueIsNullOrEmpty() {
    DataList nullKey = dataListOf(
      row("id", null, "name", "Null Key")
    );
    DataList blankKey = dataListOf(
      row("id", "   ", "name", "Blank Key")
    );

    IllegalArgumentException nullError = assertThrows(IllegalArgumentException.class,
      () -> DataListUtil.mergeByKey(List.of("id"), nullKey));
    IllegalArgumentException blankError = assertThrows(IllegalArgumentException.class,
      () -> DataListUtil.mergeByKey(List.of("id"), blankKey));

    assertAll(
      () -> assertTrue(nullError.getMessage().contains("id")),
      () -> assertTrue(blankError.getMessage().contains("id"))
    );
  }

  @Test
  void shouldReturnEmptyDataListForNullOrEmptySources() {
    DataList mergedWithoutSources = DataListUtil.mergeByKey(List.of("id"));
    DataList mergedWithNullSources = DataListUtil.mergeByKey(List.of("id"), (DataList[]) null);
    DataList mergedWithEmptySources = DataListUtil.mergeByKey(List.of("id"), new DataList(), null);

    assertAll(
      () -> assertEquals(0L, mergedWithoutSources.getRecords()),
      () -> assertEquals(0, mergedWithoutSources.getRows().size()),
      () -> assertEquals(0L, mergedWithNullSources.getRecords()),
      () -> assertEquals(0, mergedWithNullSources.getRows().size()),
      () -> assertEquals(0L, mergedWithEmptySources.getRecords()),
      () -> assertEquals(0, mergedWithEmptySources.getRows().size())
    );
  }

  @Test
  void shouldRejectNullOrEmptyKeyColumns() {
    DataList source = dataListOf(row("id", "A"));

    assertAll(
      () -> assertThrows(IllegalArgumentException.class, () -> DataListUtil.mergeByKey(null, source)),
      () -> assertThrows(IllegalArgumentException.class, () -> DataListUtil.mergeByKey(List.of(), source))
    );
  }

  @Test
  void shouldRecomputeMetadataFromUniqueRowsAndResetToSinglePage() {
    DataList first = dataListOf(
      row("id", "USR-1", "name", "Alice"),
      row("id", "USR-2", "name", "Bob")
    );
    first.setPage(3);
    first.setTotal(9);
    first.setRecords(42);

    DataList second = dataListOf(
      row("id", "USR-1", "department", "Sales")
    );
    second.setPage(5);
    second.setTotal(11);
    second.setRecords(99);

    DataList merged = DataListUtil.mergeByKey(List.of("id"), first, second);

    assertAll(
      () -> assertEquals(2L, merged.getRecords()),
      () -> assertEquals(1L, merged.getPage()),
      () -> assertEquals(1L, merged.getTotal())
    );
  }

  @Test
  void shouldKeepSourceDataListsUnchangedWhenMergedResultMutates() {
    DataList first = dataListOf(
      row("id", "USR-1", "name", "Alice", "email", null)
    );
    DataList second = dataListOf(
      row("id", "USR-1", "email", "alice@example.com", "department", "Sales")
    );
    DataList firstSnapshot = snapshot(first);
    DataList secondSnapshot = snapshot(second);

    DataList merged = DataListUtil.mergeByKey(List.of("id"), first, second);
    merged.getRows().get(0).get("name").setValue("Changed");
    merged.getRows().get(0).put("country", new CellData("ES"));
    merged.setRecords(99);
    merged.setPage(7);
    merged.setTotal(8);

    assertAll(
      () -> assertEquals(firstSnapshot, first),
      () -> assertEquals(secondSnapshot, second)
    );
  }

  @Test
  void shouldIsolateNestedObjectPayloadsWhenMergedResultMutates() {
    Map<String, Object> profile = new HashMap<>();
    Map<String, Object> address = new HashMap<>();
    address.put("city", "Madrid");
    profile.put("address", address);

    DataList source = dataListOf(
      row("id", "USR-1", "profile", profile)
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), source);
    Map<String, Object> mergedProfile = (Map<String, Object>) merged.getRows().get(0).get("profile").getObjectValue();
    Map<String, Object> mergedAddress = (Map<String, Object>) mergedProfile.get("address");
    mergedAddress.put("city", "Barcelona");

    Map<String, Object> sourceProfile = (Map<String, Object>) source.getRows().get(0).get("profile").getObjectValue();
    Map<String, Object> sourceAddress = (Map<String, Object>) sourceProfile.get("address");
    assertAll(
      () -> assertEquals("Madrid", sourceAddress.get("city")),
      () -> assertNotSame(sourceAddress, mergedAddress)
    );
  }

  @Test
  void shouldIsolateNestedJsonPayloadsWhenMergedResultMutates() {
    ObjectNode sourcePayload = JsonNodeFactory.instance.objectNode();
    sourcePayload.putObject("address").put("city", "Madrid");
    DataList source = dataListOf(
      row("id", "USR-1", "payload", sourcePayload)
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), source);
    ObjectNode mergedPayload = (ObjectNode) merged.getRows().get(0).get("payload").getObjectValue();
    ((ObjectNode) mergedPayload.get("address")).put("city", "Barcelona");

    ObjectNode originalPayload = (ObjectNode) source.getRows().get(0).get("payload").getObjectValue();
    assertAll(
      () -> assertEquals("Madrid", originalPayload.get("address").get("city").asText()),
      () -> assertNotSame(originalPayload.get("address"), mergedPayload.get("address"))
    );
  }

  @Test
  void shouldIsolateListPayloadsWhenMergedResultMutates() {
    List<String> roles = new ArrayList<>(List.of("admin", "user"));
    DataList source = dataListOf(
      row("id", "USR-1", "roles", roles)
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), source);
    List<String> mergedRoles = (List<String>) merged.getRows().get(0).get("roles").getObjectValue();
    mergedRoles.add("auditor");

    List<String> sourceRoles = (List<String>) source.getRows().get(0).get("roles").getObjectValue();
    assertAll(
      () -> assertEquals(List.of("admin", "user"), sourceRoles),
      () -> assertNotSame(sourceRoles, mergedRoles)
    );
  }

  @Test
  void shouldIsolateDatePayloadsWhenMergedResultMutates() {
    Date createdAt = new Date(1_700_000_000_000L);
    DataList source = dataListOf(
      row("id", "USR-1", "createdAt", createdAt)
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), source);
    Date mergedCreatedAt = merged.getRows().get(0).get("createdAt").getDateValue();
    mergedCreatedAt.setTime(1_800_000_000_000L);

    Date sourceCreatedAt = source.getRows().get(0).get("createdAt").getDateValue();
    assertAll(
      () -> assertEquals(1_700_000_000_000L, sourceCreatedAt.getTime()),
      () -> assertNotSame(sourceCreatedAt, mergedCreatedAt)
    );
  }

  @Test
  void shouldPreserveGenericObjectPayloadsWithoutNormalizing() {
    MutablePayload payload = new MutablePayload("admin");
    DataList source = dataListOf(
      row("id", "USR-1", "payload", payload)
    );

    DataList merged = DataListUtil.mergeByKey(List.of("id"), source);
    Object mergedPayload = merged.getRows().get(0).get("payload").getObjectValue();

    assertAll(
      () -> assertInstanceOf(MutablePayload.class, mergedPayload),
      () -> assertSame(payload, mergedPayload)
    );
  }

  @SafeVarargs
  private final DataList dataListOf(Map<String, CellData>... rows) {
    DataList dataList = new DataList();
    for (Map<String, CellData> row : rows) {
      dataList.addRow(new HashMap<>(row));
    }
    return dataList;
  }

  private Map<String, CellData> row(Object... columns) {
    Map<String, CellData> row = new HashMap<>();
    for (int index = 0; index < columns.length; index += 2) {
      row.put((String) columns[index], new CellData().setValue(columns[index + 1]));
    }
    return row;
  }

  private DataList snapshot(DataList dataList) {
    return new DataList(dataList);
  }

  private static class MutablePayload {
    public List<String> roles = new ArrayList<>();

    MutablePayload(String role) {
      roles.add(role);
    }
  }
}
