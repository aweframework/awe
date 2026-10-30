package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Advanced block of a window: the advanced criteria (and their layout tags), shown on demand below the advanced
 * filters row or in a side panel (#790)
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("advanced")
public class Advanced extends Component {

  @Serial
  private static final long serialVersionUID = 6203418657294013578L;

  @Override
  public Advanced copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve component tag
   *
   * @return Component tag
   */
  @Override
  public String getComponentTag() {
    return "advanced";
  }
}
