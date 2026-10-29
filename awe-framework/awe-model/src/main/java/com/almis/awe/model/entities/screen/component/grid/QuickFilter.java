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
 * QuickFilter Class
 * <p>
 * Used to parse the quick-filter tag of a grid with XStream. It is a criterion of the screen whose options (value,
 * label and an optional count and tone) come from its initial load, shown by the grid in its toolbar as a group of
 * buttons. Its value is sent with the requests of the grid, and choosing another option filters the grid.
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("quick-filter")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuickFilter extends AbstractCriteria {

  @Serial
  private static final long serialVersionUID = 7313552096611436170L;

  // Component of the clients
  private static final String QUICK_FILTER = "quick-filter";

  // Server action that loads the options when the quick filter does not set one
  private static final String DEFAULT_SERVER_ACTION = "data";

  @Override
  public QuickFilter copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Retrieve the component type: always a quick filter
   *
   * @return <code>quick-filter</code>
   */
  @Override
  public String getComponentType() {
    return QUICK_FILTER;
  }

  /**
   * Retrieve the server action that loads the options
   *
   * @return Server action, <code>data</code> by default
   */
  @Override
  public String getServerAction() {
    return super.getServerAction() == null ? DEFAULT_SERVER_ACTION : super.getServerAction();
  }

  @Override
  @JsonIgnore
  public String getComponentTag() {
    return QUICK_FILTER;
  }

  /**
   * The quick filter has no markup where it is declared: the grid shows it in its toolbar
   *
   * @return Empty template
   */
  @Override
  @JsonIgnore
  public String getTemplate() {
    return AweConstants.TEMPLATE_EMPTY;
  }
}
