package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.converter.RowDetailConverter;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Include;
import com.almis.awe.model.entities.screen.component.action.Dependency;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

/**
 * RowDetail Class
 * <p>
 * Used to parse the row-detail tag of a grid with XStream. The area shown under a row when the user expands it is
 * either a template (its text, where {name} placeholders are replaced with the fields of the row) or components (a
 * single read-only grid, inline or through an include), which the clients create once per open row.
 * </p>
 * <p>
 * The components of a row detail are not screen components: searches by type do not look inside a row detail, so they
 * are not registered, do not load with the screen and are not part of the grid (its columns or buttons).
 * </p>
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamAlias("row-detail")
@XStreamConverter(RowDetailConverter.class)
public class RowDetail extends Element {

  @Serial
  private static final long serialVersionUID = 2873913612830913261L;

  /**
   * Reasons why a row detail definition is not valid, with the locale of their message
   */
  @Getter
  public enum DefinitionError {
    TEMPLATE_AND_COMPONENTS("ERROR_MESSAGE_ROW_DETAIL_TEMPLATE_AND_COMPONENTS"),
    NOT_A_GRID("ERROR_MESSAGE_ROW_DETAIL_NOT_A_GRID"),
    EDITABLE_GRID("ERROR_MESSAGE_ROW_DETAIL_EDITABLE_GRID"),
    DEPENDENCY("ERROR_MESSAGE_ROW_DETAIL_DEPENDENCY");

    private final String message;

    DefinitionError(String message) {
      this.message = message;
    }
  }

  // Template of the detail (plain text with {name} placeholders). Not called "template" because the elements use
  // getTemplate() for the name of their screen template. Read from the text of the tag by its converter
  @XStreamOmitField
  private String content;

  // Component generated for the clients from the grid of the detail (components mode only)
  @JsonIgnore
  @XStreamOmitField
  private transient ScreenComponent component;

  @Override
  public RowDetail copy() {
    // A template has no elements: keep it without list, as it was read
    return this.toBuilder()
      .elementList(isComponentMode() ? ListUtil.copyList(getElementList()) : null)
      .build();
  }

  @Override
  @JsonIgnore
  public String getTemplate() {
    // The clients render the detail: it generates no screen code
    return AweConstants.TEMPLATE_EMPTY;
  }

  /**
   * Components inside a row detail are not part of the screen nor of the grid: searches by type do not find them
   *
   * @param processDialog    flag to check dialog elements
   * @param elementClassList element class
   * @param <T>              element type
   * @return Empty list
   */
  @Override
  public <T> List<T> getElementsByType(boolean processDialog, Class<T>... elementClassList) {
    return Collections.emptyList();
  }

  /**
   * Retrieve all the elements inside the row detail, at any depth (except inside the row details of its components)
   *
   * @return Elements of the row detail
   */
  @JsonIgnore
  public List<Element> getDetailElements() {
    return super.getElementsByType(true, Element.class);
  }

  /**
   * Check if the detail is made of components instead of a template
   *
   * @return The row detail has components
   */
  @JsonIgnore
  public boolean isComponentMode() {
    return !getElementList().isEmpty();
  }

  /**
   * Retrieve the grid of the detail: its only child, directly or as the only element of an include
   *
   * @return Grid of the detail, null when the detail has no single grid
   */
  @JsonIgnore
  public Grid getDetailGrid() {
    List<Element> children = getElementList();
    if (children.size() != 1) {
      return null;
    }
    Element child = children.get(0);
    if (child instanceof Include include && include.getElementList().size() == 1) {
      child = include.getElementList().get(0);
    }
    return child instanceof Grid grid ? grid : null;
  }

  /**
   * Check the definition of the row detail: a template or a single read-only grid with no dependencies
   *
   * @return Definition error, null when the definition is valid
   */
  @JsonIgnore
  public DefinitionError getDefinitionError() {
    if (!isComponentMode()) {
      return null;
    }
    Grid grid = getDetailGrid();
    if (content != null && !content.isBlank()) {
      return DefinitionError.TEMPLATE_AND_COMPONENTS;
    } else if (grid == null) {
      return DefinitionError.NOT_A_GRID;
    } else if (grid.isEditable() || grid.isMultioperation()) {
      return DefinitionError.EDITABLE_GRID;
    } else if (getDetailElements().stream().anyMatch(Dependency.class::isInstance)) {
      return DefinitionError.DEPENDENCY;
    }
    return null;
  }
}
