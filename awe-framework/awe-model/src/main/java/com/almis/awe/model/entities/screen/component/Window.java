package com.almis.awe.model.entities.screen.component;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serial;
import java.util.List;

/**
 * Window Class
 *
 * Used to parse a window tag with XStream
 *
 *
 * Generates a window structure with header center and footing
 *
 *
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("window")
public class Window extends Component {

  @Serial
  private static final long serialVersionUID = 5159433149044786985L;
  // Window can be maximized or not
  @XStreamAlias("maximize")
  @XStreamAsAttribute
  private Boolean maximize;

  // How the window shows its advanced block: inline (default) or side-panel
  @XStreamAlias("advanced-mode")
  @XStreamAsAttribute
  private String advancedMode;

  // Whether the window offers the saved searches of its advanced block (#790)
  @XStreamAlias("saved-searches")
  @XStreamAsAttribute
  private Boolean savedSearches;

  // Whether the current user left the advanced block open (#790): set at screen generation only when the user has a
  // state stored for the window, so windows without one send nothing
  @JsonProperty("advancedOpen")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @XStreamOmitField
  private transient Boolean advancedOpen;

  @Override
  public Window copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  /**
   * Returns is maximizable
   * @return Is maximizable
   */
  public boolean isMaximize() {
    return maximize != null && maximize;
  }

  /**
   * Retrieve component tag (to be overriden)
   *
   * @return component tag
   */
  @Override
  public String getComponentTag() {
    return "window";
  }

  /**
   * Returns if window allows to maximize/restore itself
   *
   * @return Window allows to maximize/restore itself
   */
  public boolean allowMaximize() {
    return isMaximize() && getLabel() != null;
  }

  /**
   * Returns if window allows to maximize/restore itself for JSON serialization
   *
   * @return Window allows to maximize/restore itself
   */
  @JsonGetter("maximize")
  public boolean getMaximizeConverter() {
    return allowMaximize();
  }

  /**
   * Returns if the window holds an advanced block, which turns its advanced search on
   *
   * @return Window has an advanced block
   */
  @JsonIgnore
  public boolean hasAdvancedBlock() {
    return !getElementsByType(Advanced.class).isEmpty();
  }

  /**
   * How the window shows its advanced block, for JSON serialization. Absent when the window has no advanced block
   *
   * @return inline or side-panel
   */
  @JsonGetter("advancedMode")
  public String getAdvancedModeConverter() {
    if (!hasAdvancedBlock()) {
      return null;
    }
    return advancedMode == null ? "inline" : advancedMode;
  }

  /**
   * Returns if the window offers saved searches: it opts in and holds an advanced block
   *
   * @return Window offers saved searches
   */
  @JsonIgnore
  public boolean hasSavedSearches() {
    return Boolean.TRUE.equals(savedSearches) && hasAdvancedBlock();
  }

  /**
   * Whether the window offers saved searches, for JSON serialization. Absent unless the window opts in and holds an
   * advanced block
   *
   * @return true or null
   */
  @JsonGetter("savedSearches")
  public Boolean getSavedSearchesConverter() {
    return hasSavedSearches() ? Boolean.TRUE : null;
  }

  /**
   * Identifiers of the advanced criteria: the non-hidden criteria inside the advanced block (any depth). Absent when
   * the window has no advanced block
   *
   * @return Advanced criteria identifiers
   */
  @JsonGetter("advancedCriteria")
  public List<String> getAdvancedCriteria() {
    if (!hasAdvancedBlock()) {
      return null;
    }
    return getElementsByType(Advanced.class).stream()
      .flatMap(advanced -> getFilterCriteria(advanced).stream())
      .toList();
  }

  /**
   * Identifiers of the criteria of the window whose values are shown as filters in effect: every non-hidden criterion,
   * in order. Absent when the window has no advanced block
   *
   * @return Search criteria identifiers
   */
  @JsonGetter("searchCriteria")
  public List<String> getSearchCriteria() {
    if (!hasAdvancedBlock()) {
      return null;
    }
    return getFilterCriteria(this);
  }

  /**
   * Retrieve the identifiers of the criteria of an element (any depth) that are search filters: all but the hidden ones
   *
   * @param element Element
   * @return Criteria identifiers
   */
  private static List<String> getFilterCriteria(Element element) {
    return element.getElementsByType(Criteria.class).stream()
      .filter(criterion -> !"hidden".equalsIgnoreCase(criterion.getComponentType()))
      .map(Criteria::getId)
      .toList();
  }

  /**
   * Get print element list (to be overwritten)
   *
   * @param printElementList Print element list
   * @param label            Previous label
   * @param parameters       Parameters
   * @param dataSuffix       Data suffix
   * @return Print bean
   */
  @JsonIgnore
  @Override
  public List<Element> getReportStructure(List<Element> printElementList, String label, ObjectNode parameters, String dataSuffix) {
    return super.getReportStructure(printElementList, getLabel() == null ? label : getLabel(), parameters, dataSuffix);
  }
}
