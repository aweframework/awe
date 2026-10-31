import {aweApplication} from "../../awe";
import "angular-ui-grid";
import "./components";
import "./editable";
import "./multioperation";
import "./events";
import "./layout";
import {getColumnHeaders, isActionColumn} from "./layoutRules";
import {getDensityRowHeight, getExplicitRowHeight, getGridRowHeight, observeDensity} from "./density";
import {AVATAR_TONES, getInitials, STATUS_TONES, toTone} from "./initials";
import {getSwatchColor} from "./swatch";
import {getIconTemplate} from "../component";

// Add requirements
aweApplication.requires.push.apply(aweApplication.requires, ["ui.grid", "ui.grid.resizeColumns", "ui.grid.pinning", "ui.grid.selection", "ui.grid.pagination", "ui.grid.moveColumns", "ui.grid.treeView"]);

// Grid commons service
aweApplication.factory('GridCommons', ['GridComponents', 'GridEditable', 'GridMultioperation', 'GridEvents', 'GridLayout', '$translate', 'AweSettings', 'Control', 'AweUtilities', '$log',
  /**
   * Grid common methods
   *
   * @param {service} GridComponents Grid components
   * @param {service} GridEditable Grid editable
   * @param {service} GridMultioperation Grid multioperation
   * @param {service} GridEvents Grid events
   * @param {service} GridLayout Grid column layout
   * @param {service} $translate Translate service
   * @param {service} $settings AWE $settings
   * @param {service} Control Control service
   * @param {service} Utilities AWE Utilities
   * @param {object} $log
   */
  function (GridComponents, GridEditable, GridMultioperation, GridEvents, GridLayout, $translate, $settings, Control, Utilities, $log) {
    // Retrieve $settings

    // Keys that move the selected row of an editable grid (or one with move-rows) while the Alt key is held
    const MOVE_ROW_KEYS = {ArrowUp: "before", ArrowDown: "after", Home: "first", End: "last"};

    // Selector of the cells whose text is cut by their maximum number of lines
    const CLAMPED_CELL = ".cell-clamp";

    /**
     * Retrieve how many lines the text of a column wraps over. A column wraps when it declares wrap, or a maximum
     * number of lines (which implies it) unless wrap is explicitly false.
     *
     * @param {object} column Column model
     * @returns {{wrap: boolean, maxLines: (number|null)}} Wrap flag and maximum number of lines (null when unlimited)
     */
    const getColumnWrap = function (column) {
      const maxLines = parseInt(column.maxLines, 10);
      const limit = maxLines > 0 ? maxLines : null;
      if (column.wrap === false || (column.wrap !== true && limit === null)) {
        return {wrap: false, maxLines: null};
      }
      return {wrap: true, maxLines: limit};
    };

    /**
     * Cell template of a text column that wraps over several lines
     *
     * @param {{wrap: boolean, maxLines: (number|null)}} wrapping Column wrapping
     * @returns {string} Cell template
     */
    const getWrapCellTemplate = function (wrapping) {
      const clamp = wrapping.maxLines === null ? "" : " cell-clamp\" style=\"--cell-max-lines: " + wrapping.maxLines;
      return "<div class=\"no-animate ui-grid-cell-contents cell-wrap" + clamp + " {{col.grid.appScope.component.getCellStyle(COL_FIELD)}}\"" +
        " column-id=\"{{col.name}}\" data-testid=\"grid-cell\" ng-bind-html=\"col.grid.appScope.component.getCellValue(COL_FIELD)\"></div>";
    };

    // Component expression of the cell templates
    const CELL_COMPONENT = "col.grid.appScope.component";

    /**
     * Check whether a column without component shows its value as a list of chips
     *
     * @param {object} column Column model
     * @returns {boolean} Column shows chips
     */
    const hasChips = (column) => !("component" in column) && column.chips === true;

    /**
     * Check whether a column without component shows a secondary line under its value
     *
     * @param {object} column Column model
     * @returns {boolean} Column shows a secondary line
     */
    const hasSecondary = (column) => !("component" in column) && !!column.secondaryField;

    /**
     * Check whether a column without component shows its value as a status (ignored in a column of chips)
     *
     * @param {object} column Column model
     * @returns {boolean} Column shows a status
     */
    const hasStatus = (column) => !("component" in column) && !!column.statusField && !hasChips(column);

    /**
     * Check whether a column without component shows an avatar before its value
     *
     * @param {object} column Column model
     * @returns {boolean} Column shows an avatar
     */
    const hasAvatar = (column) => !("component" in column) && !!column.avatarField;

    /**
     * Check whether a column without component shows a colour swatch before its value (in the place of the avatar)
     *
     * @param {object} column Column model
     * @returns {boolean} Column shows a swatch
     */
    const hasSwatch = (column) => !("component" in column) && !!column.swatchField;

    /**
     * Check whether a column without component shows an icon before its value (ignored in a column of chips)
     *
     * @param {object} column Column model
     * @returns {boolean} Column shows an icon
     */
    const hasIcon = (column) => !("component" in column) && !!column.iconField && !hasChips(column);

    /**
     * Template of the icon of a cell: the icon of the icon field of the row, decorative (the text is the value of the
     * cell); nothing when the row has no icon or an unknown one
     *
     * @returns {string} Template of the icon
     */
    const getCellIconTemplate = () => {
      const icon = CELL_COMPONENT + ".getCellIcon(row.entity, col.colDef)";
      // Without the spaces between its alternatives, which would add a space between the icon and the text
      return getIconTemplate("awe-cell-icon", "span", icon, "aria-hidden=\"true\"").replace(/>\s+</g, "><");
    };

    /**
     * Check whether the cells of a column need more than one line (secondary line, chips, avatar or swatch), so the rows
     * grow to fit them
     *
     * @param {object} column Column model
     * @returns {boolean} Cells of the column grow
     */
    const growsCell = (column) => hasSecondary(column) || hasChips(column) || hasAvatar(column) || hasSwatch(column);

    /**
     * Contents of the cell of a column with a secondary line, a status, chips, an avatar, a swatch or an icon. The values are always
     * bound as text. With an icon the status is a plain tag: the icon of the row (if any) takes the place of the dot.
     *
     * @param {object} column Column model
     * @returns {string} Contents of the cell
     */
    const getRichCellContents = function (column) {
      const value = CELL_COMPONENT + ".getCellValue(COL_FIELD)";
      const main = getMainCellContents(column, value);
      let text = main;
      if (hasSecondary(column)) {
        const secondary = CELL_COMPONENT + ".getCellSecondary(row.entity, col.colDef)";
        const mainLine = main === null ? "<span class=\"awe-cell-main\" ng-bind=\"" + value + "\"></span>" : "<span class=\"awe-cell-main\">" + main + "</span>";
        text = "<div class=\"awe-cell-lines\">" + mainLine + "<span class=\"awe-cell-secondary\" ng-if=\"" + secondary + "\" ng-bind=\"" + secondary + "\"></span></div>";
      }
      if (!hasAvatar(column) && !hasSwatch(column)) {
        return text;
      }
      return getEntityCellContents(column, text === null ? "<span class=\"awe-cell-main\" ng-bind=\"" + value + "\"></span>" : text);
    };

    /**
     * Main contents of a rich cell: its chips, its status (a plain tag with the icon of the row when it has one) or its
     * icon with the text (null when the column has none of them)
     *
     * @param {object} column Column model
     * @param {string} value Expression of the cell value
     * @returns {string|null} Main contents of the cell
     */
    const getMainCellContents = function (column, value) {
      const tone = "{{" + CELL_COMPONENT + ".getCellTone(row.entity[col.colDef.statusField])}}";
      if (hasChips(column)) {
        const chips = CELL_COMPONENT + ".getCellChips(COL_FIELD, col.colDef)";
        return "<span class=\"awe-cell-chips\"><span class=\"awe-cell-chip\" ng-repeat=\"chip in " + chips + ".visible track by $index\" ng-bind=\"chip\"></span>" +
          "<span class=\"awe-cell-chip awe-cell-chip-more\" ng-if=\"" + chips + ".hidden.length\" title=\"{{" + chips + ".hidden.join(', ')}}\"" +
          " aria-label=\"{{" + chips + ".hidden.length}} more: {{" + chips + ".hidden.join(', ')}}\">+{{" + chips + ".hidden.length}}</span></span>";
      }
      if (hasStatus(column) && hasIcon(column)) {
        return "<span class=\"awe-cell-status awe-cell-status-" + tone + " awe-cell-status-plain\" data-tone=\"" + tone + "\" title=\"{{" + value + "}}\">" +
          getCellIconTemplate() + "<span ng-bind=\"" + value + "\"></span></span>";
      }
      if (hasStatus(column)) {
        return "<span class=\"awe-cell-status awe-cell-status-" + tone + "\" data-tone=\"" + tone + "\" title=\"{{" + value + "}}\" ng-bind=\"" + value + "\"></span>";
      }
      if (hasIcon(column)) {
        return "<span class=\"awe-cell-icon-text\" title=\"{{" + value + "}}\">" + getCellIconTemplate() + "<span ng-bind=\"" + value + "\"></span></span>";
      }
      return null;
    };

    /**
     * Contents of a cell with an avatar or a swatch before its text. Both are decorative (the name is the accessible text).
     * The swatch takes the place of the avatar while the row has a valid colour; without both an empty space keeps the names
     * aligned
     *
     * @param {object} column Column model
     * @param {string} mainText Text of the cell (its main line and its secondary line)
     * @returns {string} Contents of the cell
     */
    const getEntityCellContents = function (column, mainText) {
      const swatch = hasSwatch(column) ? CELL_COMPONENT + ".getCellSwatch(row.entity, col.colDef)" : "''";
      const initials = hasAvatar(column) ? CELL_COMPONENT + ".getCellInitials(row.entity, col.colDef)" : "''";
      const avatarTone = "{{" + CELL_COMPONENT + ".getAvatarTone(row.entity[col.colDef.avatarToneField])}}";
      const swatchTemplate = hasSwatch(column) ? "<span class=\"awe-cell-swatch\" aria-hidden=\"true\" ng-if=\"" + swatch + "\"" +
        " ng-attr-style=\"--awe-swatch-color: {{" + swatch + "}}\"></span>" : "";
      const avatarTemplate = hasAvatar(column) ? "<span class=\"awe-cell-avatar awe-cell-avatar-" + avatarTone + "\" data-tone=\"" + avatarTone + "\"" +
        " aria-hidden=\"true\" ng-if=\"!" + swatch + " && " + initials + "\" ng-bind=\"" + initials + "\"></span>" : "";
      return "<div class=\"awe-cell-entity\">" + swatchTemplate + avatarTemplate +
        "<span class=\"awe-cell-avatar-spacer\" aria-hidden=\"true\" ng-if=\"!" + swatch + " && !" + initials + "\"></span>" +
        "<div class=\"awe-cell-entity-text\">" + mainText + "</div></div>";
    };

    /**
     * Cell template of a column with a secondary line, a status or chips (null when the column has none of them)
     *
     * @param {object} column Column model
     * @param {{wrap: boolean, maxLines: (number|null)}} wrapping Column wrapping
     * @returns {string|null} Cell template
     */
    const getRichCellTemplate = function (column, wrapping) {
      const contents = getRichCellContents(column);
      if (contents === null) {
        return null;
      }
      const clamp = wrapping.maxLines === null ? "" : " cell-clamp\" style=\"--cell-max-lines: " + wrapping.maxLines;
      const classes = (growsCell(column) ? " cell-rich" : "") + (wrapping.wrap ? " cell-wrap" + clamp : "");
      return "<div class=\"no-animate ui-grid-cell-contents" + classes + " {{" + CELL_COMPONENT + ".getCellStyle(COL_FIELD)}}\"" +
        (wrapping.wrap ? "" : " title=\"TOOLTIP\"") + " column-id=\"{{col.name}}\" data-testid=\"grid-cell\">" + contents + "</div>";
    };

    /**
     * Retrieve number of selected lines in grid
     *
     * @param {type} component Grid model
     * @returns {integer} Number of selected lines
     */
    const getSelectedLines = function (component) {
      let value;
      let model = component.model;
      if (model.selected === null) {
        value = 0;
      } else {
        value = model.selected.length;
      }
      return value;
    };

    /**
     * Retrieve the row value
     *
     * @param {type} model
     * @param {type} row
     * @param {type} column
     * @returns {undefined}
     */
    const getRowValue = function (model, row, column) {
      let  value = null;
      if (row === "footer" || (row > -1 && row < model.length)) {
        let  cell = model[row][column];
        if (angular.isObject(cell) && "value" in cell) {
          value = cell.value;
        } else {
          value = cell;
        }
      }
      return value;
    };

    /**
     * Sanitize selected values (retrieve values from row id)
     */
    const sanitizeSelection = function (selection, data, component) {
      let sanitized = [];
      _.each(selection, function (row) {
        let  rowIndex = Utilities.getRowIndex(data, row, component.constants.ROW_IDENTIFIER);
        if (rowIndex > -1) {
          sanitized.push(data[rowIndex][component.constants.ROW_IDENTIFIER]);
        }
      });
      return sanitized;
    };

    /**
     * Retrieve row identifier
     * @param component Component
     * @param row Row identifier
     * @param move Displacement
     * @returns {*}
     */
    const getRowIdentifier = function (component, row, move) {
      let model = component.model.values, index, value = null;
      if (!Utilities.isEmpty(row)) {
        index = Utilities.getRowIndex(model, row, component.constants.ROW_IDENTIFIER);
        value = index >= Math.max(move * -1, 0) && index < Math.min(model.length - move, model.length) ? model[index + move][component.constants.ROW_IDENTIFIER] : null;
      }
      return value;
    };

    /**
     * Grid constructor
     *
     * @param {Scope} component Component
     */
    const GridCommons = function (component) {
      this.component = component;

      // Generate grid cell controller / model
      component.constants.SELECTED_TAIL = ".selected";
      component.hasFrozen = false;
      component.addedRows = 0;
      component.isEditing = false;
      component.columnModelStringified = {};

      // Column layout of the user (grid toolbar)
      GridLayout.attach(component);

      // Define attribute methods
      this.attributeMethods = {
        /**
         * Retrieve current row value
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @param {string} row Row
         * @returns {mixed} Current row value
         */
        currentRowValue: function (comp, column, row) {
          let  index = Utilities.getRowIndex(comp.model.values, row, comp.constants.ROW_IDENTIFIER);
          return getRowValue(comp.model.values, index, column);
        },
        /**
         * Retrieve the value for the column of the previous row to the selected
         * one
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @param {string} row Row
         * @returns {mixed} Selected row value
         */
        prevCurrentRowValue: function (comp, column, row) {
          let  model = comp.model.values, index, value = null;
          let  prevRow = this.prevCurrentRow(comp, column, row);
          if (prevRow !== null) {
            index = Utilities.getRowIndex(model, prevRow, comp.constants.ROW_IDENTIFIER);
            value = getRowValue(model, index, column);
          }
          return value;
        },
        /**
         * Retrieve the value for the column of the next row to the selected one
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @param {string} row Row
         * @returns {mixed} Selected row value
         */
        nextCurrentRowValue: function (comp, column, row) {
          let  model = comp.model.values, index, value = null;
          let  nextRow = this.nextCurrentRow(comp, column, row);
          if (nextRow !== null) {
            index = Utilities.getRowIndex(model, nextRow, comp.constants.ROW_IDENTIFIER);
            value = getRowValue(model, index, column);
          }
          return value;
        },
        /**
         * Retrieve the value for the column of the selected row
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {mixed} Selected row value
         */
        selectedRowValue: function (comp, column) {
          let  model = comp.model.values;
          let  selectedRow = this.selectedRow(comp);
          let  index = Utilities.getRowIndex(model, selectedRow, comp.constants.ROW_IDENTIFIER);
          return getRowValue(model, index, column);
        },
        /**
         * Retrieve the value for the column of the previous row to the selected
         * one
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {mixed} Selected row value
         */
        prevRowValue: function (comp, column) {
          let  model = comp.model.values, index, value = null;
          let  prevRow = this.prevRow(comp);
          if (prevRow !== null) {
            index = Utilities.getRowIndex(model, prevRow, comp.constants.ROW_IDENTIFIER);
            value = getRowValue(model, index, column);
          }
          return value;
        },
        /**
         * Retrieve the value for the column of the next row to the selected one
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {mixed} Selected row value
         */
        nextRowValue: function (comp, column) {
          let  model = comp.model.values, index, value = null;
          let  nextRow = this.nextRow(comp);
          if (nextRow !== null) {
            index = Utilities.getRowIndex(model, nextRow, comp.constants.ROW_IDENTIFIER);
            value = getRowValue(model, index, column);
          }
          return value;
        },
        /**
         * Retrieve the value for the column of the footer
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {mixed} Footer value
         */
        footerValue: function (comp, column) {
          return getRowValue(comp.model, "footer", column);
        },
        /**
         * Retrieve the current row identifier
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @param {string} row Row
         * @returns {string} Row identifier
         */
        currentRow: function (comp, column, row) {
          return getRowIdentifier(comp, row, 0);
        },
        /**
         * Retrieve the previous to the selected row identifier
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @param {string} row Row
         * @returns {string} Row identifier
         */
        prevCurrentRow: function (comp, column, row) {
          return getRowIdentifier(comp, row, -1);
        },
        /**
         * Retrieve the next to the selected row identifier
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @param {string} row Row
         * @returns {string} Row identifier
         */
        nextCurrentRow: function (comp, column, row) {
          return getRowIdentifier(comp, row, 1);
        },
        /**
         * Retrieve the selected row identifier
         *
         * @param {string} comp Component
         * @returns {string} Row identifier
         */
        selectedRow: function (comp) {
          return comp.getSelectedRow();
        },
        /**
         * Retrieve the previous to the selected row identifier
         *
         * @param {string} comp Component
         * @returns {string} Row identifier
         */
        prevRow: function (comp) {
          return getRowIdentifier(comp, comp.getSelectedRow(), -1);
        },
        /**
         * Retrieve the next to the selected row identifier
         *
         * @param {string} comp Component
         * @returns {string} Row identifier
         */
        nextRow: function (comp) {
          return getRowIdentifier(comp, comp.getSelectedRow(), 1);
        },
        /**
         * Retrieve total number of rows
         *
         * @param {string} comp Component
         * @returns {integer} Number of rows
         */
        totalRows: function (comp) {
          return comp.model.records;
        },
        /**
         * Retrieve if column is empty or not
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {boolean} Column is empty
         */
        emptyDataColumn: function (comp, column) {
          let  filledRows = comp.getFilledRows(column);
          return filledRows === 0;
        },
        /**
         * Retrieve if column has data or not
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {boolean} Column has data
         */
        hasDataColumn: function (comp, column) {
          let  totalRows = comp.getTotalRows();
          let  filledRows = comp.getFilledRows(column);
          return totalRows >= filledRows && filledRows > 0;
        },
        /**
         * Retrieve if column is full (all cells with data) or not
         *
         * @param {string} comp Component
         * @param {string} column Column
         * @returns {boolean} Column is full
         */
        fullDataColumn: function (comp, column) {
          let  totalRows = comp.getTotalRows();
          let  filledRows = comp.getFilledRows(column);
          return totalRows === filledRows;
        },
        /**
         * Retrieve modified lines on multioperation grids
         *
         * @param {string} comp Component
         * @returns {integer}
         */
        modifiedRows: function (comp) {
          let modifiedRows = getSelectedLines(comp);
          if (comp.getExtraData) {
            let data = comp.getExtraData();
            modifiedRows = data[comp.address.component];
          }
          return modifiedRows;
        },
        // Retrieve selected rows number
        selectedRows: getSelectedLines,
        value: getSelectedLines
      };

      // Add attribute methods
      _.merge(this.component.attributeMethods, this.attributeMethods);
      return this;
    };

    /**
     * Summarize totals
     * @param {$rootScope.Scope} component
     * @param {object} summarizedCols
     * @param {object} componentData
     * @param {object} footerData
     */
    function summarizeTotals(component, summarizedCols, componentData, footerData) {
      // For each row, summarize the data
      _.each(component.model.values, function (row, rowIndex) {
        _.each(summarizedCols, function (column, columnId) {
          // Summarize value
          switch (column.type) {
            case "sum":
            default: {
              let value = parseFloat(getRowValue(component.model.values, rowIndex, columnId));
              column.value += isNaN(value) ? 0 : value;
              break;
            }
          }
          // Store as footer data
          if (column.component) {
            componentData[columnId] = column.value;
          } else {
            footerData[columnId] = column.value;
          }
        });
      });
    }

    /**
     * Initialize help column
     * @param {object} component
     * @param {object} column
     */
    function initHelpColumn(component, column) {
      Utilities.timeout(function () {
        // Initialize help node
        let help = {
          node: component.element.find(`[column-id='${column.id}']`),
          text: column.help,
          image: column.helpImage
        };
        component.initHelpNode(component.id, help);
      });
    }

    GridCommons.prototype = {
      /**
       * Initialize base grid
       */
      init: function () {
        // Initialize common methods
        let  component = this.component;

        // Loading indicator: the icon-loading of the grid wins over the application default
        component.scope.iconLoader = component.controller.iconLoading || $settings.get("gridIconLoading") || "spinner";
        // skeleton draws placeholder rows on every load; skeleton-dim draws them on the first load and dims the rows on a
        // reload while the grid has rows
        component.scope.skeletonLoader = ["skeleton", "skeleton-dim"].includes(component.scope.iconLoader);
        component.scope.dimReload = component.scope.iconLoader === "skeleton-dim";

        // Row height of the cells (their line height and the minimum of the wrapping rows)
        const setRowHeightProperty = (height) => component.element?.[0]?.style.setProperty("--grid-row-height", height + "px");

        /**
         * Change the row height of the grid: the options, the rows ui-grid has already built and the --grid-row-height
         * of the cells (their line height and the minimum of the wrapping rows)
         * @param {number} height Row height in pixels
         */
        component.applyRowHeight = function (height) {
          setRowHeightProperty(height);
          const options = component.scope.gridOptions;
          if (!options || options.rowHeight === height) {
            return;
          }
          options.rowHeight = height;
          const uiGrid = component.grid?.api?.grid;
          if (uiGrid) {
            uiGrid.rows.forEach((row) => {
              row.height = height;
            });
            component.updateGridScrollBars();
          }
        };

        // Row height: the one of the screen, or the one of the density, read again when the density of the page changes
        setRowHeightProperty(getGridRowHeight(component.controller));
        if (!getExplicitRowHeight(component.controller)) {
          const stopDensity = observeDensity(() => component.scope.$evalAsync(() => component.applyRowHeight(getDensityRowHeight())));
          component.scope.$on("$destroy", stopDensity);
        }

        // Check frozen attribute
        component.hasFrozen = this.hasFrozenColumns();

        // Wrapping cells: they need rows of different heights, which a grid with frozen columns (a second container
        // of rows) or a tree cannot keep aligned, so those keep their single-line rows
        component.wrapsText = !component.hasFrozen && _.some(component.controller.columnModel, (column) => getColumnWrap(column).wrap || growsCell(column));

        /**
         * Mark the cells whose text is cut as reachable with the keyboard, so their full text shows on focus
         */
        component.markCutCells = function () {
          component.element.find(CLAMPED_CELL).each(function () {
            if (this.scrollHeight > this.clientHeight + 1) {
              this.setAttribute("tabindex", "0");
            } else {
              this.removeAttribute("tabindex");
            }
          });
        };

        /**
         * Initialize the wrapping cells: the minimum row height and the tooltip with the full text of the cut cells
         * (one listener for the whole grid, on hover and on keyboard focus)
         */
        component.initWrapCells = function () {
          const element = component.element;
          setRowHeightProperty(component.scope.gridOptions?.rowHeight || getGridRowHeight(component.controller));
          let tooltip = null;
          const hide = function () {
            if (tooltip) {
              tooltip.remove();
              tooltip = null;
            }
          };
          const show = function (event) {
            const cell = event.currentTarget;
            hide();
            if (cell.scrollHeight > cell.clientHeight + 1) {
              const rect = cell.getBoundingClientRect();
              tooltip = $("<div class=\"tooltip bottom in grid-cell-tooltip\" role=\"tooltip\"><div class=\"tooltip-arrow\"></div><div class=\"tooltip-inner\"></div></div>");
              tooltip.find(".tooltip-inner").text($(cell).text());
              tooltip.css({top: rect.bottom, left: rect.left}).appendTo(document.body);
            }
          };
          element.off(".wrapCells")
            .on("mouseover.wrapCells focusin.wrapCells", CLAMPED_CELL, show)
            .on("mouseout.wrapCells focusout.wrapCells", CLAMPED_CELL, hide)
            // The handler returns nothing: jQuery treats a returned false as preventDefault, which would swallow the keys typed in the inputs of the grid
            .on("keydown.wrapCells", (event) => {
              if (event.key === "Escape") {
                hide();
              }
            });
          component.scope.$on("$destroy", () => {
            hide();
            element.off(".wrapCells");
          });
        };

        /**
         * Custom scroll management
         * @param uiGridViewport Viewport
         * @param scrollHandler Scroll handler
         */
        component.customScroller = function (uiGridViewport, scrollHandler) {
          uiGridViewport.on('scroll', (event) => {
            component.repositionSaveButton();
            scrollHandler(event);
          });
        };

        /**
         * Initialize grid layers
         */
        component.initLayers = function () {
          component.layers = {};
          // Calculate layers
          component.layers["grid"] = component.element;
          component.layers["gridNode"] = component.element.find(".ui-grid");
          component.layers["footer"] = component.element.find(".ui-grid-footer-cell-row");
          component.layers["content"] = component.element.find(".ui-grid-canvas");
          component.layers["header"] = component.element.find(".ui-grid-header");
          component.layers["clickout"] = component.element.find('.ui-grid-viewport');
          component.layers["contentWrapper"] = component.element.find('.ui-grid-contents-wrapper');
          component.layers["container"] = component.element.find('.ui-grid-render-container-body');
          setTimeout(function () {
            component.layers["save"] = component.element.find('.save-button');
          });
          component.scope.gridOptions.layers = component.layers;
        };
        /**
         * Specific getAttribute function (To be overwritten on complex
         * directives)
         *
         * @param {type} attribute Attribute to check
         * @param {type} column column to check
         * @param {type} row row to check
         * @returns {mixed} Attribute value
         */
        component.getAttribute = function (attribute, column, row) {
          return component.attributeMethods[attribute](component, column, row);
        };
        /**
         * Reposition the save button
         */
        component.repositionSaveButton = function () {
          // Initialize layers if not initialized
          if (!component.layers) {
            component.initLayers();
          }
          if (component.layers && component.layers["save"]) {
            // Change flag to editing
            component.isEditing = false;
            // Define initial button position
            let  buttonPosition = {
              top: -10000,
              left: -10000
            };
            if (component.editable && component.currentSelection && component.currentSelection.length === 1) {
              // Calculate button position if editing
              buttonPosition = component.getButtonPosition();
              // Change flag to editing
              component.isEditing = true;
            }

            // Set button position
            component.layers["save"].css(buttonPosition);
          }
        };
        /**
         * Update button position
         *
         * @returns {object} button position
         */
        component.getButtonPosition = function () {
          let  buttonPosition = {
            top: -10000
          };
          // Find selected row
          let  selectedRows = component.getSelection();
          if (selectedRows.length > 0) {
            let  selectedRow = component.layers["content"].find(".ui-grid-row[id='" + selectedRows[0][component.constants.ROW_IDENTIFIER] + "']").last();
            if (selectedRow.length > 0) {
              let  headerHeight = component.layers["header"].outerHeight(true);
              let  clickout = component.layers["clickout"];
              let  buttonWidth = component.layers["save"].outerWidth(true);
              let  clickoutMeasures = clickout.first().offset();
              let  rowMeasures = selectedRow.offset();
              rowMeasures.height = selectedRow.outerHeight(true);
              rowMeasures.width = selectedRow.outerWidth();
              rowMeasures.left -= selectedRow.outerWidth(true) - rowMeasures.width;
              clickoutMeasures.height = clickout.height();
              clickoutMeasures.width = clickout.last().width();
              clickoutMeasures.scrollLeft = clickout.last().scrollLeft();
              // Calculate new button position
              buttonPosition.left = (rowMeasures.left + clickoutMeasures.scrollLeft + Math.min(rowMeasures.width, clickoutMeasures.width) - buttonWidth) - clickoutMeasures.left;
              buttonPosition.top = (rowMeasures.top + rowMeasures.height + headerHeight) - clickoutMeasures.top;
              // If top position is offbound, move out
              buttonPosition.top = buttonPosition.top < headerHeight || buttonPosition.top > headerHeight + clickoutMeasures.height + 2 ? -10000 : buttonPosition.top;
            }
          }

          // Return button position
          return buttonPosition;
        };
        /**
         * Generate the cell data
         *
         * @param {object || array || string || integer} cellValue
         */
        component.getCellObject = function (cellValue) {
          let  cellObject = cellValue;
          if (Utilities.isNull(cellValue)) {
            cellObject = {
              value: null,
              label: ""
            };
          } else if (typeof cellValue !== "object") {
            cellObject = {
              value: cellValue,
              label: cellValue
            };
          } else if (angular.isArray(cellValue)) {
            cellObject = {
              value: cellValue,
              label: cellValue.join(", ")
            };
          } else if ("value" in cellObject && !("label" in cellObject)) {
            cellObject.label = cellObject.value;
          }

          return cellObject;
        };

        /**
         * Retrieve cell data
         *
         * @param {object} value Model value
         * @returns {object} Grid cell
         */
        component.getCell = function (value) {
          let cell = component.getCellObject(value);
          let constants = component.constants;
          let checkList = [
            constants.CELL_VALUE,
            constants.CELL_LABEL,
            constants.CELL_TITLE,
            constants.CELL_STYLE,
            constants.CELL_ICON
          ];

          _.each(checkList, (attribute) => {
            if (!(attribute in cell)) {
              cell[attribute] = "";
            }
          });

          return cell;
        };

        /**
         * Retrieve cell data
         *
         * @param {object} cell Cell to retrieve
         * @param {object} attribute Cell attribute
         * @returns {mixed} Grid data parsed
         */
        component.getCellData = function (cell, attribute) {
          return component.getCell(cell)[attribute];
        };

        /**
         * Retrieve column data
         *
         * @param {Service} column Column identifier
         * @param {Boolean} getAll Get all values
         * @returns {Object} Data from column
         */
        component.getColumnData = function (column, getAll) {
          // Initialize data
          let  data = {};
          let  columnData = [];
          let  selectedRowData = [];
          let  selected = Utilities.asArray(component.model.selected);

          _.each(component.model.values, function (row) {
            let  rowId = row[component.constants.ROW_IDENTIFIER];
            let  cellValue = component.getCellData(row[column], component.constants.CELL_VALUE);
            let  isSelected = selected.indexOf(rowId) > -1;
            if (getAll || component.controller.sendAll || isSelected) {
              columnData.push(cellValue);
            }

            // Get selected rows if there is only one row selected
            if (isSelected) {
              selectedRowData.push(cellValue);
            }
          });

          // Format data list
          data[column] = columnData;

          // Store selected data
          component.getSelectedCellData(data, column, selectedRowData);
          return data;
        };

        /**
         * Retrieve identifier column data
         *
         * @returns {Object} Data from column
         */
        component.getIdentifierColumnData = function () {
          // Initialize data
          let  data = {};
          let  columnData = [];
          let  selected = Utilities.asArray(component.model.selected);

          _.each(component.model.values, function (row) {
            let  rowId = row[component.constants.ROW_IDENTIFIER];
            let  isSelected = selected.indexOf(rowId) > -1;
            if (component.controller.sendAll || isSelected) {
              columnData.push(rowId);
            }
          });

          // Store as identifier list
          data[`${component.address.component}-id`] = columnData;

          // If there is only one row selected, send selected row address
          if (selected.length === 1) {
            data[`${component.address.component}.selectedRowAddress`] = {...component.address, row: selected[0]};
          }

          return data;
        };

        /**
         * Retrieve selected row data
         *
         * @param {type} data
         * @param {type} column
         * @param {type} cellData
         * @returns {Object} Data from grid
         */
        component.getSelectedCellData = function (data, column, cellData) {
          let  cellValue = cellData.length === 0 ? null : cellData.length === 1 ? cellData[0] : cellData;
          data[column + component.constants.SELECTED_TAIL] = cellValue;
        };

        /**
         * Get data function, to retrieve grid data
         *
         * @returns {Object} Data from grid
         */
        component.getData = function () {
          // Initialize data
          let  data = {};

          // Selected rows
          data[component.address.component] = component.model.selected;

          // Sort and order fields
          data[component.address.component + $settings.get("dataSuffix")] = component.getSpecificFields();

          // Column data
          _.each(component.controller.columnModel, function (column) {
            if (column.sendable && "id" in column) {
              _.merge(data, component.getColumnData(column.id));
            }
          });

          // Identifier data
          _.merge(data, component.getIdentifierColumnData());

          // Add extra data
          if (component.getExtraData) {
            _.merge(data, component.getExtraData());
          }

          return data;
        };

        /**
         * Get data function, to retrieve grid data
         *
         * @returns {Object} Data from grid
         */
        component.getPrintData = function () {
          // Initialize data
          let  data = {};

          // Sort and order fields
          data[component.address.component + $settings.get("dataSuffix")] = component.getSpecificFields();

          // Column data
          _.each(component.controller.columnModel, function (column) {
            if (column.sendable && "id" in column) {
              _.merge(data, component.getColumnPrintData(column.id, column.component));
            }
          });

          // Identifier data
          _.merge(data, component.getIdentifierColumnData());

          // Add extra data
          if (component.getExtraData) {
            _.merge(data, component.getExtraData());
          }

          // Add visible columns information
          data[component.address.component + $settings.get("dataSuffix")].visibleColumns = component.getVisibleColumns();

          // Add footer information
          let footerData = {};

          // Footer data
          _.each(component.controller.columnModel, function (column) {
            if (column.sendable && "id" in column) {
              let address = angular.extend({column: column.id, row: "footer"}, component.address);
              footerData[column.id] = {
                value: (component.model.footer || {})[column.id] || null,
                label: component.getVisibleData(address, "footer", column)
              };
            }
          });
          data[component.address.component + $settings.get("dataSuffix")].footer = footerData;

          return data;
        };

        /**
         * Retrieve column data
         *
         * @param {Service} column Column identifier
         * @returns {Object} Data from column
         */
        component.getColumnPrintData = function (columnId) {
          // Initialize data
          let  data = {};
          let  columnValues = [];
          let  columnData = [];
          let  address = angular.extend({column: columnId}, component.address);
          let  column = component.getColumn(columnId);
          let  selectedRowData = [];
          let  selected = Utilities.asArray(component.model.selected);

          _.each(component.model.values, function (row) {
            let  rowId = row[component.constants.ROW_IDENTIFIER];
            address.row = rowId;
            let isSelected = selected.indexOf(rowId) > -1;
            let cellData = {
              ...component.getCell(row[address.column]),
              label: component.getVisibleData(address, row, column)
            };
            let cellValue = cellData.value;
            columnValues.push(cellValue);
            columnData.push(cellData);

            // Get selected rows if there is only one row selected
            if (isSelected) {
              selectedRowData.push(cellValue);
            }
          });

          // Format data list
          data[columnId] = columnValues;
          data[columnId + $settings.get("dataSuffix")] = columnData;

          // Store selected data
          component.getSelectedCellData(data, columnId, selectedRowData);
          return data;
        };

        /**
         * Retrieve column visible data
         *
         * @returns {String} Visible column data
         */
        component.getVisibleData = function (address, row, column) {
          let  data = component.getCellData(row[address.column], component.constants.CELL_LABEL);
          switch (column.type) {
            case "float":
            case "integer":
              data = component.getCellData(row[address.column], component.constants.CELL_VALUE);
              break;
            default:
              switch (column.component) {
                case "select":
                case "suggest":
                case "select-multiple":
                case "suggest-multiple":
                case "radio":
                case "button-radio":
                case "progress":
                case "numeric":
                  if (component.getApi) {
                    let componentApi = component.getApi(address);
                    if (componentApi && componentApi.getVisibleValue) {
                      data = componentApi.getVisibleValue();
                    }
                  }
                  break;
                case "checkbox":
                  data = component.getCellData(row[address.column], component.constants.CELL_VALUE);
                  data = !Utilities.isEmpty(data) && String(data) !== "0";
                  break;
                case "image":
                  data = component.getCellData(row[address.column], component.constants.CELL_IMAGE);
                  break;
                case "icon":
                  data = component.getCellData(row[address.column], component.constants.CELL_ICON);
                  break;
                case "password":
                  data = "****";
                  break;
                default:
                  break;
              }
          }

          return data;
        };

        /**
         * Retrieve current header for column
         * @param headers
         * @param column
         * @returns {{columnCounter: *, name: *, columnList: [], header: boolean, label}|null}
         */
        function findCurrentHeader(headers = [], column = {}) {
          let header = headers.find(h => h.startColumnName === column.name) || null;
          if (header !== null) {
            return {
              columnList: [],
              columnCounter: header.numberOfColumns,
              name: header.titleText,
              label: header.titleText.split(" ").map(singleLabel => $translate.instant(singleLabel)).join(" "),
              header: true
            };
          }
          return null;
        }

        /**
         * Get column print data
         * @param column
         * @returns {{component, charlength, name, width, label, type, align, printable}}
         */
        function getColumnInformation(column) {
          const {name, label, type, component, charlength, align} = column;
          // Hidden columns are rendered with width 0 on screen, so use their initial width for printing
          const width = column.hidden ? column.initialWidth : column.width;
          const translate = (text) => text.split(" ").map(l => $translate.instant(l)).join(" ");
          const information = {
            name, type, component, width, charlength, align,
            printable: column.printMode,
            label: translate(label || "")
          };
          // Secondary field, which the export may write in its own column
          if (column.secondaryField) {
            information.secondaryField = column.secondaryField;
            information.exportSecondary = column.exportSecondary === true;
            information.secondaryLabel = column.secondaryLabel ? translate(column.secondaryLabel) : null;
          }
          return information;
        }

        /**
         * Check whether a column must be sent to the report, according to its declared `printable` value:
         * - `false`: never
         * - `true`: always, visible or hidden
         * - `excel`: always (the server keeps it for spreadsheet outputs only)
         * - not declared: only when the column is visible on screen
         *
         * Internal ui-grid columns (row numbers, selection checkbox) carry no `printable` flag and are never printed, and
         * neither are the action columns (`component="button"`), whatever their `printable` value.
         *
         * @param {object} column Column model
         * @returns {boolean} Column must be printed
         */
        function isPrintableColumn(column) {
          if (!column.printable || isActionColumn(column)) {
            return false;
          }
          switch (String(column.printMode || "").toLowerCase()) {
            case "false":
              return false;
            case "true":
            case "excel":
              return true;
            default:
              // Columns hidden by the layout of the user are printed: reports use the definition of the screen
              return !column.hidden || !!column.layoutHidden;
          }
        }

        /**
         * Retrieve printable column information for reports (see isPrintableColumn).
         * Header counters are walked over every column so that non printable columns
         * inside a group header do not break the grouping.
         *
         * @returns {array} Printable column list
         */
        component.getVisibleColumns = function () {
          let columns = [];
          let headers = component.controller.headerModel;
          let currentHeader = null;
          component.controller.columnModel.forEach(column => {
            currentHeader = currentHeader || findCurrentHeader(headers, column);
            if (currentHeader !== null) {
              if (isPrintableColumn(column)) {
                currentHeader.columnList.push(getColumnInformation(column));
              }
              currentHeader.columnCounter--;
              if (currentHeader.columnCounter === 0) {
                if (currentHeader.columnList.length > 0) {
                  columns.push(currentHeader);
                }
                currentHeader = null;
              }
            } else if (isPrintableColumn(column)) {
              columns.push(getColumnInformation(column));
            }
          });

          // Retrieve columns
          return columns;
        };
        /**
         * Retrieve component model
         *
         * @param {string} rowId Row identifier
         */
        component.getRowValues = function (rowId) {
          // Calculate rowIndex
          let  rowIndex = Utilities.getRowIndex(component.model.values, rowId, component.constants.ROW_IDENTIFIER);

          // Retrieve selected row values
          return component.model.values[rowIndex];
        };
        /**
         * Retrieve selected row
         *
         * @return {String} Selected row
         */
        component.getSelectedRow = function () {
          let selected = component.model.selected;
          return angular.isArray(selected) && selected.length > 0 ? selected[0] : null;
        };
        /**
         * Retrieve selected rows
         *
         * @return {String} Selected row
         */
        component.getSelectedRows = function () {
          let  selected = component.model.selected;
          return angular.isArray(selected) && selected.length > 0 ? selected : null;
        };
        /**
         * Retrieve selected row data
         */
        component.getSelectedRowData = function () {
          let  selectedRowData = {};
          let  selectedRow = component.getSelectedRow();
          if (selectedRow !== null) {
            selectedRowData = _.cloneDeep(component.getRowValues(selectedRow));
          }
          // Retrieve selected row values
          return selectedRowData;
        };
        /**
         * Update the model and select rows
         */
        component.updateModelAndSelectRows = function () {
          // Update model
          component.updateModelSpecific().then(function () {
            // Call select rows event
            component.filterSelection();
            component.setSelection(component.currentSelection);
          });
        };
        /**
         * On update cell data
         *
         * @param {type} rowId
         * @param {type} columnId
         * @param {type} data
         */
        component.updateCell = function (rowId, columnId, data) {
          let value = null, style = null;
          let model = {};
          if (data != null && typeof data === "object") {
            value = "value" in data ? data.value : null;
            style = "style" in data ? data.style : null;
            model.selected = value;
            model.values = [data];
          } else {
            value = data;
            model.selected = value;
          }

          // Calculate rowIndex
          let rowIndex = Utilities.getRowIndex(component.model.values, rowId, component.constants.ROW_IDENTIFIER);
          if (rowIndex > -1) {
            // Update value data
            component.model.values[rowIndex][columnId] = data;

            // Publish model changed
            let address = _.cloneDeep(component.address);
            address.column = columnId;
            address.row = rowId;
            Control.publishModelChanged(address, model);

            // Update cell specific
            component.updateCellSpecific(rowId, columnId, value, style);
          }
        };
        /**
         * On update cell data (empty hook, overrides receive the parameters below)
         *
         * @param {type} rowId
         * @param {type} columnId
         * @param {type} value
         * @param {type} style
         */
        component.updateCellSpecific = function () {
        };
        /**
         * Retrieve column index
         *
         * @param {string} columnId Column identifier
         * @returns {integer} Selected column index in columns array
         */
        component.getColumnIndex = function (columnId) {
          let  colIndex = -1;
          _.each(component.controller.columnModel, function (column, index) {
            if (column.name === columnId) {
              colIndex = index;
            }
          });
          return colIndex;
        };
        /**
         * Retrieve column index
         *
         * @param {string} columnId Column identifier
         * @returns {integer} Selected column index in columns array
         */
        component.getColumn = function (columnId) {
          let  columnObject = null;
          _.each(component.controller.columnModel, function (column) {
            if (column.name === columnId) {
              columnObject = column;
            }
          });
          return columnObject;
        };
        /**
         * Retrieve column value list (if defined)
         *
         * @param {string} address Row index
         * @returns {array} Column value list
         */
        component.getColumnValueList = function (address) {
          // Retrieve values from cells if it exists
          let  cellId = Utilities.getCellId(address);
          let  values = _.get(component, ["model", "cells", cellId], null);
          if (values === null) {
            // Retrieve values from column model if it exists
            let  columnModel = component.controller.columnModel;
            let  columnIndex = component.getColumnIndex(address.column);
            if ('model' in columnModel[columnIndex]) {
              values = columnModel[columnIndex].model.values;
            }
          }

          return values;
        };
        /**
         * Retrieve total loaded rows
         */
        component.getTotalRows = function () {
          // Retrieve total rows
          return component.model.values.length;
        };
        /**
         * Retrieve filled loaded rows for a column
         *
         * @param {string} column Column to check
         */
        component.getFilledRows = function (column) {
          let filledRows = 0;
          // For each row, check the cell value
          _.each(component.model.values, function (row) {
            // Retrieve cell value
            let cellValue = component.getCellData(row[column], component.constants.CELL_VALUE);
            // Add 1 to filledRows if cell is not empty
            filledRows += Utilities.isEmpty(cellValue) ? 0 : 1;
          });
          return filledRows;
        };
        /**
         * Mark the rows as selected
         *
         * @param {object} selectedRows
         */
        component.selectRows = function (selectedRows) {
          // Store selected rows
          if (!_.isEqual(component.currentSelection, selectedRows)) {
            component.currentSelection = selectedRows;
            component.model.selected = selectedRows.length === 0 ? null : selectedRows;
            // Store event
            component.storeEvent('select-row');
            // Publish model change
            Control.publishModelChanged(component.address, {
              selected: component.model.selected
            });
            component.scope.$broadcast("selectionChanged");
          }
        };
        /**
         * Unselect all rows
         */
        component.unselectRows = function () {
          // Unselect the rows
          component.resetSelection();
        };
        /**
         * Select the rows
         *
         * @param {Array} selection Selection
         */
        component.setSelection = function (selection) {
          // Reset selection
          let sanitizedSelection = selection;
          component.resetSelection();
          if (selection !== null) {
            // Get selection as array
            let selectionArray = selection;
            if (typeof selection === "string") {
              selectionArray = [selection];
            }
            // Set selection
            let data = component.model.values;

            // Sanitize selection
            sanitizedSelection = sanitizeSelection(selectionArray, data, component);

            // Select rows
            _.each(sanitizedSelection, function (row) {
              let rowIndex = Utilities.getRowIndex(data, row, component.constants.ROW_IDENTIFIER);
              if (rowIndex > -1) {
                component.selectRow(data[rowIndex]);
              }
            });
          }
          // Call select rows event
          component.onSelectRows(sanitizedSelection);
        };

        /**
         * Reset the grid
         */
        component.resetGrid = function () {
          // Change editing flag
          component.isEditing = false;
          // Clear grid data
          component.emptyGrid();
          // Udpate model
          component.updateModel();
        };
        /**
         * Empty the grid
         */
        component.emptyGrid = function () {
          component.currentSelection = [];
          component.model.selected = [];
          component.model.values = [];
          component.model.cells = {};
          component.model.footer = {};
          component.model.records = 0;
          component.model.total = 1;
          component.model.page = 1;
          // Reposition save button
          component.repositionSaveButton();
        };
        /**
         * Filter the selection with the model values
         */
        component.filterSelection = function () {
          // Store selected rows
          let  selectedRows = Utilities.asArray(component.currentSelection);
          let  availableSelectedRows = [];
          _.each(component.model.values, function (row) {
            let  rowId = row[component.constants.ROW_IDENTIFIER];
            if (selectedRows.indexOf(rowId) > -1) {
              availableSelectedRows.push(rowId);
            }
          });

          // Select available rows
          component.selectRows(availableSelectedRows);
        };
        /**
         * On select rows event
         *
         * @param {Array} selectedRows Selected rows
         */
        component.onSelectRows = function (selectedRows) {
          if (!_.isEqual(selectedRows, component.currentSelection)) {
            component.selectRows(selectedRows);
          }
        };
        /**
         * Add a new row
         *
         * @param {integer} position
         * @param {object} data
         * @param {String} selectedRow selected row
         */
        component.addRow = function (position, data, selectedRow) {
          // Add the new row
          component.addRowSpecific(selectedRow, position, data);
        };
        /**
         * Move a row to a new position of the grid. The rows of the model keep the new order, so it is the order
         * the grid sends to the server when it is saved
         *
         * @param {string} position Where to move the row: first, before (one up), after (one down) or last. With a
         * target row, before or after that row
         * @param {string} [rowId] Row identifier (the selected row by default)
         * @param {string} [targetRowId] Row next to which the row lands (drag and drop), instead of the grid positions
         * @return {boolean} The row has been moved (the rows of a tree grid follow its structure and never move)
         */
        component.moveRow = function (position, rowId, targetRowId) {
          const values = component.model.values;
          const row = rowId || component.getSelectedRow();
          const index = row === null || row === undefined ? -1 : Utilities.getRowIndex(values, row, component.constants.ROW_IDENTIFIER);
          const hasTarget = targetRowId !== undefined;
          let target = {first: 0, before: index - 1, after: index + 1, last: values.length - 1}[position];
          if (hasTarget) {
            // The target row, once the moved one is out of the list, tells where the moved row lands
            const others = values.filter((value, valueIndex) => valueIndex !== index);
            const targetIndex = index < 0 ? -1 : Utilities.getRowIndex(others, targetRowId, component.constants.ROW_IDENTIFIER);
            const offset = {before: 0, after: 1}[position];
            target = targetIndex < 0 || offset === undefined ? undefined : targetIndex + offset;
          }
          if (component.isTree || index < 0 || target === undefined || target < 0 || target >= values.length || target === index) {
            return false;
          }

          // Move the row and keep it selected (a dropped row is the one selected) once the grid shows the new order
          // The grid watches only the reference and the length of its data (fastWatch), so the model gets a new array
          const selection = hasTarget ? [values[index][component.constants.ROW_IDENTIFIER]] : component.model.selected;
          const reordered = [...values];
          reordered.splice(target, 0, reordered.splice(index, 1)[0]);
          component.model.values = reordered;
          component.updateModelSpecific().then(() => component.setSelection(selection));
          return true;
        };
        /**
         * Move the selected row of an editable grid with the keyboard:
         * Alt+ArrowUp (up), Alt+ArrowDown (down), Alt+Home (top) and Alt+End (bottom)
         *
         * @param {KeyboardEvent} event Keyboard event
         * @return {boolean} The key combination has been handled
         */
        component.onMoveRowKeyDown = function (event) {
          const position = MOVE_ROW_KEYS[event.key];
          if (!(component.editable || component.controller?.moveRows) || !position || !event.altKey || event.ctrlKey || event.shiftKey || event.metaKey) {
            return false;
          }
          event.preventDefault();
          component.moveRow(position);
          return true;
        };
        /**
         * Update the selected row
         *
         * @param {string} rowId Row identifier
         * @param {object} data  Row data
         * @param {string} style Row style
         */
        component.updateRow = function (rowId, data, style) {
          _.each(data, function (value, columnId) {
            component.updateCell(rowId, columnId, value);
          });

          // Update row style if defined
          if (style) {
            component.addRowStyle(rowId, style);
          }

          // Update row specific call
          if (component.updateRowSpecific) {
            component.updateRowSpecific(rowId, data, style);
          }
        };
        /**
         * Update the selected row
         * @param {object} data
         * @param {string} New row style
         */
        component.updateSelectedRow = function (data, style) {
          // Update selected row
          let  selectedRow = component.getSelectedRow();
          component.updateRow(selectedRow, data, style);
        };
        /**
         * Add column dependencies to grid
         *
         * @param {Object} column Column parameters
         * @public
         */
        component.addGridDependencies = function (column) {
          // Store cell specific dependencies
          let  columnDependencies = [];
          // Transform show, hide and label dependencies into showColumn,
          // hideColumn and columnLabel dependencies
          _.each(column.dependencies, function (dependency) {

            // Set flag
            let  addToGrid = false;
            // Transform target_type depending on previous target_type
            switch (dependency.target) {
              case "show":
                if (!column.component) {
                  dependency.target = "showColumn";
                  addToGrid = true;
                } else {
                  columnDependencies.push(dependency);
                }
                break;
              case "show-column":
                dependency.target = "showColumn";
                addToGrid = true;
                break;
              case "hide":
                if (!column.component) {
                  dependency.target = "hideColumn";
                  addToGrid = true;
                } else {
                  columnDependencies.push(dependency);
                }
                break;
              case "hide-column":
                dependency.target = "hideColumn";
                addToGrid = true;
                break;
              case "label":
                dependency.target = "columnLabel";
                addToGrid = true;
                break;
              default:
                columnDependencies.push(dependency);
                break;
            }

            // Add column name
            dependency.column = column.id;
            // Add dependency to grid dependencies
            if (addToGrid) {
              component.controller.dependencies.push(_.cloneDeep(dependency));
            }
          });
          // Store cell dependencies
          column.dependencies = columnDependencies;
        };

        /**
         * Add columns
         *
         * @param {Array} columns
         */
        component.addColumns = function (columns) {
          let  columnsAdded = false;
          _.each(columns, function (column) {
            let  added = component.addColumn(column);
            columnsAdded = columnsAdded || added;
          });

          // Notify changed columns and resize
          Utilities.timeout(function () {
            component.reportChangedColumns();
            component.resize();
          });
        };

        /**
         * Replace columns
         *
         * @param {Array} columns
         */
        component.replaceColumns = function (columns) {

          // Reset grid columns model
          component.controller.columnModel.splice(0, component.controller.columnModel.length);
          component.columnModelStringified = {};

          // Fix column model
          component.fixColumnModel(false);

          // Add columns
          component.addColumns(columns);
        };

        /**
         * Fix retrieved column model
         */
        component.fixColumnModel = function (fixEachColumn) {

          // Store the columns of the screen before the grid adds its own ones
          if (fixEachColumn) {
            component.storeLayoutBase();
          }

          // Add initial columns model if exists
          if (component.addInitialColumns) {
            component.addInitialColumns();
          }

          // Add extra column model if exists
          if (component.addExtraColumnModelData) {
            component.addExtraColumnModelData();
          }

          // For each column, fix the column
          if (fixEachColumn) {
            _.each(component.controller.columnModel, component.fixColumn);
          }
        };

        /**
         * Fix retrieved column model to fit jquery grid colmodel
         *
         * @param {object} column Column model
         */
        component.fixColumn = function (column) {
          // Calculate width based on charlength
          if (column.charlength !== "0" && !column.width) {
            column.width = column.charlength * component.scope.charSize;
          }

          // Set default column width
          if (!column.width) {
            column.width = 20 * component.scope.charSize;
          }

          // Store initial width
          column.initialWidth = column.width;

          // If column is hidden, generate column but don't display it
          if (column.hidden) {
            column.width = 0;
            column.enableColumnResizing = false;
            column.enableColumnMoving = false;
          }

          // Other attributes
          column.displayName = column.label || "";
          column.headerClass = column.style || "";
          column.name = column.name || column.id;
          column.id = column.name;
          column.enableSorting = component.enableSorting && column.sortable;
          column.sortField = column.sortField || column.name;
          column.field = column.name;

          column.pinnedLeft = column.frozen;

          // Retrieve cell template
          column.cellClass = function (grid, row, col) {
            let  value = "text-" + col.colDef.align + (col.colDef.style ? " " + col.colDef.style : "");
            let  cellStyle = component.getCellStyle(row.entity[col.field]);
            value += cellStyle ? " " + cellStyle : "";
            return value;
          };

          // Column templates
          column.headerCellTemplate = column.headerCellTemplate || "grid/header";
          column.cellTemplate = column.cellTemplate || "grid/cell";
          column.footerCellTemplate = column.footerCellTemplate || "grid/footer";
          column.filterHeaderTemplate = column.filterHeaderTemplate || "grid/headerFilter";

          // Text that wraps over several lines, secondary line, status and chips
          if (!("component" in column) && column.cellTemplate === "grid/cell") {
            const wrapping = component.wrapsText ? getColumnWrap(column) : {wrap: false, maxLines: null};
            const richTemplate = getRichCellTemplate(column, wrapping);
            if (richTemplate !== null) {
              column.cellTemplate = richTemplate;
            } else if (wrapping.wrap) {
              column.cellTemplate = getWrapCellTemplate(wrapping);
            }
          }

          column.sortingAlgorithm = component.sortingAlgorithm;

          // Calculate fields with components
          if ("component" in column) {
            column.enableFiltering = false;
            column.cellTemplate = "<div class=\"ui-grid-cell-contents component\" title=\"TOOLTIP\" column-id=\"{{col.name}}\" data-testid=\"grid-cell\"><awe-column-" + column.component +
              " cell-address='{\"hash\":\"{{row.uid}}\", \"view\":\"" + component.address.view + "\", \"component\":\"" + component.address.component + "\", \"row\":\"{{row.entity." +
              component.constants.ROW_IDENTIFIER + "}}\", \"column\":\"{{col.name}}\"}'/></div>";
            column.footerCellTemplate = column.summaryType ? "<div class=\"ui-grid-cell-contents ui-grid-cell-footer\" title=\"TOOLTIP\" column-id=\"{{col.name}}\"><awe-column-" +
              column.component + " cell-address='{\"hash\":\"footer-{{grid.appScope.model.page}}\", \"view\":\"" + component.address.view + "\", \"component\":\"" +
              component.address.component + "\", \"row\":\"footer\", \"column\":\"{{col.name}}\"}'/></div>" : column.footerCellTemplate;
          }

          // Remove dependencies and add them to the grid
          component.addGridDependencies(column);
        };
        /**
         * On column change
         */
        component.reportChangedColumns = function () {
          let  columns = component.getColumns();
          let  headerModel = component.controller.headerModel;
          if (headerModel.length > 0) {
            // The header of each column comes from the membership of the column, never from its position
            component.fixedHeaders = getColumnHeaders(headerModel, component.layoutBase || component.controller.columnModel);
          }
          component.scope.gridOptions.columnHeaders = {
            columns: columns,
            headers: component.fixedHeaders
          };
          component.scope.$broadcast('columns-changed', component.scope.gridOptions.columnHeaders);
        };
        /**
         * Right click over row
         *
         * @param {type} event
         * @param {type} row
         */
        component.rowContextMenu = function (event, row) {
          component.selectRow(row.entity);
          component.showContextMenu(event);
        };
        /**
         * Select the row of a button of the grid before its actions: that row ends being the only selected one. A row
         * that is already the only selected one is left as it is (an edited row keeps being edited), and an identifier
         * that is not a row of the grid (the footer) changes nothing
         *
         * @param {String} rowId Row identifier
         */
        component.selectButtonRow = function (rowId) {
          const data = component.model.values || [];
          if (Utilities.getRowIndex(data, rowId, component.constants.ROW_IDENTIFIER) === -1) {
            return;
          }
          const selected = component.model.selected || [];
          if (selected.length === 1 && String(selected[0]) === String(rowId)) {
            return;
          }
          component.setSelection([rowId]);
        };
        /**
         * Select a row and show the context menu under an element (action show-context-menu of a row button)
         *
         * @param {String} rowId Row identifier
         * @param {jQuery} anchor Element the menu hangs from
         */
        component.showRowContextMenu = function (rowId, anchor) {
          // A grid that declares no options has nothing to open: the row is not selected either. A grid that does
          // declare them opens its menu even if none is visible yet, as their dependencies resolve after the selection
          if (!(component.controller.contextMenu || []).length) {
            return;
          }
          const data = component.model.values;
          const rowIndex = Utilities.getRowIndex(data, rowId, component.constants.ROW_IDENTIFIER);
          if (rowIndex > -1) {
            component.selectRow(data[rowIndex]);
            component.showContextMenuUnder(anchor);
          }
        };
        /**
         * Click over viewport
         *
         * @param {type} event
         */
        component.viewportClick = function (event) {
          if (event.currentTarget === event.target) {
            component.hideContextMenu(event);
          }
        };
        /**
         * Right click over viewport
         *
         * @param {type} event
         */
        component.viewportContextMenu = function (event) {
          component.showContextMenu(event);
        };

        /**
         * Retrieves the cell value
         *
         * @param {type} cell
         * @returns {String}
         */
        component.getCellValue = function (cell) {
          return component.getCellData(cell, component.constants.CELL_LABEL);
        };

        /**
         * Retrieves the cell value
         *
         * @param {type} cell
         * @returns {String}
         */
        component.getCellStyle = function (cell) {
          return component.getCellData(cell, component.constants.CELL_STYLE);
        };

        /**
         * Retrieve the tone of a status cell: one of STATUS_TONES, neutral for any other value
         *
         * @param {*} cell Cell of the status field
         * @returns {String} Tone
         */
        component.getCellTone = function (cell) {
          return toTone(component.getCellData(cell, component.constants.CELL_VALUE), STATUS_TONES);
        };

        /**
         * Retrieve the tone of an avatar: one of AVATAR_TONES, neutral for any other value
         *
         * @param {*} cell Cell of the avatar tone field
         * @returns {String} Tone
         */
        component.getAvatarTone = function (cell) {
          return toTone(component.getCellData(cell, component.constants.CELL_VALUE), AVATAR_TONES);
        };

        /**
         * Retrieve the icon of a cell, from the icon field of the row (no family when it has none)
         *
         * @param {object} row Row data
         * @param {object} column Column model
         * @returns {{family: String, name: String, classes?: String, href?: String}} Icon
         */
        component.getCellIcon = function (row, column) {
          const icon = component.getCellData(row[column.iconField], component.constants.CELL_VALUE);
          return Utilities.extractIcon(Utilities.isNull(icon) ? "" : String(icon));
        };

        /**
         * Retrieve the initials of the avatar of a cell, built from the avatar field of the row (empty when it has none)
         *
         * @param {object} row Row data
         * @param {object} column Column model
         * @returns {String} Initials
         */
        component.getCellInitials = function (row, column) {
          const name = component.getCellValue(row[column.avatarField]);
          return Utilities.isNull(name) ? "" : getInitials(String(name));
        };

        /**
         * Retrieve the colour of the swatch of a cell, from the swatch field of the row: a known palette or a hex (empty
         * for any other value, so nothing else reaches the style of the swatch)
         *
         * @param {object} row Row data
         * @param {object} column Column model
         * @returns {String} CSS colour
         */
        component.getCellSwatch = function (row, column) {
          return getSwatchColor(component.getCellData(row[column.swatchField], component.constants.CELL_VALUE));
        };

        /**
         * Retrieve the text of the secondary line of a cell (empty when the row has none)
         *
         * @param {object} row Row data
         * @param {object} column Column model
         * @returns {String} Secondary text
         */
        component.getCellSecondary = function (row, column) {
          const secondary = component.getCellValue(row[column.secondaryField]);
          return Utilities.isNull(secondary) ? "" : String(secondary);
        };

        /**
         * Retrieve the chips of a cell: its value split by the separator of the column, trimmed and without empty items.
         * Over the maximum of the column, the rest are hidden (shown as a "+n" chip).
         *
         * @param {*} cell Cell
         * @param {object} column Column model
         * @returns {{visible: String[], hidden: String[]}} Visible and hidden chips
         */
        component.getCellChips = function (cell, column) {
          const value = component.getCellValue(cell);
          const items = Utilities.isNull(value) ? [] : String(value).split(column.chipSeparator || ",")
            .map(item => item.trim())
            .filter(item => item !== "");
          const maxChips = parseInt(column.maxChips, 10);
          const limit = maxChips > 0 ? maxChips : items.length;
          return {visible: items.slice(0, limit), hidden: items.slice(limit)};
        };
        /**
         * Edit a row
         *
         * @param {String} rowId row Identifier
         */
        component.editRowSpecific = function (rowId) {
          // Select the row
          let  data = component.model.values;
          let  rowIndex = Utilities.getRowIndex(data, rowId, component.constants.ROW_IDENTIFIER);
          if (rowIndex !== -1) {
            component.selectRow(data[rowIndex]);
            // Reposition buttons
            component.repositionSaveButton();
          }
        };
        /**
         * Add an style to a row
         *
         * @param {String} rowId row Identifier
         * @param {String} rowClass row class to add
         */
        component.addRowStyle = function (rowId, rowClass) {
          // Select the row
          let  data = component.model.values;
          let  rowIndex = Utilities.getRowIndex(data, rowId, component.constants.ROW_IDENTIFIER);
          data[rowIndex][component.constants.ROW_CLASS_FIELD] = rowClass;
        };
        /**
         * Add column to column model
         *
         * @param {Array} model Column to add
         */
        component.addToColumnModel = function (model) {
          component.controller.columnModel.push(model);
          component.columnModelStringified[model.id] = JSON.stringify(model);
        };
        /**
         * Add column to column model
         *
         * @param {Object} column
         * @returns {boolean} column has been added
         */
        component.addColumn = function (column) {
          let  added = false;
          // Check whether column has been already added or not
          if (component.getColumnIndex(column.name) === -1) {
            // Fix the column
            component.fixColumn(column);
            // Add column to columnModel
            component.addToColumnModel(column);
            // Column has been added
            added = true;
          }
          return added;
        };

        /**
         * Calculate totals
         */
        component.calculateTotals = function () {
          if (component.controller.showTotals) {
            // Get the rows that can be summarized
            let  summarizedCols = {};
            let  footerData = {};
            let  componentData = {};
            // Calculate columns to summarize
            _.each(component.controller.columnModel, function (column, columnIndex) {
              let  columnName = column.name;
              let  summaryType = column.summaryType || null;
              if (summaryType !== null) {
                summarizedCols[columnName] = {
                  name: columnName,
                  type: summaryType,
                  value: 0,
                  index: columnIndex,
                  component: column.component
                };
              }

              // Store as footer data
              if (column.component) {
                componentData[columnName] = null;
              } else {
                footerData[columnName] = "";
              }
            });
            // Summarize totals
            summarizeTotals(component, summarizedCols, componentData, footerData);

            // Store footer
            component.model.footer = componentData;
            // Broadcast footer change
            Utilities.publishFromScope("footer-changed", {
              footer: component.model.footer
            }, component.scope);
          }
        };

        component.initHelpColumns = function() {
          component.controller.columnModel.forEach(column => {
            if ("help" in column) {
              initHelpColumn(component, column);
            }
          });
        };

        component.deferRowsRendered = function(onDefer = null) {
          let deferred = Utilities.q.defer();
          let startTime = new Date();
          let listener = component.scope.$on("rows-rendered", function (event, parameters) {
            if (parameters.grid === component.id) {
              // Remove listener
              listener();
              // Resolve promise
              if (onDefer) {
                onDefer(deferred);
              } else {
                deferred.resolve();
              }

              let dateDiff = (new Date() - startTime) / 1000;
              $log.debug("[GRID ROWS] Grid rows have been COMPILED", {grid: component.id, compilationTime: dateDiff + "s"});
            }
          });
          // Retrieve new id
          return deferred.promise;
        };

        /** ******************************************************************* */
        /* EVENTS */
        /** ******************************************************************* */

        // Map grid actions
        GridEvents.mapCommonActions(component);
        /** ******************************************************************* */
        /* API METHODS */
        /** ******************************************************************* */

        /**
         * Apply dependency target
         *
         * @param {Object} dependency Dependency
         * @param {String} value Target value
         * @param {Boolean} update Condition updated
         */
        component.api.applyDependency = function (dependency, value, update) {
          let  target = dependency.target || "none";
          switch (target) {
            // Show column if update
            case "showColumn":
              if (update) {
                component.showColumn(dependency.column);
              } else {
                component.hideColumn(dependency.column);
              }
              break;
            // Hide column if update
            case "hideColumn":
              if (update) {
                component.hideColumn(dependency.column);
              } else {
                component.showColumn(dependency.column);
              }
              break;
            // Change column label
            case "columnLabel":
              component.changeColumnLabel(dependency.column, value);
              break;
            default:
              break;
          }
        };

        /**
         * API link to finish loading
         */
        component.api.endLoad = function () {
          Control.changeControllerAttribute(component.address, {
            loading: false
          });
          Utilities.publishFromScope("compiled", component.address.view, component.scope);
        };

        /**
         * API link to update the selected values
         *
         * @param {Object} selectedValues
         */
        component.api.updateSelectedValue = function (selectedValues) {
          // Store value list
          let  selectedList = [];
          _.each(selectedValues, function (selected) {
            if (typeof selected === "object" && "value" in selected) {
              selectedList.push(selected.value);
            } else {
              selectedList.push(selected);
            }
          });

          // Set selection
          component.setSelection(selectedList);
        };

        // Initialize as multioperation
        let  initializationFlag = true;
        if (this.isMultioperation()) {
          component.gridMultioperation = new GridMultioperation(this.component);
          initializationFlag = component.gridMultioperation.init();
          // Initialize as editable
        } else if (this.isEditable()) {
          component.gridEditable = new GridEditable(this.component);
          initializationFlag = component.gridEditable.init();
          // Initialize with component management
        } else {
          component.gridComponents = new GridComponents(this.component);
          initializationFlag = component.gridComponents.init();
        }

        return initializationFlag;
      },
      /**
       * Check if column model has components
       *
       * @returns {Boolean}
       */
      hasComponents: function () {
        let  hasComponents = false;
        _.each(this.component.controller.columnModel, function (column) {
          if ("component" in column) {
            hasComponents = true;
          }
        });
        return hasComponents;
      },
      /**
       * Check if grid is multioperation
       *
       * @returns {Boolean}
       */
      isMultioperation: function () {
        return this.component.controller.multioperation;
      },
      /**
       * Check if grid is editable
       *
       * @returns {Boolean}
       */
      isEditable: function () {
        return this.component.controller.editable;
      },
      /**
       * Check if grid is editable
       *
       * @returns {Boolean}
       */
      hasFrozenColumns: function () {
        let  hasFrozen = false;
        _.each(this.component.controller.columnModel, function (column) {
          if (column.frozen) {
            hasFrozen = true;
          }
        });
        return hasFrozen || this.component.isTree;
      }
    };
    return GridCommons;
  }]);
