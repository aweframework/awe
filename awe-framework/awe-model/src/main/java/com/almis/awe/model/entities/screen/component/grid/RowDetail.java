package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.Element;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * RowDetail Class
 * <p>
 * Used to parse the row-detail tag of a grid with XStream. Its text is the template of the area shown under a row when
 * the user expands it: {name} placeholders are replaced with the fields of the row.
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("row-detail")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = {"content"})
public class RowDetail extends Element {

  @Serial
  private static final long serialVersionUID = 2873913612830913261L;

  // Template of the detail (plain text with {name} placeholders). Not called "template" because the elements use
  // getTemplate() for the name of their screen template
  private String content;

  @Override
  public RowDetail copy() {
    return this.toBuilder().build();
  }

  @Override
  @JsonIgnore
  public String getTemplate() {
    // The clients render the detail: it generates no screen code
    return AweConstants.TEMPLATE_EMPTY;
  }
}
