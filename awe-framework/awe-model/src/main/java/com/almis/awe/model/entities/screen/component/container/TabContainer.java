package com.almis.awe.model.entities.screen.component.container;

import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * TabContainer Class
 *
 * Used to parse a tab with XStream
 *
 *
 * Generates an screen criteria
 *
 *
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@XStreamAlias("tabcontainer")
public class TabContainer extends Container {

  @Serial
  private static final long serialVersionUID = 485344960889412732L;

  // Run the initial load of the tab components when the tab is selected for the first time
  @XStreamAlias("deferred")
  @XStreamAsAttribute
  private Boolean deferred;

  /**
   * Returns if the data of the tab components is loaded when the tab is selected for the first time
   *
   * @return Tab is deferred
   */
  @JsonGetter("deferred")
  public Boolean getDeferred() {
    return deferred;
  }

  /**
   * Returns if the data of the components is deferred (helper for the server side, not a separate JSON property)
   *
   * @return Deferred
   */
  @JsonIgnore
  public boolean isDeferred() {
    return Boolean.TRUE.equals(deferred);
  }

  @Override
  public TabContainer copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  @Override
  public String getComponentTag() {
    return "tabcontainer";
  }
}
