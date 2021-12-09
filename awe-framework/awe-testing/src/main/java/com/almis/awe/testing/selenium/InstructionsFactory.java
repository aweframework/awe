package com.almis.awe.testing.selenium;

public class InstructionsFactory {
  private InstructionsFactory() {}
  public static IAweInstructions getInstance(String type) {
    switch (type) {
      case "react":
        return new ReactAweInstructions();
      case "angular":
      default:
        return new AngularAweInstructions();
    }
  }
}
