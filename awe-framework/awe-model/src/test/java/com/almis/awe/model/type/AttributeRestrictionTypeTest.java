package com.almis.awe.model.type;

import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.component.grid.Column;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeRestrictionTypeTest {

  @Test
  void valueTypeFollowsTheTypeOfTheModelField() {
    assertThat(AttributeRestrictionType.PLACEHOLDER.getValueType()).isEqualTo(AttributeValueType.TEXT);
    assertThat(AttributeRestrictionType.CAPITALIZE.getValueType()).isEqualTo(AttributeValueType.BOOLEAN);
    assertThat(AttributeRestrictionType.TIMEOUT.getValueType()).isEqualTo(AttributeValueType.INTEGER);
    // Inherited from the component class
    assertThat(AttributeRestrictionType.VISIBLE.getValueType()).isEqualTo(AttributeValueType.BOOLEAN);
    // Has no field: it is turned into a validation
    assertThat(AttributeRestrictionType.REQUIRED.getValueType()).isEqualTo(AttributeValueType.BOOLEAN);
  }

  @Test
  void attributesWithAClosedSetOfValuesAreEnumerated() {
    assertThat(AttributeRestrictionType.ALIGN.getValueType()).isEqualTo(AttributeValueType.ENUM);
    assertThat(AttributeRestrictionType.ALIGN.getAllowedValues()).containsExactly("left", "center", "right");
    assertThat(AttributeRestrictionType.PLACEHOLDER.getAllowedValues()).isEmpty();
  }

  @Test
  void attributesApplyToTheComponentClassesThatInheritThem() {
    assertThat(AttributeRestrictionType.applicableTo(Column.class))
      .contains(AttributeRestrictionType.WIDTH, AttributeRestrictionType.PLACEHOLDER, AttributeRestrictionType.LABEL)
      .doesNotContain(AttributeRestrictionType.TOTALIZE);
    assertThat(AttributeRestrictionType.applicableTo(Criteria.class))
      .contains(AttributeRestrictionType.PLACEHOLDER)
      .doesNotContain(AttributeRestrictionType.WIDTH);
  }

  @Test
  void everyAttributeNameIsAFieldOfItsComponentClass() {
    // The generator sets the attribute through the field of that name; required is turned into a validation
    List<String> withoutField = Arrays.stream(AttributeRestrictionType.values())
      .filter(attribute -> attribute != AttributeRestrictionType.REQUIRED)
      .filter(attribute -> !hasField(attribute.getComponentClass(), attribute.getValue()))
      .map(Enum::name)
      .toList();

    assertThat(withoutField).isEmpty();
  }

  private static boolean hasField(Class<?> type, String name) {
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      if (Arrays.stream(current.getDeclaredFields()).anyMatch(field -> field.getName().equals(name))) {
        return true;
      }
    }
    return false;
  }
}
