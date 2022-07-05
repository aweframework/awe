package com.almis.awe.testing.selenium;

import com.almis.awe.testing.model.types.FrontendType;

public class InstructionsFactory {
  private InstructionsFactory() {}
  public static IAweInstructions getInstance(FrontendType type) {
    switch (type) {
      case REACT:
        return new ReactAweInstructions();
      case ANGULAR:
      default:
        return new AngularAweInstructions();
    }
  }
}
