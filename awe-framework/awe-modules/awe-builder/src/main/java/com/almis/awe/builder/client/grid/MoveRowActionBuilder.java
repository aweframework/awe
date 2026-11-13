package com.almis.awe.builder.client.grid;

import com.almis.awe.builder.client.ClientActionBuilder;
import com.almis.awe.builder.enumerates.RowPosition;

/**
 * Move row action builder. Moves the selected row of a grid to a new position
 *
 * @author pgarcia
 */
public class MoveRowActionBuilder extends ClientActionBuilder<MoveRowActionBuilder> {

  private static final String TYPE_PREFIX = "move-row-";
  private static final String SELECTED_ROW = "selectedRow";

  /**
   * Empty constructor (moves the selected row to the bottom)
   */
  public MoveRowActionBuilder() {
    this(RowPosition.BOTTOM);
  }

  /**
   * Constructor with position
   *
   * @param position Row position (TOP, UP, DOWN or BOTTOM). Null means BOTTOM
   */
  public MoveRowActionBuilder(RowPosition position) {
    setType(TYPE_PREFIX + (position == null ? RowPosition.BOTTOM : position).name().toLowerCase());
  }

  /**
   * Constructor with position and target
   *
   * @param position Row position
   * @param target   Target grid id
   */
  public MoveRowActionBuilder(RowPosition position, String target) {
    this(position);
    setTarget(target);
  }

  /**
   * Constructor with position, target and the row to move
   *
   * @param position Row position
   * @param target   Target grid id
   * @param rowId    Row identifier to move
   */
  public MoveRowActionBuilder(RowPosition position, String target, String rowId) {
    this(position, target);
    addParameter(SELECTED_ROW, rowId);
  }
}
