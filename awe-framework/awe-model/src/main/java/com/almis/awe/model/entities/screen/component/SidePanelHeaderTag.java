package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Tag of the identity header of a side panel (#793): a field of the selected row of the header grid shown as a tag, with
 * the tone of another field and, optionally, the icon of a third one. It has no markup where it is declared: the side
 * panel shows it in its header.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("header-tag")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SidePanelHeaderTag extends Element {

  @Serial
  private static final long serialVersionUID = 6182046633215770384L;

  // Field of the row with the text of the tag (an empty text hides the tag)
  @XStreamAlias("field")
  @XStreamAsAttribute
  private String field;

  // Field of the row with the tone of the tag (success, warning, danger, info or neutral)
  @XStreamAlias("tone-field")
  @XStreamAsAttribute
  private String toneField;

  // Field of the row with the icon of the tag (a plain tag: the icon takes the place of the dot)
  @XStreamAlias("icon-field")
  @XStreamAsAttribute
  private String iconField;

  @Override
  public SidePanelHeaderTag copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  @Override
  @JsonIgnore
  public String getTemplate() {
    return AweConstants.TEMPLATE_EMPTY;
  }
}
