package com.almis.awe.builder.screen;

import com.almis.awe.builder.enumerates.OnClose;
import com.almis.awe.builder.enumerates.SidePanelPosition;
import com.almis.awe.builder.screen.base.AbstractTagBuilder;
import com.almis.awe.model.entities.screen.component.SidePanel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Side panel builder: a dialog anchored to the left or right edge of the screen (#843)
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class SidePanelBuilder extends AbstractTagBuilder<SidePanelBuilder, SidePanel> {

  private OnClose onClose;
  private String icon;
  private boolean deferred;
  private SidePanelPosition position;
  private String maxWidth;

  @Override
  public SidePanel build() {
    return build(new SidePanel());
  }

  @Override
  public SidePanel build(SidePanel sidePanel) {
    super.build(sidePanel)
      .setIcon(getIcon());

    sidePanel.setMaxWidth(getMaxWidth());

    if (isDeferred()) {
      sidePanel.setDeferred(true);
    }

    if (getOnClose() != null) {
      sidePanel.setOnClose(getOnClose().toString());
    }

    if (getPosition() != null) {
      sidePanel.setPosition(getPosition().toString());
    }

    return sidePanel;
  }
}
