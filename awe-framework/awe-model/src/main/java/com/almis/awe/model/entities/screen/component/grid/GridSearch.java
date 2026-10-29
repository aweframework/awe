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
 * GridSearch Class
 * <p>
 * Used to parse the search tag of a grid with XStream. It is a text criterion of the screen, shown by the grid at the
 * start of its toolbar as a search field. It has no query of its own: its value is sent with the requests of the grid,
 * and typing in it filters the grid.
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("search")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GridSearch extends AbstractCriteria {

  @Serial
  private static final long serialVersionUID = -2716431839012748561L;

  // Component of the clients
  private static final String GRID_SEARCH = "grid-search";

  @Override
  public GridSearch copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve the component type: always a grid search
   *
   * @return <code>grid-search</code>
   */
  @Override
  public String getComponentType() {
    return GRID_SEARCH;
  }

  @Override
  @JsonIgnore
  public String getComponentTag() {
    return GRID_SEARCH;
  }

  /**
   * The search has no markup where it is declared: the grid shows it in its toolbar
   *
   * @return Empty template
   */
  @Override
  @JsonIgnore
  public String getTemplate() {
    return AweConstants.TEMPLATE_EMPTY;
  }
}
