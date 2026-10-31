package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.util.data.ListUtil;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Column Class
 * Used to parse a grid column with XStream
 * Column class extends from Criteria
 *
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@ToString
@XStreamAlias("column")
@JsonInclude(Include.NON_NULL)
public class Column extends AbstractCriteria {

  // Serial UID
  @Serial
  private static final long serialVersionUID = 1L;

  // Column FIELD name to sort
  @JsonProperty("sortField")
  @XStreamAlias("sort-field")
  @XStreamAsAttribute
  private String field;

  // Column POSITION in grid
  @XStreamOmitField
  private String position;

  // Column WIDTH in pixels
  @XStreamAlias("width")
  @XStreamAsAttribute
  private String width;

  // Column WIDTH in characters
  @JsonProperty("charlength")
  @XStreamAlias("charlength")
  @XStreamAsAttribute
  private Integer charLength;

  // Column ALIGN
  @XStreamAlias("align")
  @XStreamAsAttribute
  private String align;

  // Column is SORTABLE
  @XStreamAlias("sortable")
  @XStreamAsAttribute
  private Boolean sortable;

  // Column is MOVABLE
  @XStreamAlias("movable")
  @XStreamAsAttribute
  private Boolean movable;

  // Column is not VISIBLE
  @XStreamAlias("hidden")
  @XStreamAsAttribute
  private Boolean hidden;

  // Column is SENDABLE
  @XStreamAlias("sendable")
  @XStreamAsAttribute
  private Boolean sendable;

  // Totalise TYPE
  @XStreamAlias("summary-type")
  @XStreamAsAttribute
  private String summaryType;

  // Formatter
  @XStreamAlias("formatter")
  @XStreamAsAttribute
  private String formatter;

  // Formatter
  @XStreamAlias("format-options")
  @XStreamAsAttribute
  private String formatOptions;

  // Column is FROZEN
  @XStreamAlias("frozen")
  @XStreamAsAttribute
  private Boolean frozen;

  // Column is page break
  @XStreamAlias("pagebreak")
  @XStreamAsAttribute
  private Boolean pagebreak;

  // Cell text wraps over several lines
  @XStreamAlias("wrap")
  @XStreamAsAttribute
  private Boolean wrap;

  // Maximum number of lines of a wrapping cell
  @XStreamAlias("max-lines")
  @XStreamAsAttribute
  private Integer maxLines;

  // Field shown under the main value of the cell
  @XStreamAlias("secondary-field")
  @XStreamAsAttribute
  private String secondaryField;

  // Print and export the secondary field in its own column
  @XStreamAlias("export-secondary")
  @XStreamAsAttribute
  private Boolean exportSecondary;

  // Label of the exported secondary column
  @XStreamAlias("secondary-label")
  @XStreamAsAttribute
  private String secondaryLabel;

  // Field with the status tone of the cell
  @XStreamAlias("status-field")
  @XStreamAsAttribute
  private String statusField;

  // Cell value shown as a list of chips
  @XStreamAlias("chips")
  @XStreamAsAttribute
  private Boolean chips;

  // Separator of the chips items
  @XStreamAlias("chip-separator")
  @XStreamAsAttribute
  private String chipSeparator;

  // Maximum number of chips shown
  @XStreamAlias("max-chips")
  @XStreamAsAttribute
  private Integer maxChips;

  // Lower end of the scale of the bar of a progress column
  @XStreamAlias("bar-min")
  @XStreamAsAttribute
  private Double barMin;

  // Upper end of the scale of the bar of a progress column
  @XStreamAlias("bar-max")
  @XStreamAsAttribute
  private Double barMax;

  // Field with the second value drawn under the bar of a progress column
  @XStreamAlias("bar-second-field")
  @XStreamAsAttribute
  private String barSecondField;

  // Comma-separated fields with the stacked segments of the bar of a progress column
  @XStreamAlias("bar-stack-fields")
  @XStreamAsAttribute
  private String barStackFields;

  // Draw the bar of a progress column only on the leaf rows of a tree grid
  @XStreamAlias("bar-leaf-only")
  @XStreamAsAttribute
  private Boolean barLeafOnly;

  // Field with the name (or initials) of the avatar shown before the main value
  @XStreamAlias("avatar-field")
  @XStreamAsAttribute
  private String avatarField;

  // Field with the tone of the avatar
  @XStreamAlias("avatar-tone-field")
  @XStreamAsAttribute
  private String avatarToneField;

  // Field with a colour (an AWE 5 palette name or a hex) drawn as a circle before the main value, in place of the avatar
  @XStreamAlias("swatch-field")
  @XStreamAsAttribute
  private String swatchField;

  // Field with the icon shown before the main value (in place of the dot of a status)
  @XStreamAlias("icon-field")
  @XStreamAsAttribute
  private String iconField;

  // Dialog to open
  @XStreamAlias("dialog")
  @XStreamAsAttribute
  private String dialog;

  // Sparkline TYPE (line, bar or area)
  @XStreamAlias("sparkline-type")
  @XStreamAsAttribute
  private String sparklineType;

  // Sparkline series COLOR
  @XStreamAlias("sparkline-color")
  @XStreamAsAttribute
  private String sparklineColor;

  // Sparkline COLOR of the negative values
  @XStreamAlias("sparkline-negative-color")
  @XStreamAsAttribute
  private String sparklineNegativeColor;

  // Sparkline highlights the LAST POINT
  @XStreamAlias("sparkline-last-point")
  @XStreamAsAttribute
  private Boolean sparklineLastPoint;

  // Sparkline marks the MINIMUM and MAXIMUM points
  @XStreamAlias("sparkline-min-max")
  @XStreamAsAttribute
  private Boolean sparklineMinMax;

  // Sort by this column
  @XStreamOmitField
  private String sortColumn;

  @Override
  public Column copy() {
    return this.toBuilder()
      .elementList(ListUtil.copyList(getElementList()))
      .build();
  }

  @Override
  @JsonIgnore
  public String getTemplate() {
    // Retrieve code
    return AweConstants.TEMPLATE_EMPTY;
  }

  @Override
  @JsonIgnore
  public String getComponentTag() {
    return AweConstants.NO_TAG;
  }

  /**
   * Returns if the text of the cells of the column wraps over several lines: the column declares wrap, or a
   * maximum number of lines (which implies it). An explicit wrap="false" keeps the text in one line.
   *
   * @return Column wraps its text
   */
  @JsonIgnore
  public boolean isWrapped() {
    if (wrap != null) {
      return wrap;
    }
    return maxLines != null && maxLines > 0;
  }

  /**
   * Returns if column is hidden
   *
   * @return Column is hidden
   */
  @JsonGetter("hidden")
  public boolean isHidden() {
    return hidden != null && hidden;
  }

  /**
   * Returns if column is sendable
   *
   * @return Column is sendable
   */
  @JsonGetter("sendable")
  public boolean isSendable() {
    return sendable == null || sendable;
  }


  /**
   * Returns if column is movable
   *
   * @return Column is movable
   */
  @JsonGetter("movable")
  public boolean isMovable() {
    return movable == null || movable;
  }

  /**
   * Returns if column is sortable
   *
   * @return Column is sortable
   */
  @JsonGetter("sortable")
  public boolean isSortable() {
    return sortable == null || sortable;
  }

  /**
   * Returns if column is frozen
   *
   * @return Column is frozen
   */
  @JsonGetter("frozen")
  public boolean isFrozen() {
    return frozen != null && frozen;
  }

  /**
   * Returns if column is visible
   *
   * @return Column is visible
   */
  @Override
  @JsonGetter("visible")
  public boolean isVisible() {
    return !isHidden();
  }

  /**
   * Returns the declared print mode of the column, so the client can decide which
   * columns go to the report: `true` (always), `false` (never), `excel` (spreadsheet
   * outputs only) or null (only when visible on screen).
   *
   * @return Declared `printable` attribute value
   */
  @JsonGetter("printMode")
  public String getPrintMode() {
    return getPrintable();
  }

  /**
   * Returns the text of the column
   *
   * @return Text of the column
   */
  @JsonIgnore
  public String getText() {
    if (this.getLabel() != null) {
      return this.getLabel();
    } else {
      return "";
    }
  }

  @JsonIgnore
  @Override
  public String getElementKey() {
    return getName();
  }

  @JsonIgnore
  @Override
  public String getHelpTemplate() {
    // Retrieve code
    return AweConstants.TEMPLATE_HELP_COLUMN;
  }

  /**
   * @return the sort object
   */
  @JsonGetter("sort")
  public SortColumn getSortColumnHandler() {
    if (sortColumn != null) {
      return new SortColumn(null, sortColumn);
    }
    return null;
  }

  /**
   * Returns the action list
   *
   * @return Action list
   */
  @JsonGetter("actions")
  public List<ButtonAction> getActionList() {

    List<ButtonAction> actionList = new ArrayList<>();

    for (Element element : getElementList()) {
      if (element instanceof ButtonAction action) {
        actionList.add(action);
      }
    }

    return actionList;
  }
}
