package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Side panel: a dialog anchored to the left or right edge of the screen, opened and closed as a dialog (#843).
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("side-panel")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SidePanel extends Dialog {

  @Serial
  private static final long serialVersionUID = 4120758291378016543L;

  private static final String DEFAULT_POSITION = "right";

  // Edge of the screen the panel is anchored to (left or right)
  @XStreamAlias("position")
  @XStreamAsAttribute
  private String position;

  // Maximum width of the panel (CSS length)
  @XStreamAlias("max-width")
  @XStreamAsAttribute
  private String maxWidth;

  @Override
  public SidePanel copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Returns the edge of the screen the panel is anchored to
   *
   * @return Panel position (right by default)
   */
  @JsonGetter("position")
  public String getPosition() {
    return position == null ? DEFAULT_POSITION : position;
  }

  /**
   * Retrieve component tag
   *
   * @return Component tag
   */
  @Override
  public String getComponentTag() {
    return "side-panel";
  }
}
