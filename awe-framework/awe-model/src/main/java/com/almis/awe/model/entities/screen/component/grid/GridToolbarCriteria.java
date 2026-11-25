package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * GridToolbarCriteria Class
 * <p>
 * Used to parse the toolbar-criteria tag of a grid with XStream. It is a criterion of the screen of any component (a
 * checkbox switch, a select...), with its value, dependencies and actions as any criterion, that the grid shows in its
 * toolbar, at the end, just before the Columns button, instead of where it is declared.
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("toolbar-criteria")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GridToolbarCriteria extends AbstractCriteria {

  @Serial
  private static final long serialVersionUID = -3528720931874452613L;

  @Override
  public GridToolbarCriteria copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * The criterion has no markup where it is declared: the grid shows it in its toolbar
   *
   * @return Empty template
   */
  @Override
  @JsonIgnore
  public String getTemplate() {
    return AweConstants.TEMPLATE_EMPTY;
  }
}
