package com.almis.awe.model.entities.screen.component.button;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * Avatar Class
 * Used to add an avatar element with XStream
 *
 * @author Pablo García - 20/AGO/2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("avatar")
public class Avatar extends AbstractButton {

  @Serial
  private static final long serialVersionUID = 9093348372079212934L;

  // Image path
  @XStreamAlias("image")
  @XStreamAsAttribute
  private String image;

  // Show label
  @XStreamAlias("show-label")
  @XStreamAsAttribute
  private Boolean showLabel;

  /**
   * Returns if avatar must show the label
   * @return Component must show the label
   */
  @JsonGetter("showLabel")
  public boolean isShowLabel() {
    return getShowLabel() == null || getShowLabel();
  }


  /**
   * Retrieves value for JSON serialization
   *
   * @return value
   */
  @JsonGetter("text")
  public String getValueConverter() {
    return this.getValue();
  }

  /**
   * Get avatar dropdown children data
   *
   * @return Avatar dropdown children data
   */
  @JsonGetter("children")
  public Integer getChildren() {
    return getElementsByType(Component.class).size();
  }

  @Override
  @JsonIgnore
  public String getComponentTag() {
    return "avatar";
  }

  @JsonIgnore
  @Override
  public String getHelpTemplate() {
    // Retrieve code
    return AweConstants.TEMPLATE_HELP_EMPTY;
  }

  @Override
  public Avatar copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }
}
