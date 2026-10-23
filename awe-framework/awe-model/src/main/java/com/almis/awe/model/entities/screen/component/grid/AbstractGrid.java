package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.GridColumnLayout;
import com.almis.awe.model.dto.SortColumn;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.component.button.Button;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.entities.screen.component.pivottable.PivotTable;
import com.almis.awe.model.entities.screen.data.ScreenColumn;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamInclude;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.constant.AweConstants.NO_KEY;

/**
 * Grid Class
 * <p>
 * Used to parse a grid tag with XStream
 * Generates a screen data grid
 * </p>
 * @author Pablo GARCIA - 28/JUN/2010
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Accessors(chain = true)
@XStreamInclude({Grid.class, PivotTable.class})
public abstract class AbstractGrid extends AbstractCriteria {

  @Serial
  private static final long serialVersionUID = 3775377158422655923L;

  // Send all data of the grid or only selected rows
  @XStreamAlias("send-all")
  @XStreamAsAttribute
  private Boolean sendAll;

  // Column to EXPAND the TREEGRID
  @XStreamAlias("expand-column")
  @XStreamAsAttribute
  private String expandColumn;

  // Grid is TREEGRID
  @XStreamAlias("treegrid")
  @XStreamAsAttribute
  private Boolean treegrid;

  // Column name for TREEGRID Id
  @XStreamAlias("tree-id")
  @XStreamAsAttribute
  private String treeId;

  // Column name for TREEGRID parent
  @XStreamAlias("tree-parent")
  @XStreamAsAttribute
  private String treeParent;

  // Column name for TREEGRID leaf
  @XStreamAlias("tree-leaf")
  @XStreamAsAttribute
  private String treeLeaf;

  // Grid is EDITABLE
  @XStreamAlias("editable")
  @XStreamAsAttribute
  private Boolean editable;

  // User can select more than one record
  @XStreamAlias("multiselect")
  @XStreamAsAttribute
  private Boolean multiselect;

  // User can select more than one record
  @XStreamAlias("checkbox-multiselect")
  @XStreamAsAttribute
  private Boolean checkboxMultiselect;

  // Grid sends on submit row operation TYPE (insert, update, delete or none)
  @XStreamAlias("send-operations")
  @XStreamAsAttribute
  private Boolean multioperation;

  // Style class for total rows
  @XStreamAlias("totalize")
  @XStreamAsAttribute
  private String totalize;

  // Show row numbers or not
  @XStreamAlias("row-numbers")
  @XStreamAsAttribute
  private Boolean rowNumbers;

  // Show calculated totals
  @XStreamAlias("show-totals")
  @XStreamAsAttribute
  private Boolean showTotals;

  // Show calculated totals
  @XStreamAlias("enable-filters")
  @XStreamAsAttribute
  private Boolean enableFilters;

  // Validate on save
  @XStreamAlias("validate-on-save")
  @XStreamAsAttribute
  private Boolean validateOnSave;

  // Show the grid toolbar (columns and maximize)
  @XStreamAlias("show-toolbar")
  @XStreamAsAttribute
  private Boolean showToolbar;

  // How the toolbar shows the Columns: a modal dialog or a side panel (sent only when the grid sets it, so the
  // clients fall back to the application default)
  @XStreamAlias("columns-panel")
  @XStreamAsAttribute
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String columnsPanel;

  // Identifier of the screen the grid was generated for: the key, with the grid id, of the layout of the user
  @JsonProperty("screenId")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @XStreamOmitField
  private transient String screenId;

  // Column layout chosen by the current user (merged with the XML columns), sent next to the original column model
  @JsonProperty("columnLayout")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @XStreamOmitField
  private transient GridColumnLayout columnLayout;

  // Column list of grid
  @JsonProperty("columnModel")
  @XStreamOmitField
  private transient List<ScreenColumn> columnList;

  // List of buttons
  @XStreamOmitField
  private transient List<SortColumn> sortList;

  // Expand icon
  @XStreamAlias("icon-expand")
  @XStreamAsAttribute
  private String iconExpand;

  // Collapse icon
  @XStreamAlias("icon-collapse")
  @XStreamAsAttribute
  private String iconCollapse;

  // Leaf icon
  @XStreamAlias("icon-leaf")
  @XStreamAsAttribute
  private String iconLeaf;

  // Initial expansion level
  @XStreamAlias("initial-level")
  @XStreamAsAttribute
  private Integer initialLevel;

  // Disable pagination
  @XStreamAlias("pagination-disabled")
  @XStreamAsAttribute
  private Boolean disablePagination;

  // Grid pager values
  @XStreamAlias("pager-values")
  @XStreamAsAttribute
  private String pagerValues;

  // Grid row height
  @XStreamAlias("row-height")
  @XStreamAsAttribute
  private String rowHeight;

  // Empty state icon
  @XStreamAlias("empty-icon")
  @XStreamAsAttribute
  private String emptyIcon;

  // Empty state title (locale)
  @XStreamAlias("empty-title")
  @XStreamAsAttribute
  private String emptyTitle;

  // Empty state text (locale)
  @XStreamAlias("empty-text")
  @XStreamAsAttribute
  private String emptyText;

  // Empty state action (identifier of a button of the grid)
  @XStreamAlias("empty-action")
  @XStreamAsAttribute
  private String emptyAction;

  /**
   * Retrieve component tag
   *
   * @return <code>grid</code> tag
   */
  @Override
  public String getComponentType() {
    return "grid";
  }

  /**
   * Check if grid is editable
   *
   * @return Grid is editable
   */
  @JsonGetter("editable")
  public boolean isEditable() {
    return editable != null && editable;
  }

  /**
   * Check if grid is checkbox multiselect
   *
   * @return Grid is checkbox multiselect
   */
  @JsonGetter("checkboxMultiselect")
  public boolean isCheckboxMultiselect() {
    return checkboxMultiselect != null && checkboxMultiselect;
  }

  /**
   * Check if grid disable pagination
   *
   * @return Grid disable pagination
   */
  @JsonGetter("disablePagination")
  public boolean isDisablePagination() {
    return disablePagination != null && disablePagination;
  }

  /**
   * Check if grid has row numbers
   *
   * @return Grid has row numbers
   */
  @JsonGetter("rowNumbers")
  public boolean isRowNumbers() {
    return rowNumbers == null || rowNumbers;
  }

  /**
   * Check if grid send all data
   *
   * @return Grid send all data
   */
  @JsonGetter("sendAll")
  public boolean isSendAll() {
    return sendAll != null && sendAll;
  }

  /**
   * Check if grid is multiselect
   *
   * @return Grid is multiselect
   */
  @JsonGetter("multiselect")
  public boolean isMultiselect() {
    return multiselect != null && multiselect;
  }

  /**
   * Check if the grid shows its toolbar
   *
   * @return Grid shows the toolbar
   */
  @JsonGetter("showToolbar")
  public boolean isShowToolbar() {
    return showToolbar == null || showToolbar;
  }

  /**
   * Retrieve the columns a user can show, hide and reorder: the ones the XML does not hide, in XML order, with the
   * key of their group header (null when they have none). The key is the position of the group header in the grid.
   *
   * @return Layout columns by name
   */
  @JsonIgnore
  public Map<String, String> getLayoutColumns() {
    Map<String, String> layoutColumns = new LinkedHashMap<>();
    int groupIndex = 0;
    for (Element element : getElementList()) {
      if (element instanceof GroupHeader groupHeader) {
        addLayoutColumns(layoutColumns, groupHeader.getElementList(), String.valueOf(groupIndex++));
      } else {
        addLayoutColumns(layoutColumns, List.of(element), null);
      }
    }
    return layoutColumns;
  }

  private void addLayoutColumns(Map<String, String> layoutColumns, List<Element> elements, String group) {
    for (Element element : elements) {
      if (element instanceof Column column && !column.isHidden() && column.getName() != null) {
        layoutColumns.put(column.getName(), group);
      }
    }
  }

  /**
   * Check if grid is multioperation
   *
   * @return Grid is multoperation
   */
  @JsonGetter("multioperation")
  public boolean isMultioperation() {
    return multioperation != null && multioperation;
  }

  /**
   * Check if grid is validate on save
   *
   * @return Grid is validate on save
   */
  @JsonGetter("validateOnSave")
  public boolean isValidateOnSave() {
    return validateOnSave == null || validateOnSave;
  }

  /**
   * Check if grid show totals
   *
   * @return Grid show totals
   */
  @JsonGetter("showTotals")
  public boolean isShowTotals() {
    return showTotals != null && showTotals;
  }

  /**
   * Returns if grid is a treegrid or not
   *
   * @return Grid is a treegrid
   */
  @JsonGetter("treegrid")
  public boolean isTreegrid() {
    return treegrid != null && treegrid;
  }

  @JsonIgnore
  @Override
  public String getComponentTag() {
    return isTreegrid() ? "tree-grid" : "grid";
  }

  /**
   * Returns a list with the grid pager values for JSON serialization
   *
   * @return Column names list
   */
  @JsonGetter("pagerValues")
  public List<Integer> getGridPagerValuesConverter() {
    // Variable definition
    List<Integer> pagerValueList = new ArrayList<>();

    if (this.getPagerValues() != null) {
      String[] strPagerValues = this.getPagerValues().replaceAll("\\s+", "").split(",");
      for (String value : strPagerValues) {
        pagerValueList.add(Integer.valueOf(value));
      }
    }

    // Return array
    return pagerValueList;
  }

  /**
   * Returns the column list description for JSON serialization
   *
   * @return Column Model List
   */
  @JsonGetter("headerModel")
  public List<GroupHeader> getGroupHeaderModel() {

    // Variable definition
    List<GroupHeader> groupHeaderModel = new ArrayList<>();

    // Get elements (columns)
    for (Element element : getElementList()) {
      if (element instanceof GroupHeader groupHeader) {
        // Get GROUP header
        // Retrieve column model
        groupHeaderModel.add(groupHeader);
      }
    }

    // Return string parameter list
    return groupHeaderModel;
  }

  /**
   * Returns a the button list description for JSON serialization
   *
   * @return Button Model List
   */
  @JsonGetter("buttonModel")
  public List<Button> getButtonModelConverter() {

    List<Button> buttonModel = new ArrayList<>();

    // Get elements (buttons)
    for (Element element : getElementList()) {
      if (isButtonValid(element)) {
        // Get button
        Button button = (Button) element;
        // Retrieve button menu model
        buttonModel.add(button);
      }
    }

    // Return string parameter list
    return buttonModel;
  }

  /**
   * Retrieve the row detail of the grid
   *
   * @return Row detail, null when the grid has none
   */
  @JsonIgnore
  public RowDetail getRowDetail() {
    return getChildrenByType(RowDetail.class).stream().findFirst().orElse(null);
  }

  /**
   * Returns the row detail description for JSON serialization
   *
   * @return Row detail, empty (not sent) when the grid has none: its style (when set) and either its template or, when
   * it has components, the component the clients create for each open row (when it has been generated)
   */
  @JsonGetter("rowDetail")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public Map<String, Object> getRowDetailConverter() {
    RowDetail rowDetail = getRowDetail();
    if (rowDetail == null) {
      return Collections.emptyMap();
    }
    Map<String, Object> description = new LinkedHashMap<>();
    if (!rowDetail.isComponentMode()) {
      description.put("template", rowDetail.getContent() == null ? "" : rowDetail.getContent());
    }
    if (rowDetail.getStyle() != null) {
      description.put("style", rowDetail.getStyle());
    }
    if (rowDetail.getComponent() != null) {
      description.put("component", rowDetail.getComponent());
    }
    return description;
  }

  /**
   * Checks if an element is a valid button
   *
   * @param element Element
   * @return Is valid or not
   */
  private boolean isButtonValid(Element element) {
    String elementKey = element.getElementKey();
    return elementKey != null && !NO_KEY.equalsIgnoreCase(elementKey) && element instanceof Button;
  }

  /**
   * Retrieve column by column id
   *
   * @param name column id
   * @return Column found
   */
  public ScreenColumn getColumnById(String name) {
    ScreenColumn columnFound = null;
    // Get grid columns
    List<ScreenColumn> columns = getColumnList();
    for (ScreenColumn columnComponent : columns) {
      // Generate the children
      if (name.equalsIgnoreCase(columnComponent.getId())) {
        columnFound = columnComponent;
      }
    }
    return columnFound;
  }

  @JsonIgnore
  @Override
  public String getHelpTemplate() {
    // Retrieve code
    return AweConstants.TEMPLATE_HELP_GRID;
  }

  @Override
  public ST generateHelpTemplate(STGroup group, String label, boolean developers) {
    ST template = group.createStringTemplate(group.rawGetTemplate(getHelpTemplate()));
    List<ST> columns = new ArrayList<>();
    List<ST> buttons = new ArrayList<>();
    String currentLabel = getLabel() == null ? label : getLabel();

    // Get grid columns
    List<Column> gridColumns = getElementsByType(Column.class);
    for (Column column : gridColumns) {
      // Generate the children
      if (!column.isHidden() && column.getLabel() != null) {
        columns.add(column.generateHelpTemplate(group, null, developers));
      }
    }

    // Get grid buttons
    List<Button> buttonList = getElementsByType(Button.class);
    for (Button button : buttonList) {
      // Generate the children
      buttons.add(button.generateHelpTemplate(group, null, AweConstants.TEMPLATE_HELP_BUTTON_GRID, developers));
    }

    // Generate template
    template.add("e", this).add("label", currentLabel).add("developers", developers).add("columns", columns).add("buttons", buttons);

    // Retrieve code
    return template;
  }

  /**
   * Get print element list (to be overwritten)
   *
   * @param printElementList Print element list
   * @param label            Last label
   * @param parameters       Parameters
   * @param dataSuffix       Data suffix
   * @return Print bean
   */
  @JsonIgnore
  @Override
  public List<Element> getReportStructure(List<Element> printElementList, String label, ObjectNode parameters, String dataSuffix) {
    if (getLabel() == null) {
      setLabel(label);
    }
    printElementList.add(this);
    return printElementList;
  }
}
