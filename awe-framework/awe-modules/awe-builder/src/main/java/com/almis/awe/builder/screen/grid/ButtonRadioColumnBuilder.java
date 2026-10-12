package com.almis.awe.builder.screen.grid;

import com.almis.awe.builder.enumerates.Component;
import com.almis.awe.builder.screen.base.AbstractColumnBuilder;
import com.almis.awe.model.entities.screen.component.grid.Column;

/**
 * Button radio column: the same choice as the radio column, with the options of the list of the column drawn as one
 * joined bar of toggle buttons
 *
 * @author pgarcia
 */
public class ButtonRadioColumnBuilder extends AbstractColumnBuilder<ButtonRadioColumnBuilder, Column> {

  @Override
  public Column build() {
    return (Column) super.build(new Column())
      .setComponentType(Component.BUTTON_RADIO.toString());
  }
}
