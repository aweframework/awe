package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamInclude;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.List;

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
@XStreamInclude({SidePanelHeaderTag.class})
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

  // Identity header (#793): while exactly one row of this grid is selected, the header shows that row (avatar, title,
  // subtitle and tags) instead of the label of the panel
  @XStreamAlias("header-grid")
  @XStreamAsAttribute
  private String headerGrid;

  // Field of the selected row with the title of the header
  @XStreamAlias("header-title-field")
  @XStreamAsAttribute
  private String headerTitleField;

  // Field of the selected row with the subtitle of the header
  @XStreamAlias("header-subtitle-field")
  @XStreamAsAttribute
  private String headerSubtitleField;

  // Field of the selected row the initials of the avatar are taken from
  @XStreamAlias("header-avatar-field")
  @XStreamAsAttribute
  private String headerAvatarField;

  // Field of the selected row with the tone of the avatar
  @XStreamAlias("header-avatar-tone-field")
  @XStreamAsAttribute
  private String headerAvatarToneField;

  // Field of the selected row with a colour (an AWE 5 palette name or a hex) drawn as a circle in place of the avatar
  @XStreamAlias("header-swatch-field")
  @XStreamAsAttribute
  private String headerSwatchField;

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
   * Returns the tags of the identity header, in the order of the XML
   *
   * @return Header tags (not sent when the panel has none)
   */
  @JsonGetter("headerTags")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public List<SidePanelHeaderTag> getHeaderTags() {
    return getChildrenByType(SidePanelHeaderTag.class);
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
