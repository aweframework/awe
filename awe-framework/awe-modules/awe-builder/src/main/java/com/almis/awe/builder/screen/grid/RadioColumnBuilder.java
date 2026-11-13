package com.almis.awe.builder.screen.grid;

import com.almis.awe.builder.enumerates.Component;
import com.almis.awe.builder.screen.base.AbstractColumnBuilder;
import com.almis.awe.model.entities.screen.component.grid.Column;

/**
 * Radio column: a radio button per option of the list of the column in every cell. The options come from the initial
 * load and the target action of the column, like the ones of a select column, and the chosen value is the value of the
 * cell
 *
 * @author pgarcia
 */
public class RadioColumnBuilder extends AbstractColumnBuilder<RadioColumnBuilder, Column> {

  @Override
  public Column build() {
    return (Column) super.build(new Column())
      .setComponentType(Component.RADIO.toString());
  }
}
