package com.almis.awe.service.report;


import com.almis.ade.api.bean.component.*;
import com.almis.ade.api.bean.component.Image;
import com.almis.ade.api.bean.component.grid.GridHeader;
import com.almis.ade.api.bean.component.grid.ReportColumn;
import com.almis.ade.api.bean.component.grid.ReportGrid;
import com.almis.ade.api.bean.component.grid.ReportHeader;
import com.almis.ade.api.bean.input.DataBean;
import com.almis.ade.api.bean.input.PrintBean;
import com.almis.ade.api.bean.section.PageFooter;
import com.almis.ade.api.bean.section.PageHeader;
import com.almis.ade.api.bean.style.AdeStyle;
import com.almis.ade.api.bean.style.StyleTemplate;
import com.almis.ade.api.enumerate.ColumnType;
import com.almis.ade.api.enumerate.HorizontalTextAlignment;
import com.almis.ade.api.enumerate.LayoutType;
import com.almis.ade.api.enumerate.PageOrientation;
import com.almis.ade.api.enumerate.PagingType;
import com.almis.ade.api.enumerate.SparklineType;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.PrintColumnData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.chart.Chart;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.screen.component.grid.RowDetail;
import com.almis.awe.model.type.OutputFormatType;
import com.almis.awe.model.type.TotalizeStyleType;
import com.almis.awe.service.QueryService;
import lombok.extern.log4j.Log4j2;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Generate the component controllers of the screen
 */
@Log4j2
public class ReportDesigner extends ServiceConfig {

  // Report colors
  // TODO: Make them customizable
  private static final Color REPORT_HEADER_TEXT_COLOR = new Color(100, 100, 100);
  private static final Color REPORT_HEADER_LINE_COLOR = new Color(200, 200, 200);
  private static final Integer REPORT_HEADER_TEXT_SIZE = 12;
  private static final Integer REPORT_FOOTER_TEXT_SIZE = 10;
  private static final Integer CRITERIA_GAP = 4;
  private static final Integer TOP_PADDING = 10;
  private static final Integer TOP_PADDING_SMALL = 5;
  private static final Integer MAX_GRID_FONT_SIZE = 10;
  private static final Integer MIN_GRID_FONT_SIZE = 5;
  private static final Float GRID_FONT_CORRECTION_RATIO = 1.7f;
  private static final String PRINTABLE_SPREADSHEET_ONLY = "excel";
  private static final String NOT_PRINTABLE = "false";
  private static final String ACTION_COLUMN_COMPONENT = "button";
  // Rows of a nested row detail grid when it declares no max: each printed row runs one query, so it is always limited
  private static final int MAX_DETAIL_ROWS = 100;

  // Autowired services
  private final QueryService queryService;
  private final ObjectMapper mapper;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param queryService Query service
   * @param baseConfigProperties Base config properties
   */
  public ReportDesigner(QueryService queryService, ObjectMapper mapper, BaseConfigProperties baseConfigProperties) {
    this.queryService = queryService;
    this.mapper = mapper;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Design the report
   *
   * @param reportStructure Report structure
   * @param parameters      Screen parameters
   * @return Print bean designed
   * @throws AWException Error designing report
   */
  public PrintBean getPrintDesign(List<Element> reportStructure, ObjectNode parameters) throws AWException {
    return getPrintDesign(reportStructure, parameters, false);
  }

  /**
   * Design the report for a given output profile.
   * Grid columns declared with `printable="excel"` are only included when
   * designing for spreadsheet outputs.
   *
   * @param reportStructure               Report structure
   * @param parameters                    Screen parameters
   * @param includeSpreadsheetOnlyColumns Include columns declared as `printable="excel"`
   * @return Print bean designed
   * @throws AWException Error designing report
   */
  public PrintBean getPrintDesign(List<Element> reportStructure, ObjectNode parameters, boolean includeSpreadsheetOnlyColumns) throws AWException {
    Layout layout = new Layout("report-layout", LayoutType.MULTIPAGE);
    PrintBean printBean = new PrintBean()
      .setDetail(layout);

    // Set orientation if defined
    if (parameters.has(AweConstants.PRINT_ORIENTATION) && !parameters.get(AweConstants.PRINT_ORIENTATION).isNull()) {
      printBean.setOrientation(PageOrientation.valueOf(parameters.get(AweConstants.PRINT_ORIENTATION).asString("")));
    }

    // For each element, generate report bean
    for (Element element : reportStructure) {
      if (element instanceof Screen screen) {
        getElementDesign(printBean, screen);
      } else if (element instanceof Grid grid) {
        getElementDesign(printBean, grid, parameters, includeSpreadsheetOnlyColumns);
      } else if (element instanceof Chart chart) {
        getElementDesign(printBean, chart, parameters);
      } else if (element instanceof Criteria criteria) {
        initializeCriteriaLayout(layout);
        getElementDesign(printBean, criteria, parameters);
      } else {
        getElementDesign(printBean, element);
      }
    }

    // Default orientation
    if (printBean.getOrientation() == null) {
      printBean.setOrientation(PageOrientation.PORTRAIT);
    }

    // Retrieve print bean generated
    return printBean;
  }

  /**
   * Check whether the output format is a spreadsheet, so columns declared as
   * `printable="excel"` must be included.
   *
   * @param format Output format name
   * @return Format is a spreadsheet output
   */
  public static boolean isSpreadsheetFormat(String format) {
    try {
      OutputFormatType type = OutputFormatType.valueOf(format.toUpperCase());
      return type == OutputFormatType.XLSX || type == OutputFormatType.CSV;
    } catch (IllegalArgumentException | NullPointerException exc) {
      return false;
    }
  }

  /**
   * Check whether any grid of the report structure declares columns that must be
   * printed only on spreadsheet outputs.
   *
   * @param reportStructure Report structure
   * @param parameters      Screen parameters
   * @return Some grid has `printable="excel"` columns
   */
  public boolean hasSpreadsheetOnlyColumns(List<Element> reportStructure, ObjectNode parameters) {
    return reportStructure.stream()
      .filter(Grid.class::isInstance)
      .map(Grid.class::cast)
      .anyMatch(grid -> {
        try {
          return getVisibleColumns(grid, parameters).stream().anyMatch(this::hasSpreadsheetOnlyColumn);
        } catch (IOException | NullPointerException exc) {
          return false;
        }
      });
  }

  /**
   * Check whether the spreadsheet outputs need their own design: a grid declares columns printed only on spreadsheets
   * (`printable="excel"`) or a column with a secondary line, which documents print inside the cell and spreadsheets in
   * their own column when asked
   *
   * @param reportStructure Report structure
   * @param parameters      Screen parameters
   * @return Spreadsheets are designed apart from documents
   */
  public boolean needsSpreadsheetDesign(List<Element> reportStructure, ObjectNode parameters) {
    return hasSpreadsheetOnlyColumns(reportStructure, parameters) || reportStructure.stream()
      .filter(Grid.class::isInstance)
      .map(Grid.class::cast)
      .filter(grid -> parameters.get(grid.getId() + baseConfigProperties.getComponent().getDataSuffix()) != null)
      .anyMatch(grid -> {
        try {
          return Optional.ofNullable(getVisibleColumns(grid, parameters)).orElse(List.of()).stream()
            .anyMatch(this::hasSecondaryField);
        } catch (IOException exc) {
          log.warn("Print design: cannot read the visible columns of grid '{}'", grid.getId(), exc);
          return false;
        }
      });
  }

  /**
   * Check whether a print column (or any column of a header) has a secondary field
   *
   * @param column Print column
   * @return Column has a secondary field
   */
  private boolean hasSecondaryField(PrintColumnData column) {
    if (column.isHeader()) {
      return Optional.ofNullable(column.getColumnList()).orElse(List.of()).stream().anyMatch(this::hasSecondaryField);
    }
    return column.getSecondaryField() != null && !column.getSecondaryField().isEmpty();
  }

  /**
   * Check whether a print column (or any column of a header) is spreadsheet only
   *
   * @param column Print column
   * @return Column is spreadsheet only
   */
  private boolean hasSpreadsheetOnlyColumn(PrintColumnData column) {
    if (column.isHeader()) {
      return Optional.ofNullable(column.getColumnList()).orElse(List.of()).stream().anyMatch(this::hasSpreadsheetOnlyColumn);
    }
    return !isActionColumn(column) && isSpreadsheetOnly(column);
  }

  /**
   * Check whether a print column is declared as `printable="excel"`
   *
   * @param column Print column
   * @return Column is spreadsheet only
   */
  private boolean isSpreadsheetOnly(PrintColumnData column) {
    return PRINTABLE_SPREADSHEET_ONLY.equalsIgnoreCase(column.getPrintable());
  }

  /**
   * Check whether a print column is an action column (`component="button"`): it is not data, so it is never printed
   * nor exported, whatever its `printable` value
   *
   * @param column Print column
   * @return Column is an action column
   */
  private boolean isActionColumn(PrintColumnData column) {
    return ACTION_COLUMN_COMPONENT.equalsIgnoreCase(column.getComponent());
  }

  /**
   * Check whether a print column is declared as `printable="false"`: it is never printed nor exported, whatever the
   * client sends
   *
   * @param column Print column
   * @return Column is not printable
   */
  private boolean isNotPrintable(PrintColumnData column) {
    return NOT_PRINTABLE.equalsIgnoreCase(column.getPrintable());
  }

  /**
   * Check whether a print column goes to the report: never an action column nor a column declared not printable, and a
   * spreadsheet only column only on spreadsheet outputs
   *
   * @param column                        Print column
   * @param includeSpreadsheetOnlyColumns Include columns declared as `printable="excel"`
   * @return Column is printed
   */
  private boolean isPrinted(PrintColumnData column, boolean includeSpreadsheetOnlyColumns) {
    return !isActionColumn(column) && !isNotPrintable(column)
      && (includeSpreadsheetOnlyColumns || !isSpreadsheetOnly(column));
  }

  /**
   * Initialize criteria layout
   *
   * @param layout Multi page layout
   */
  private void initializeCriteriaLayout(Layout layout) {
    if (layout.getElements().isEmpty() ||
      !"criteria-layout".equalsIgnoreCase(layout.getElements().get(layout.getElements().size() - 1).getIdentifier())) {
      Layout criteriaLayout = new Layout("criteria-layout", LayoutType.HORIZONTAL).setGap(CRITERIA_GAP);
      criteriaLayout.getStyle()
        .setTopPadding(TOP_PADDING)
        .setBottomPadding(TOP_PADDING);
      layout.addElement(criteriaLayout);
    }
  }

  /**
   * Get element design for screen
   *
   * @param printBean Print bean
   * @param element   Element
   */
  private void getElementDesign(PrintBean printBean, Screen element) {
    String title = getLocale(element.getLabel());
    // Generate layouts
    PageHeader header = new PageHeader("page-header");
    PageFooter footer = new PageFooter("page-footer");

    // Generate header
    Text headerTitle = new Text("report-header")
      .setValue(title);
    headerTitle.getStyle()
      .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
      .bold()
      .setFontSize(REPORT_HEADER_TEXT_SIZE)
      .setForegroundColor(REPORT_HEADER_TEXT_COLOR);
    header.setLine(new Line("header-line")
      .setLineColor(REPORT_HEADER_LINE_COLOR));
    header.setTitle(headerTitle);

    // Generate footer
    Paging footerPaging = new Paging("report-pagination", PagingType.X_SLASH_Y);
    footerPaging.getStyle()
      .setHorizontalTextAlignment(HorizontalTextAlignment.RIGHT)
      .setFontSize(REPORT_FOOTER_TEXT_SIZE)
      .setForegroundColor(REPORT_HEADER_TEXT_COLOR);
    footer.setLine(new Line("footer-line")
      .setLineColor(REPORT_HEADER_LINE_COLOR));
    footer.setPaging(footerPaging);

    // Add to print bean
    printBean
      .setPageHeader(header)
      .setPageFooter(footer);
  }

  /**
   * Get element design for criteria
   *
   * @param printBean  Print bean
   * @param element    Element
   * @param parameters Parameters
   */
  private void getElementDesign(PrintBean printBean, Criteria element, ObjectNode parameters) {
    Layout layout = (Layout) ((Layout) printBean.getDetail()).getElements().get(((Layout) printBean.getDetail()).getElements().size() - 1);

    // Add criterion element to layout as text
    Criterion criterionElement = new Criterion(element.getId());

    // Get criterion data
    JsonNode data = parameters.get(element.getId() + baseConfigProperties.getComponent().getDataSuffix());

    if (data != null) {
      JsonNode criterionText = data.get(AweConstants.JSON_TEXT_PARAMETER);
      if (criterionText == null) {
        log.warn("Print design: criterion '{}' data has no '{}' field (received a {} node). Another component with the same identifier (e.g. a grid column) has overwritten its print parameter. Rename one of them to print this criterion.",
          element.getId(), AweConstants.JSON_TEXT_PARAMETER, data.getNodeType());
        return;
      }
      String criterionValue = criterionText.asString("");
      if (criterionValue != null && !criterionValue.isEmpty()) {
        // Configure criterion label
        String criterionLabel = null;
        if (element.getLabel() != null) {
          criterionLabel = getLocale(element.getLabel());
        }

        if (criterionLabel != null) {
          // Set criterion values
          criterionElement.setTitle(criterionLabel)
            .setValue(criterionValue);

          // Add element to layout
          layout.addElement(criterionElement)
            .getStyle()
            .setTopPadding(TOP_PADDING_SMALL);
        }
      }
    }
  }

  /**
   * Get element design for grid
   *
   * @param printBean  Print bean
   * @param element    Element
   * @param parameters Parameters
   */
  private void getElementDesign(PrintBean printBean, Grid element, ObjectNode parameters, boolean includeSpreadsheetOnlyColumns) throws AWException {
    Layout layout = (Layout) printBean.getDetail();
    Layout gridLayout = new Layout("grid-layout", LayoutType.VERTICAL);
    layout.addElement(gridLayout);

    // Add grid element to layout
    ReportGrid gridElement = new ReportGrid(element.getId());

    // Set grid title
    if (element.getLabel() != null) {
      Text gridTitleText = new Text(element.getId() + "-title")
        .setValue(getLocale(element.getLabel()))
        .setStyle(new AdeStyle(StyleTemplate.TITLE_STYLE));
      gridElement.setTitle(gridTitleText);
    }

    try {
      // Get grid data
      List<PrintColumnData> visibleColumns = getVisibleColumns(element, parameters);

      // Generate columns
      Map<String, Column> definitions = getColumnDefinitions(element);
      GridPrintContext context = new GridPrintContext(definitions, includeSpreadsheetOnlyColumns, new GridCellPrinter(mapper),
        getGroupLevels(element, parameters, definitions), !Boolean.FALSE.equals(element.getGroupSubtotals()), createRowDetailSpec(element, includeSpreadsheetOnlyColumns));
      List<String> fields = setGridColumns(gridElement, visibleColumns, includeSpreadsheetOnlyColumns, context);

      // Set grid orientation and font size
      calculateGridOrientationAndFont(gridElement, printBean);

      // Add data to grid
      setGridData(element, gridElement, fields, parameters, context);

      // Add grid to layout
      gridLayout.addElement(gridElement)
        .getStyle()
        .setTopPadding(TOP_PADDING);
    } catch (Exception exc) {
      throw new AWException("Error trying to read visible columns from screen", exc);
    }
  }

  /**
   * Add sort data to parameters
   *
   * @param reportGrid Report grid
   */
  private Integer getGridWidth(ReportGrid reportGrid) {
    int gridWidth = 0;
    for (GridHeader header : reportGrid.getGridHeaders()) {
      if (header instanceof ReportColumn reportColumn) {
        gridWidth += reportColumn.getWidth() == null ? 0 : reportColumn.getWidth();
      } else if (header instanceof ReportHeader reportHeader) {
        for (ReportColumn column : reportHeader.getColumns()) {
          gridWidth += column.getWidth() == null ? 0 : column.getWidth();
        }
      }
    }
    return gridWidth;
  }


  /**
   * Add sort data to parameters
   *
   * @param reportGrid Report grid
   */
  private void calculateGridOrientationAndFont(ReportGrid reportGrid, PrintBean printBean) {
    Integer gridWidth = getGridWidth(reportGrid);
    int gridPageSize = PageOrientation.LANDSCAPE.equals(printBean.getOrientation()) ?
      Image.Size.FULL_SIZE_LANDSCAPE.getSize() : Image.Size.FULL_SIZE.getSize();
    if (gridWidth > Image.Size.FULL_SIZE.getSize()) {
      if (printBean.getOrientation() == null) {
        printBean.setOrientation(PageOrientation.LANDSCAPE);
        gridPageSize = Image.Size.FULL_SIZE_LANDSCAPE.getSize();
      }
      float fontSize = MAX_GRID_FONT_SIZE - (gridWidth * GRID_FONT_CORRECTION_RATIO / gridPageSize);
      reportGrid.setFontSize(Math.max(Math.round(fontSize), MIN_GRID_FONT_SIZE));
    } else {
      reportGrid.setFontSize(MAX_GRID_FONT_SIZE);
    }
  }

  /**
   * Get visible columns
   *
   * @param grid       Grid element
   * @param parameters Parameters
   * @return Visible columns
   */
  private List<PrintColumnData> getVisibleColumns(Grid grid, ObjectNode parameters) throws IOException {
    return mapper.convertValue(parameters.get(grid.getId() + baseConfigProperties.getComponent().getDataSuffix()).get(AweConstants.JSON_VISIBLE_COLUMNS), new TypeReference<List<PrintColumnData>>(){});
  }

  /**
   * Lift the grid pagination (sort, page size and current page) and its column filters from the grid data node to the
   * parameters root, so the relaunched query returns the same rows the user is looking at.
   * Values the client did not send are left untouched so the query keeps its own defaults.
   *
   * @param grid       Grid element
   * @param parameters Parameters
   */
  private void addGridPaginationData(Grid grid, ObjectNode parameters) {
    JsonNode gridData = parameters.get(grid.getId() + baseConfigProperties.getComponent().getDataSuffix());
    for (String key : List.of(AweConstants.COMPONENT_SORT, AweConstants.COMPONENT_MAX, AweConstants.COMPONENT_PAGE,
      AweConstants.COMPONENT_COLUMN_FILTERS)) {
      JsonNode value = gridData == null ? null : gridData.get(key);
      if (value != null && !value.isNull()) {
        parameters.set(key, value);
      }
    }
  }

  /**
   * Fill grid data
   *
   * @param reportGrid     Report grid
   * @param visibleColumns Visible columns
   */
  private List<String> setGridColumns(ReportGrid reportGrid, List<PrintColumnData> visibleColumns, boolean includeSpreadsheetOnlyColumns, GridPrintContext context) {
    List<String> fieldList = new ArrayList<>();

    visibleColumns.forEach(columnData -> {
      if (columnData.isHeader()) {
        List<PrintColumnData> headerColumns = columnData.getColumnList().stream()
          .filter(column -> isPrinted(column, includeSpreadsheetOnlyColumns))
          .toList();
        if (!headerColumns.isEmpty()) {
          ReportHeader reportHeader = new ReportHeader(columnData.getLabel())
            .setLabel(columnData.getLabel());
          headerColumns.forEach(column -> getReportColumns(column, fieldList, context).forEach(reportHeader::addColumn));
          reportGrid.addGridHeader(reportHeader);
        }
      } else if (isPrinted(columnData, includeSpreadsheetOnlyColumns)) {
        getReportColumns(columnData, fieldList, context).forEach(reportGrid::addGridHeader);
      }
    });

    // Fields read from the data that are not a column of the report: secondary lines and status tones
    context.extraFields().stream().filter(field -> !fieldList.contains(field)).forEach(fieldList::add);

    return fieldList;
  }

  /**
   * Generate the report columns of a print column: the column itself and its secondary line. Documents print the
   * secondary line under the value in the same cell when the column is a text; otherwise (and in spreadsheets, only when
   * the column exports its secondary field) it goes to a column right after it, titled with the secondary label or else
   * the column label
   *
   * @param column    Column
   * @param fieldList Grid field list
   * @param context   Print context of the grid
   * @return Report columns
   */
  private List<ReportColumn> getReportColumns(PrintColumnData column, List<String> fieldList, GridPrintContext context) {
    List<ReportColumn> reportColumns = new ArrayList<>();
    ReportColumn mainColumn = getReportColumn(column, fieldList, context);
    reportColumns.add(mainColumn);
    String secondaryField = column.getSecondaryField();
    if (secondaryField != null && !secondaryField.isEmpty()) {
      boolean inCell = !context.spreadsheet() && isTextColumn(mainColumn);
      boolean ownColumn = context.spreadsheet() ? Boolean.TRUE.equals(column.getExportSecondary()) : !inCell;
      if (inCell) {
        mainColumn.setSecondaryField(secondaryField);
        context.extraFields().add(secondaryField);
      } else if (ownColumn) {
        PrintColumnData secondaryColumn = new PrintColumnData()
          .setName(secondaryField)
          .setLabel(Optional.ofNullable(column.getSecondaryLabel()).filter(label -> !label.isEmpty()).orElse(column.getLabel()))
          .setWidth(column.getWidth())
          .setCharlength(column.getCharlength())
          .setAlign(column.getAlign());
        reportColumns.add(getReportColumn(secondaryColumn, fieldList, context));
      }
    }
    return reportColumns;
  }

  /**
   * Check whether a column prints text, so ADE can print a secondary line under its value (its type is unknown until the
   * data is read when the screen does not declare it: it is taken as a text)
   *
   * @param reportColumn Report column
   * @return Column prints text
   */
  private boolean isTextColumn(ReportColumn reportColumn) {
    return reportColumn.getType() == null || reportColumn.getType() == ColumnType.STRING;
  }

  /**
   * Generate report column and retrieve it
   *
   * @param column    Column
   * @param fieldList Grid field list
   * @return Report column
   */
  private ReportColumn getReportColumn(PrintColumnData column, List<String> fieldList, GridPrintContext context) {
    ReportColumn reportColumn = new ReportColumn(column.getName())
      .setLabel(column.getLabel())
      .setField(column.getName())
      .setWidth(getColumnWidth(column));
    HorizontalTextAlignment alignment = getColumnAlignment(column);
    if (alignment != null) {
      reportColumn.setAlign(alignment);
    }
    if (column.getType() != null) {
      reportColumn.setType(ColumnType.valueOf(column.getType().toUpperCase()));
    }
    if (column.getComponent() != null &&
      ("icon".equalsIgnoreCase(column.getComponent()) || "image".equalsIgnoreCase(column.getComponent()))) {
      reportColumn.setType(ColumnType.valueOf(column.getComponent().toUpperCase()));
    }
    applyCellFeatures(reportColumn, context.definition(column.getName()), context);
    fieldList.add(column.getName());
    return reportColumn;
  }

  /**
   * Retrieve the column definitions of a grid by name
   *
   * @param grid Grid element
   * @return Column definitions
   */
  private Map<String, Column> getColumnDefinitions(Grid grid) {
    Map<String, Column> definitions = new LinkedHashMap<>();
    grid.getElementsByType(Column.class).forEach(column -> definitions.putIfAbsent(column.getName(), column));
    return definitions;
  }

  /**
   * Set the column type and options of the cell features declared in the column definition: status, chips, progress
   * bar, sparkline and formatted text. They are read from the screen definition, not from what the client sends
   *
   * @param reportColumn Report column
   * @param definition   Column definition, null when the screen has none
   * @param context      Print context of the grid
   */
  private void applyCellFeatures(ReportColumn reportColumn, Column definition, GridPrintContext context) {
    if (definition == null) {
      return;
    }
    String field = reportColumn.getField();
    String component = definition.getComponentType() == null ? "" : definition.getComponentType().toLowerCase(Locale.ROOT);
    String statusField = definition.getStatusField();
    boolean hasStatus = statusField != null && !statusField.isBlank();
    if (component.isBlank() && (Boolean.TRUE.equals(definition.getChips()) || hasStatus)) {
      reportColumn.setType(ColumnType.CHIP);
      PrintPalette.chipTones().forEach(reportColumn::addChipTone);
      if (hasStatus) {
        context.extraFields().add(statusField);
      }
      if (Boolean.TRUE.equals(definition.getChips())) {
        context.printer().addChips(field, definition.getChipSeparator(), statusField);
      } else {
        context.printer().addStatus(field, statusField);
      }
    } else if ("progress".equals(component)) {
      reportColumn.setType(ColumnType.PROGRESS);
      context.printer().addProgress(field, definition.getBarMin(), definition.getBarMax());
    } else if ("sparkline".equals(component)) {
      reportColumn.setType(ColumnType.SPARKLINE).setSparklineType(getSparklineType(definition.getSparklineType()));
      Optional.ofNullable(PrintPalette.hexColor(definition.getSparklineColor())).ifPresent(reportColumn::setSparklineColor);
      Optional.ofNullable(PrintPalette.hexColor(definition.getSparklineNegativeColor())).ifPresent(reportColumn::setSparklineNegativeColor);
      context.printer().addSparkline(field);
    } else if ("formatted-text".equals(component)) {
      context.printer().addFormattedText(field);
    }
  }

  /**
   * Get the sparkline type of a column: line, bar or area (any other value is a line, as in the clients)
   *
   * @param type Declared type
   * @return Sparkline type
   */
  private SparklineType getSparklineType(String type) {
    try {
      return type == null ? SparklineType.LINE : SparklineType.valueOf(type.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exc) {
      return SparklineType.LINE;
    }
  }

  /**
   * Get column width
   *
   * @param column Column
   * @return Column width in pixels, or null when neither charlength nor width are defined
   */
  private Integer getColumnWidth(PrintColumnData column) {
    Integer columnWidth = null;
    if (column.getCharlength() != null) {
      columnWidth = column.getCharlength() * baseConfigProperties.getComponent().getGridPixelsPerCharacter();
    } else if (column.getWidth() != null) {
      columnWidth = column.getWidth().equalsIgnoreCase("*") ? 300 : Integer.parseInt(column.getWidth());
    }
    return columnWidth;
  }

  /**
   * Get column alignment
   *
   * @param column Column
   * @return Column horizontal alignment, or null when not defined
   */
  private HorizontalTextAlignment getColumnAlignment(PrintColumnData column) {
    return toAlignment(column.getAlign());
  }

  /**
   * Parse a column alignment
   *
   * @param align Align attribute
   * @return Horizontal alignment, or null (the default alignment) when it is not defined or not known
   */
  private static HorizontalTextAlignment toAlignment(String align) {
    if (align == null || align.isBlank()) {
      return null;
    }
    try {
      return HorizontalTextAlignment.valueOf(align.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exc) {
      log.debug("Print design: unknown column align '{}', the default alignment applies", align);
      return null;
    }
  }

  /**
   * Fill grid data
   *
   * @param grid       Grid element
   * @param reportGrid Report grid
   * @param parameters Parameters
   */
  private void setGridData(Grid grid, ReportGrid reportGrid, List<String> fields, ObjectNode parameters, GridPrintContext context) throws AWException {
    // Retrieve columns
    reportGrid.setFields(fields);

    if (grid.isLoadAll() || grid.isEditable() || grid.isMultioperation()) {
      // Get data from parameters
      reportGrid.setData(getGridDataParameters(grid, reportGrid, fields, parameters, context));
    } else {
      // Get data from query
      reportGrid.setData(getGridDataQuery(grid, reportGrid, fields, parameters, context));
    }

    // Row detail: its own hidden field, with the detail of each data row (the totals have none)
    if (context.rowDetail() != null) {
      fields.add(RowDetailSpec.FIELD);
      List<List<Object>> rows = reportGrid.getData();
      for (int index = 0; index < rows.size(); index++) {
        rows.get(index).add(index < context.detailCells().size() ? context.detailCells().get(index) : null);
      }
      reportGrid.setFields(fields);
      reportGrid.setRowDetailField(RowDetailSpec.FIELD);
      if (context.rowDetail().isGrid()) {
        reportGrid.setRowDetailGrid(context.rowDetail().nestedTemplate());
      }
    }
  }

  /**
   * Retrieve the row detail a grid prints under each row: its text template or its nested grid. Null when the grid does
   * not ask for it (`print="true"`) or the server cannot resolve it (a detail made through an include)
   *
   * @param grid        Grid
   * @param spreadsheet The report is a spreadsheet
   * @return Row detail to print
   */
  private RowDetailSpec createRowDetailSpec(Grid grid, boolean spreadsheet) {
    RowDetail rowDetail = grid.getRowDetail();
    if (rowDetail == null || !rowDetail.isPrint()) {
      return null;
    }
    if (!rowDetail.isComponentMode()) {
      String template = rowDetail.getContent();
      return template == null || template.isBlank() ? null : new RowDetailSpec(template, null, null, List.of(), 0);
    }
    Grid nested = rowDetail.getDetailGrid();
    if (rowDetail.getDefinitionError() != null || nested == null || nested.getTargetAction() == null) {
      log.debug("Print design: the row detail of grid '{}' is not printed, the server cannot resolve its nested grid", grid.getId());
      return null;
    }
    ReportGrid template = new ReportGrid(nested.getId());
    List<String> nestedFields = new ArrayList<>();
    nested.getElementsByType(Column.class).stream()
      .filter(column -> isDetailColumnPrinted(column, spreadsheet))
      .forEach(column -> {
        ReportColumn reportColumn = new ReportColumn(column.getName())
          .setLabel(getLocale(column.getLabel() == null ? column.getName() : column.getLabel()))
          .setField(column.getName());
        if (column.getCharLength() != null) {
          reportColumn.setWidth(column.getCharLength() * baseConfigProperties.getComponent().getGridPixelsPerCharacter());
        }
        reportColumn.setAlign(toAlignment(column.getAlign()));
        template.addGridHeader(reportColumn);
        nestedFields.add(column.getName());
      });
    if (nestedFields.isEmpty()) {
      return null;
    }
    template.setFields(nestedFields);
    int max = nested.getMax() != null && nested.getMax() > 0 ? nested.getMax() : MAX_DETAIL_ROWS;
    return new RowDetailSpec(null, nested, template, nestedFields, max);
  }

  /**
   * Check whether a column of a nested row detail grid is printed, with the rules of the columns of a grid
   *
   * @param column      Column
   * @param spreadsheet The report is a spreadsheet
   * @return Column is printed
   */
  private boolean isDetailColumnPrinted(Column column, boolean spreadsheet) {
    String printMode = column.getPrintMode();
    boolean action = ACTION_COLUMN_COMPONENT.equalsIgnoreCase(column.getComponentType());
    boolean forced = "true".equalsIgnoreCase(printMode) || (spreadsheet && PRINTABLE_SPREADSHEET_ONLY.equalsIgnoreCase(printMode));
    boolean blocked = NOT_PRINTABLE.equalsIgnoreCase(printMode) || (!spreadsheet && PRINTABLE_SPREADSHEET_ONLY.equalsIgnoreCase(printMode));
    return !action && !blocked && (forced || !column.isHidden());
  }

  /**
   * Fields of a row of the grid, as the row detail reads them
   *
   * @param texts  Text of each field (its label, or else its value)
   * @param values Value of each field
   */
  private record DetailRow(Map<String, String> texts, Map<String, JsonNode> values) {
  }

  /**
   * Names of the fields the client sent with their data (one cell per row)
   *
   * @param parameters Parameters
   * @return Field names
   */
  private List<String> getSentFields(ObjectNode parameters) {
    String suffix = baseConfigProperties.getComponent().getDataSuffix();
    List<String> names = new ArrayList<>();
    parameters.properties().forEach(entry -> {
      if (entry.getKey().endsWith(suffix) && entry.getValue().isArray()) {
        names.add(entry.getKey().substring(0, entry.getKey().length() - suffix.length()));
      }
    });
    return names;
  }

  /**
   * Fields of a row sent by the client
   *
   * @param parameters Parameters
   * @param sentFields Names of the fields sent
   * @param rowIndex   Row index
   * @return Fields of the row
   */
  private DetailRow getDetailRow(ObjectNode parameters, List<String> sentFields, int rowIndex) {
    Map<String, String> texts = new LinkedHashMap<>();
    Map<String, JsonNode> values = new LinkedHashMap<>();
    for (String field : sentFields) {
      JsonNode cell = parameters.get(field + baseConfigProperties.getComponent().getDataSuffix()).get(rowIndex);
      JsonNode value = cell == null ? null : cell.get(AweConstants.JSON_VALUE_PARAMETER);
      JsonNode label = cell == null ? null : cell.get(AweConstants.JSON_LABEL_PARAMETER);
      JsonNode text = label != null && !label.isNull() ? label : value;
      texts.put(field, text == null || text.isNull() ? "" : text.asString(""));
      values.put(field, value == null ? JsonNodeFactory.instance.nullNode() : value);
    }
    return new DetailRow(texts, values);
  }

  /**
   * Fields of a row of a query
   *
   * @param row Query row
   * @return Fields of the row
   */
  private DetailRow getDetailRow(Map<String, CellData> row) {
    Map<String, String> texts = new LinkedHashMap<>();
    Map<String, JsonNode> values = new LinkedHashMap<>();
    row.forEach((field, cell) -> {
      texts.put(field, cell.getStringValue());
      Object value = cell.getValue();
      values.put(field, value instanceof Number || value instanceof Boolean ? mapper.valueToTree(value)
        : JsonNodeFactory.instance.stringNode(cell.getStringValue()));
    });
    return new DetailRow(texts, values);
  }

  /**
   * Detail of a row: its template rendered with the row, or the rows of its nested grid
   *
   * @param grid       Grid that prints
   * @param spec       Row detail
   * @param row        Fields of the row
   * @param parameters Parameters of the print
   * @return Text of the detail, or the rows of the nested table
   */
  private Object getRowDetail(Grid grid, RowDetailSpec spec, DetailRow row, ObjectNode parameters) throws AWException {
    if (!spec.isGrid()) {
      return RowDetailText.render(spec.template(), field -> row.texts().getOrDefault(field, ""));
    }

    // The request of the nested grid: the screen values and the row of its parent, as the clients send it
    ObjectNode request = JsonNodeFactory.instance.objectNode();
    request.setAll(parameters);
    List.of(AweConstants.COMPONENT_SORT, AweConstants.COMPONENT_COLUMN_FILTERS).forEach(request::remove);
    row.values().forEach((field, value) -> request.set(grid.getId() + "." + AweConstants.ADDRESS_ROW + "." + field, value));
    request.put(AweConstants.COMPONENT_MAX, spec.maxRows());
    request.put(AweConstants.COMPONENT_PAGE, 1);

    List<List<Object>> nestedRows = new ArrayList<>();
    DataList nestedData = queryService.launchPrivateQuery(spec.nestedGrid().getTargetAction(), request).getDataList();
    for (Map<String, CellData> nestedRow : nestedData.getRows().stream().limit(spec.maxRows()).toList()) {
      List<Object> rowData = new ArrayList<>();
      nestedRows.add(rowData);
      for (String field : spec.nestedFields()) {
        updateColumnType(spec.nestedTemplate(), field, addRowData(nestedRow.get(field), rowData));
      }
    }
    return nestedRows;
  }

  /**
   * Values of the first field, which give the number of rows sent
   *
   * @param fields     Fields
   * @param parameters Parameters
   * @return Values of the first field, empty when there is none
   */
  private static ArrayNode getFirstFieldData(List<String> fields, ObjectNode parameters) {
    JsonNode firstField = fields.isEmpty() ? null : parameters.get(fields.get(0));
    return firstField != null && firstField.isArray() ? (ArrayNode) firstField : JsonNodeFactory.instance.arrayNode();
  }

  /**
   * Set report data from parameters
   *
   * @param reportGrid Report grid
   * @param fields     Fields
   * @param parameters Parameters
   * @return report data
   */
  private List<List<Object>> getGridDataParameters(Grid grid, ReportGrid reportGrid, List<String> fields, ObjectNode parameters, GridPrintContext context) throws AWException {
    List<List<Object>> data = new ArrayList<>();
    boolean firstRow = true;
    ArrayNode firstFieldData = getFirstFieldData(fields, parameters);

    // Add _style_ to fields if it exists
    if (parameters.has(AweConstants.DATALIST_STYLE_FIELD) || grid.isShowTotals() || !context.groupLevels().isEmpty()) {
      addStyleField(reportGrid, fields);
    }

    // Fields the client sent for each row, where the row detail reads its row
    List<String> sentFields = context.rowDetail() == null ? List.of() : getSentFields(parameters);

    // Retrieve data from parameters
    for (int rowIndex = 0, rowTotal = firstFieldData.size(); rowIndex < rowTotal; rowIndex++) {
      List<Object> rowData = new ArrayList<>();
      data.add(rowData);
      if (context.rowDetail() != null) {
        context.detailCells().add(getRowDetail(grid, context.rowDetail(), getDetailRow(parameters, sentFields, rowIndex), parameters));
      }
      for (String field : fields) {
        ArrayNode fieldData = (ArrayNode) Optional.ofNullable(parameters.get(field + baseConfigProperties.getComponent().getDataSuffix())).orElse(JsonNodeFactory.instance.arrayNode());
        ColumnType type = storeParameterRowData(fieldData.get(rowIndex), rowData);

        // Update column type on first row
        if (firstRow) {
          updateColumnType(reportGrid, field, type);
        }
      }
      firstRow = false;
    }

    // Group the rows from the raw cells
    GridGrouper grouper = createGrouper(reportGrid, fields, context);
    List<List<Object>> rows = grouper == null ? data : grouper.group(data);

    // Retrieve footer row if exists
    if (grid.isShowTotals()) {
      ObjectNode totalRows = (ObjectNode) parameters.get(grid.getId() + baseConfigProperties.getComponent().getDataSuffix()).get("footer");
      totalRows.set(AweConstants.DATALIST_STYLE_FIELD, JsonNodeFactory.instance.objectNode().put(AweConstants.JSON_VALUE_PARAMETER, TotalizeStyleType.TOTAL.toString()));
      List<Object> footerData = new ArrayList<>();
      rows.add(footerData);
      fields.forEach(field -> storeParameterRowData(totalRows.get(field), footerData));
    }

    // Draw the cell features of every row, so group and total rows leave a drawn cell without value blank
    context.printer().apply(fields, rows);
    return rows;
  }

  /**
   * Set report data from a query
   *
   * @param grid       Grid
   * @param reportGrid Report grid
   * @param fields     Fields
   * @param parameters Parameter
   * @return report data
   */
  private List<List<Object>> getGridDataQuery(Grid grid, ReportGrid reportGrid, List<String> fields, ObjectNode parameters, GridPrintContext context) throws AWException {
    List<List<Object>> data = new ArrayList<>();
    boolean firstRow = true;

    // Set sort, page size and current page from the grid
    addGridPaginationData(grid, parameters);

    // Launch query and get data
    DataList queryData = queryService.launchPrivateQuery(grid.getTargetAction(), parameters).getDataList();

    // Add _style_ to fields if it exists
    if (!queryData.getRows().isEmpty()) {
      Map<String, CellData> row = queryData.getRows().get(0);
      if (row.containsKey(AweConstants.DATALIST_STYLE_FIELD)) {
        addStyleField(reportGrid, fields);
      }
    }

    // Retrieve data from parameters
    for (Map<String, CellData> row : queryData.getRows()) {
      List<Object> rowData = new ArrayList<>();
      data.add(rowData);
      if (context.rowDetail() != null) {
        context.detailCells().add(getRowDetail(grid, context.rowDetail(), getDetailRow(row), parameters));
      }
      for (String field : fields) {
        CellData cell = row.get(field);
        ColumnType type = addRowData(cell, rowData);

        // Update column type on first row
        if (firstRow) {
          updateColumnType(reportGrid, field, type);
        }
      }
      firstRow = false;
    }

    // A groupable grid always loads all its rows, so a grid loaded by query is never grouped
    context.printer().apply(fields, data);
    return data;
  }

  /**
   * Retrieve the columns that group the rows of a grid in the report: the levels the client sends (the grouping the user
   * has chosen, which may be none) or, when it sends none, the group-by of the grid. Grids that cannot group (the ones the
   * clients do not group either) and columns the grid does not have are ignored
   *
   * @param grid        Grid
   * @param parameters  Parameters
   * @param definitions Column definitions of the grid by name
   * @return Group levels, in order
   */
  private List<String> getGroupLevels(Grid grid, ObjectNode parameters, Map<String, Column> definitions) {
    boolean groupable = grid.isGroupable() && !grid.isEditable() && !grid.isMultioperation() && !grid.isMoveRows()
      && grid.getRowDetail() == null;
    if (!groupable) {
      return List.of();
    }
    JsonNode gridData = parameters.get(grid.getId() + baseConfigProperties.getComponent().getDataSuffix());
    JsonNode sent = gridData == null ? null : gridData.get(AweConstants.JSON_GROUP_BY);
    List<String> names = new ArrayList<>();
    if (sent != null && sent.isArray()) {
      sent.forEach(name -> names.add(name.asString("")));
    } else if (sent != null && sent.isString()) {
      names.addAll(Arrays.asList(sent.asString("").split(",")));
    } else if (grid.getGroupBy() != null) {
      names.addAll(Arrays.asList(grid.getGroupBy().split(",")));
    }
    return names.stream().map(String::trim).filter(definitions::containsKey).distinct().toList();
  }

  /**
   * Create the grouper of a grid, null when its rows are not grouped
   *
   * @param reportGrid Report grid
   * @param fields     Fields of the data
   * @param context    Print context of the grid
   * @return Grouper
   */
  private GridGrouper createGrouper(ReportGrid reportGrid, List<String> fields, GridPrintContext context) {
    if (context.groupLevels().isEmpty()) {
      return null;
    }
    List<ReportColumn> printed = getPrintedColumns(reportGrid);
    Map<String, String> summaries = new LinkedHashMap<>();
    printed.forEach(column -> Optional.ofNullable(context.definition(column.getField()))
      .map(Column::getSummaryType)
      .filter(GridGrouper::isAggregate)
      .ifPresent(summaryType -> summaries.put(column.getField(), summaryType)));
    // The label of a group goes in the first column that is plain text; a grid without one prints its groups unlabelled,
    // so no text lands in a number or date cell
    String labelField = printed.stream()
      .filter(this::isTextColumn)
      .map(ReportColumn::getField)
      .filter(field -> !context.printer().handles(field))
      .findFirst().orElse(null);
    addGroupRowStyles(reportGrid, context.groupLevels().size());
    return new GridGrouper(new GridGrouper.Spec(context.groupLevels(), summaries, labelField,
      context.subtotals(), getLocale("GRID_GROUP_EMPTY"), getLocale("GRID_GROUP_SUBTOTAL"),
      field -> printed.stream().filter(column -> field.equals(column.getField())).map(ReportColumn::getType).findFirst().orElse(null)), fields);
  }

  /**
   * Retrieve the columns of a report grid, including the ones inside a header
   *
   * @param reportGrid Report grid
   * @return Report columns
   */
  private List<ReportColumn> getPrintedColumns(ReportGrid reportGrid) {
    List<ReportColumn> columns = new ArrayList<>();
    for (GridHeader header : reportGrid.getGridHeaders()) {
      if (header instanceof ReportColumn column) {
        columns.add(column);
      } else if (header instanceof ReportHeader reportHeader) {
        columns.addAll(reportHeader.getColumns());
      }
    }
    return columns;
  }

  /**
   * Style the group rows: bold, on a background that gets lighter with the level (the surface tokens of the design system)
   *
   * @param reportGrid Report grid
   * @param levels     Number of group levels
   */
  private void addGroupRowStyles(ReportGrid reportGrid, int levels) {
    for (int level = 1; level <= levels; level++) {
      String background = switch (level) {
        case 1 -> PrintPalette.GROUP_BACKGROUND_LEVEL_1;
        case 2 -> PrintPalette.GROUP_BACKGROUND_LEVEL_2;
        default -> PrintPalette.GROUP_BACKGROUND_DEEPER;
      };
      reportGrid.addRowStyle(GridGrouper.GROUP_STYLE_PREFIX + level, new AdeStyle().bold().setBackgroundColor(Color.decode(background)));
    }
  }

  /**
   * Add style field to report grid: ADE styles each row (group rows, subtotals and the total) by its label, without
   * printing it
   *
   * @param reportGrid Report grid
   * @param fields     Fields
   */
  private void addStyleField(ReportGrid reportGrid, List<String> fields) {
    reportGrid.setStyleColumn(new ReportColumn("style-field")
      .setField(AweConstants.DATALIST_STYLE_FIELD)
      .setType(ColumnType.STRING));
    fields.add(AweConstants.DATALIST_STYLE_FIELD);
  }

  /**
   * Add row data and retrieve data type
   *
   * @param cell    Cell data
   * @param rowData Row data
   * @return Column type
   */
  private ColumnType addRowData(CellData cell, List<Object> rowData) {
    ColumnType type;
    CellData safeCell = Optional.ofNullable(cell).orElse(new CellData());
    switch (safeCell.getType()) {
      case OBJECT:
      case JSON:
        rowData.add(new DataBean(toValueMap((ObjectNode) safeCell.getObjectValue())));
        type = ColumnType.OBJECT;
        break;
      case NULL:
        rowData.add(new DataBean()
          .add(AweConstants.JSON_VALUE_PARAMETER, "")
          .add(AweConstants.JSON_LABEL_PARAMETER, ""));
        type = ColumnType.STRING;
        break;
      case FLOAT:
      case DOUBLE:
      case DECIMAL:
        rowData.add(new DataBean()
          .add(AweConstants.JSON_VALUE_PARAMETER, safeCell.getDoubleValue())
          .add(AweConstants.JSON_LABEL_PARAMETER, safeCell.getStringValue()));
        type = ColumnType.DOUBLE;
        break;
      case INTEGER:
      case LONG:
        rowData.add(new DataBean()
          .add(AweConstants.JSON_VALUE_PARAMETER, safeCell.getIntegerValue())
          .add(AweConstants.JSON_LABEL_PARAMETER, safeCell.getStringValue()));
        type = ColumnType.INTEGER;
        break;
      default:
        rowData.add(new DataBean()
          .add(AweConstants.JSON_VALUE_PARAMETER, safeCell.getStringValue())
          .add(AweConstants.JSON_LABEL_PARAMETER, safeCell.getStringValue()));
        type = ColumnType.valueOf(safeCell.getType().toString());
        break;
    }

    return type;
  }

  /**
   * Update column type on data retrieval
   *
   * @param reportGrid Report grid
   * @param field      Field to update
   * @param type       Type
   */
  private void updateColumnType(ReportGrid reportGrid, String field, ColumnType type) {
    for (GridHeader gridHeader : reportGrid.getGridHeaders()) {
      if (gridHeader instanceof ReportColumn column1) {
        updateType(column1, field, type);
      } else if (gridHeader instanceof ReportHeader header) {
        for (ReportColumn column : header.getColumns()) {
          updateType(column, field, type);
        }
      }
    }
  }

  /**
   * Update a column type
   *
   * @param column Report column
   * @param field  Field to update
   * @param type   Type
   */
  private void updateType(ReportColumn column, String field, ColumnType type) {
    if (field.equalsIgnoreCase(column.getField()) && column.getType() == null) {
      column.setType(type);
    }
  }

  /**
   * Store parameter data
   *
   * @param data Parameter data
   * @param row  Row
   */
  private ColumnType storeParameterRowData(JsonNode data, List<Object> row) {
    ColumnType type;
    JsonNode emptyData = JsonNodeFactory.instance.objectNode()
      .put(AweConstants.JSON_LABEL_PARAMETER, "")
      .putNull(AweConstants.JSON_VALUE_PARAMETER);
    JsonNode safeData = Optional.ofNullable(data).orElse(emptyData);
    JsonNode value = safeData.get(AweConstants.JSON_VALUE_PARAMETER);
    if (value.isDouble()) {
      type = ColumnType.DOUBLE;
    } else if (value.isFloat()) {
      type = ColumnType.FLOAT;
    } else if (value.isLong()) {
      type = ColumnType.LONG;
    } else if (value.isInt()) {
      type = ColumnType.INTEGER;
    } else if (value.isBoolean()) {
      type = ColumnType.BOOLEAN;
    } else if (value.isBigDecimal()) {
      type = ColumnType.BIGDECIMAL;
    } else if (value.isBigInteger()) {
      type = ColumnType.BIGINTEGER;
    } else {
      type = ColumnType.STRING;
    }
    row.add(new DataBean(toValueMap((ObjectNode) safeData)));
    return type;
  }

  /**
   * Convert a Jackson node into the plain value map ADE 3 beans consume
   *
   * @param node JSON node
   * @return value map
   */
  private Map<String, Object> toValueMap(ObjectNode node) {
    return mapper.convertValue(node, new TypeReference<Map<String, Object>>() {
    });
  }

  /**
   * Get element design for chart
   *
   * @param printBean  Print bean
   * @param element    Element
   * @param parameters Parameters
   */
  private void getElementDesign(PrintBean printBean, Chart element, ObjectNode parameters) {
    Layout layout = (Layout) printBean.getDetail();

    // Add chart element to layout
    Image chartElement = new Image(element.getId());
    Layout chartLayout = Elements.layout(element.getId() + "-layout", LayoutType.VERTICAL);
    chartLayout.addElement(chartElement)
      .getStyle()
      .setTopPadding(TOP_PADDING_SMALL);
    layout.addElement(chartLayout);

    // Get chart data
    String image = parameters.get(element.getId()).get(AweConstants.JSON_IMAGE_PARAMETER).asString("");

    // Configure chart
    chartElement.setSVGImage(image)
      .setSize(Image.Size.FULL_SIZE.getSize())
      .setScale(AweConstants.SVG_SCALE)
      .getStyle()
      .setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);

    // Set as landscape if defined
    if (PageOrientation.LANDSCAPE.equals(printBean.getOrientation())) {
      chartElement
        .setSize(Image.Size.FULL_SIZE_LANDSCAPE.getSize())
        .setScale(AweConstants.SVG_SCALE_LANDSCAPE);
    }
  }

  /**
   * Get element design for standard element (new page)
   *
   * @param printBean Print bean
   * @param element   Element
   * @return Print bean
   */
  private PrintBean getElementDesign(PrintBean printBean, Element element) {
    Layout layout = (Layout) printBean.getDetail();
    layout.addElement(new PageBreak(element.getId()));

    // Return print bean
    return printBean;
  }
}
