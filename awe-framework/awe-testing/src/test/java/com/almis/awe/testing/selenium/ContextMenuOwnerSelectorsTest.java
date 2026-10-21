package com.almis.awe.testing.selenium;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Selectors that the steps use to right click the components that own a context menu (issue #801)
 */
class ContextMenuOwnerSelectorsTest {

  static Stream<IAweFrontEndInstructions> engines() {
    return Stream.of(new AngularAweInstructions(), new ReactAweInstructions());
  }

  @ParameterizedTest
  @MethodSource("engines")
  void activeWizardStepIsScopedToTheWizard(IAweFrontEndInstructions instructions) {
    String selector = instructions.getActiveWizardStepOf("RevWizard").toString();

    assertThat(selector).contains("criterion-id='RevWizard'").contains("wizard-step").contains("active");
  }
}
