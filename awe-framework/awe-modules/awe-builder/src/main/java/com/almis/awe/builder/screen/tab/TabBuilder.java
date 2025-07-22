package com.almis.awe.builder.screen.tab;

import com.almis.awe.builder.screen.base.AbstractComponentBuilder;
import com.almis.awe.model.entities.screen.component.panelable.Tab;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author dfuentes
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class TabBuilder extends AbstractComponentBuilder<TabBuilder, Tab> {

  private Boolean maximize;
  private String orientation;

  @Override
  public Tab build() {
    return build(new Tab());
  }

  @Override
  public Tab build(Tab tab) {
    return super.build((Tab) tab
      .setMaximize(getMaximize())
      .setOrientation(getOrientation()));
  }

  /**
   * Add tab container
   *
   * @param tabContainerList
   * @return
   */
  public TabBuilder addTabContainerList(TabContainerBuilder... tabContainerList) {
    addAllElements(tabContainerList);
    return this;
  }
}
