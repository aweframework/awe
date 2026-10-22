package com.almis.awe.builder.screen;

import com.almis.awe.builder.enumerates.OnClose;
import com.almis.awe.builder.enumerates.PopoverPosition;
import com.almis.awe.builder.screen.base.AbstractTagBuilder;
import com.almis.awe.model.entities.screen.component.Popover;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Popover builder: a dialog anchored to a control of the screen (#843)
 */
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class PopoverBuilder extends AbstractTagBuilder<PopoverBuilder, Popover> {

  private OnClose onClose;
  private String icon;
  private boolean deferred;
  private String anchor;
  private PopoverPosition position;
  private String maxWidth;

  @Override
  public Popover build() {
    return build(new Popover());
  }

  @Override
  public Popover build(Popover popover) {
    super.build(popover)
      .setIcon(getIcon());

    popover.setAnchor(getAnchor());
    popover.setMaxWidth(getMaxWidth());

    if (isDeferred()) {
      popover.setDeferred(true);
    }

    if (getOnClose() != null) {
      popover.setOnClose(getOnClose().toString());
    }

    if (getPosition() != null) {
      popover.setPosition(getPosition().toString());
    }

    return popover;
  }
}
