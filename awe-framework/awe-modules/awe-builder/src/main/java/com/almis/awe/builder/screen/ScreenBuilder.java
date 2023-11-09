package com.almis.awe.builder.screen;

import com.almis.awe.builder.screen.base.AbstractElementBuilder;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.screen.Screen;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author dfuentes
 */
@Getter
@Setter
@Accessors(chain = true)
public class ScreenBuilder extends AbstractElementBuilder<ScreenBuilder, Screen> {

  private boolean keepCriteria;
  private String template = "full";
  private String onLoad;
  private String onUnload;
  private String target;
  private String menuType = AweConstants.PRIVATE_MENU;

  /**
   * Specific build that returns a service data with a screen client action.
   *
   * @return Generated screen
   */
  public Screen build() {
    return build(new Screen());
  }

  @Override
  public Screen build(Screen screen) {
    super.build(screen)
      .setKeepCriteria(isKeepCriteria())
      .setTemplate(getTemplate())
      .setOnLoad(getOnLoad())
      .setOnUnload(getOnUnload())
      .setTarget(getTarget());

    return screen;
  }

  /**
   * Add tag
   *
   * @param tag
   * @return
   */
  public ScreenBuilder addTag(TagBuilder... tag) {
    addAllElements(tag);
    return this;
  }

  /**
   * Add message
   *
   * @param message
   * @return
   */
  public ScreenBuilder addMessage(MessageBuilder... message) {
    addAllElements(message);
    return this;
  }
}
