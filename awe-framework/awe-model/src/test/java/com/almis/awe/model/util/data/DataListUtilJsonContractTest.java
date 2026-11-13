package com.almis.awe.model.util.data;

import com.almis.awe.model.type.MaintainType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Guards the JSON contract of AWE 5: the AWE mapper keeps the wire format of AWE 4 (Jackson 2) although Jackson 3
 * changed several defaults, and rejects trailing content after the root value.
 */
@DisplayName("AWE JSON contract on Jackson 3")
class DataListUtilJsonContractTest {

  record Ordered(String zeta, String alpha) {
  }

  /** A plain bean: unlike record components, its properties are subject to MapperFeature.SORT_PROPERTIES_ALPHABETICALLY. */
  public static class OrderedBean {
    public String zeta = "z";
    public String alpha = "a";
  }

  record Counter(int count) {
  }

  @Test
  @DisplayName("Enums are written by name, not by their toString()")
  void enumsAreWrittenByName() {
    assertEquals("\"INSERT\"", DataListUtil.getMapper().writeValueAsString(MaintainType.INSERT));
  }

  @Test
  @DisplayName("Enums are read by name, not by their toString()")
  void enumsAreReadByName() {
    assertEquals(MaintainType.INSERT, DataListUtil.getMapper().readValue("\"INSERT\"", MaintainType.class));
  }

  @Test
  @DisplayName("Properties keep their declaration order instead of being sorted alphabetically")
  void propertiesKeepDeclarationOrder() {
    assertEquals("{\"zeta\":\"z\",\"alpha\":\"a\"}", DataListUtil.getMapper().writeValueAsString(new OrderedBean()));
  }

  @Test
  @DisplayName("A null value for a primitive is read as its default value")
  void nullForPrimitiveIsAccepted() {
    assertEquals(0, DataListUtil.getMapper().readValue("{\"count\":null}", Counter.class).count());
  }

  @Test
  @DisplayName("Trailing content after the root value is rejected")
  void trailingContentIsRejected() {
    assertThrows(JacksonException.class,
      () -> DataListUtil.getMapper().readValue("{\"zeta\":\"z\",\"alpha\":\"a\"},", Ordered.class));
  }
}
