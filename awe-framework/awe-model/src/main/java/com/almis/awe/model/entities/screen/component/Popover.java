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
 * Popover: a dialog anchored to a control of the screen, opened and closed as a dialog (#843).
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("popover")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Popover extends Dialog {

  @Serial
  private static final long serialVersionUID = -6083517294627105382L;

  private static final String DEFAULT_POSITION = "bottom";

  // Id of the control the popover is anchored to
  @XStreamAlias("anchor")
  @XStreamAsAttribute
  private String anchor;

  // Side of the anchor where the popover opens (bottom, top, left or right)
  @XStreamAlias("position")
  @XStreamAsAttribute
  private String position;

  // Maximum width of the popover (CSS length)
  @XStreamAlias("max-width")
  @XStreamAsAttribute
  private String maxWidth;

  @Override
  public Popover copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Returns the side of the anchor where the popover opens
   *
   * @return Popover position (bottom by default)
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
    return "popover";
  }
}
