package com.almis.awe.test.selenium;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Boxes of the panes of a split layout, as {@code getBounds} returns them ({@code {left, top, width, height}} in pixels),
 * and the checks of the tests that drag its splitters (#892)
 */
final class PaneBoxes {

  static final int LEFT = 0;
  static final int TOP = 1;
  static final int WIDTH = 2;
  static final int HEIGHT = 3;

  /**
   * Distance that the tests drag a splitter, in pixels
   */
  static final int SPLITTER_DRAG = 100;

  /**
   * A size or an edge may be this far from the expected one: the layout rounds to whole pixels, and the splitter of a
   * pane is a border of a few pixels that the drag starts from the middle of
   */
  static final double TOLERANCE = 4;

  private PaneBoxes() {
  }

  /**
   * Check that a size or an edge is the expected one, give or take {@link #TOLERANCE}
   *
   * @param what     What is measured
   * @param actual   Measured value
   * @param expected Expected value
   */
  static void checkClose(String what, double actual, double expected) {
    assertEquals(expected, actual, TOLERANCE, what);
  }

  /**
   * @param box Box of a pane
   * @return The box as text, for the messages
   */
  static String describe(double[] box) {
    return String.format("(left %.0f, top %.0f, width %.0f, height %.0f)", box[LEFT], box[TOP], box[WIDTH], box[HEIGHT]);
  }
}
