package com.almis.awe.builder.screen;

import com.almis.awe.builder.screen.base.AbstractTagBuilder;
import com.almis.awe.model.entities.screen.component.Advanced;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Advanced block builder: the advanced criteria of a window and their layout tags (#790)
 */
@NoArgsConstructor
@Accessors(chain = true)
public class AdvancedBuilder extends AbstractTagBuilder<AdvancedBuilder, Advanced> {

  @Override
  public Advanced build() {
    return build(new Advanced());
  }
}
