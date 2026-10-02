import React from "react";
import { testHook, TestIds } from "./testIds";

/**
 * PrimeReact pass-through ("pt") builders that add the test hooks (data-testid) to the parts of the components the
 * client renders. They use the official extension point of the library, so the markup of PrimeReact is never patched.
 * Overlays that PrimeReact renders outside of the component (panels, calendars) carry the owner component id.
 * @category Utilities
 * @namespace TestPassThrough
 */

/**
 * Check if a calendar value (date, date range or list of dates) selects a day
 * @param {Date|Date[]|null} value Calendar value
 * @param {{day: number, month: number, year: number}} day Calendar day
 * @returns {boolean} The day is selected
 */
export function isDaySelected(value, day) {
  const values = Array.isArray(value) ? value : [value];
  return values.some(date => date instanceof Date
    && date.getDate() === day.day && date.getMonth() === day.month && date.getFullYear() === day.year);
}

/**
 * Pass-through of a Calendar
 * @param {string} owner Id of the component that owns the calendar overlay
 * @returns {object} Calendar pt
 */
export function calendarPassThrough(owner) {
  return {
    input: { root: testHook(TestIds.criterionInput) },
    panel: testHook(TestIds.datepicker, { owner }),
    day: ({ props, context }) => testHook(TestIds.datepickerDay, {
      owner,
      selected: isDaySelected(props?.value, context.date),
      disabled: !context.date.selectable,
      outsideMonth: !!context.otherMonth
    }),
    month: ({ context }) => testHook(TestIds.datepickerMonth, {
      owner, selected: context.selected, disabled: context.disabled
    }),
    year: ({ context }) => testHook(TestIds.datepickerYear, {
      owner, selected: context.selected, disabled: context.disabled
    })
  };
}

/**
 * Option (item) of an overlay list of a select or a suggest
 * @param {string} owner Id of the component that owns the overlay
 * @returns {function} pt function
 */
const optionPassThrough = (owner) => ({ context }) => testHook(TestIds.selectOption, {
  owner, selected: context?.selected, disabled: context?.disabled
});

/**
 * Value template of a Dropdown: the chosen label (or the placeholder) goes in an element with the test hook, because
 * PrimeReact renders the same pass-through section for the label and for its hidden input.
 * @param {object} option Chosen option
 * @param {object} props Dropdown props
 * @returns {object} Value element
 */
export function selectValueTemplate(option, props) {
  return <span {...testHook(TestIds.selectValue)}>{option ? option.label : (props?.placeholder || "\u00a0")}</span>;
}

/**
 * Pass-through of a Dropdown
 * @param {string} owner Id of the component that owns the dropdown panel
 * @returns {object} Dropdown pt
 */
export function dropdownPassThrough(owner) {
  return {
    root: testHook(TestIds.select),
    panel: testHook(TestIds.selectDropdown, { owner }),
    filterInput: testHook(TestIds.selectSearch),
    item: optionPassThrough(owner)
  };
}

/**
 * Pass-through of a MultiSelect
 * @param {string} owner Id of the component that owns the panel
 * @returns {object} MultiSelect pt
 */
export function multiSelectPassThrough(owner) {
  return {
    root: testHook(TestIds.select),
    token: testHook(TestIds.selectChoice),
    removeTokenIcon: testHook(TestIds.selectChoiceClose),
    panel: testHook(TestIds.selectDropdown, { owner }),
    filterInput: { root: testHook(TestIds.selectSearch) },
    item: optionPassThrough(owner)
  };
}

/**
 * Pass-through of an AutoComplete (single or multiple)
 * @param {string} owner Id of the component that owns the suggestions panel
 * @param {boolean} [multiple] The suggest has multiple choices (its input is rendered by the component itself)
 * @returns {object} AutoComplete pt
 */
export function autoCompletePassThrough(owner, multiple = false) {
  return {
    root: testHook(TestIds.select),
    input: multiple ? testHook(TestIds.selectSearch) : { root: testHook(TestIds.selectSearch) },
    token: testHook(TestIds.selectChoice),
    removeTokenIcon: testHook(TestIds.selectChoiceClose),
    loadingIcon: testHook(TestIds.loader),
    panel: testHook(TestIds.selectDropdown, { owner }),
    item: optionPassThrough(owner)
  };
}

/**
 * Pass-through of the pagination of a grid
 * @returns {object} Paginator pt
 */
export function paginatorPassThrough() {
  return {
    root: testHook(TestIds.gridPagination),
    prevPageButton: ({ context }) => testHook(TestIds.gridPagePrevious, { disabled: context?.disabled }),
    nextPageButton: ({ context }) => testHook(TestIds.gridPageNext, { disabled: context?.disabled }),
    RPPDropdown: { root: testHook(TestIds.gridPageSize) }
  };
}

/**
 * Pass-through of a grid (DataTable) or a tree grid (TreeTable)
 * @param {object} options Grid options
 * @param {string} options.gridId Grid identifier
 * @param {boolean} [options.tree] The grid is a tree grid (the identifier is rendered as "tree-grid-id")
 * @param {function} [options.getRowId] Function that returns the id of the row PrimeReact is rendering
 * @param {function} [options.isRowEditing] Function that tells if the row PrimeReact is rendering is being edited
 * @returns {object} DataTable or TreeTable pt
 */
export function gridPassThrough({ gridId, tree = false, getRowId, isRowEditing }) {
  const pt = {
    root: testHook(TestIds.grid, { attributes: { [tree ? "tree-grid-id" : "grid-id"]: gridId } }),
    paginator: paginatorPassThrough()
  };
  // A scrollable TreeTable renders its body in the "scrollableBody" part
  pt[tree ? "scrollableBody" : "wrapper"] = testHook(TestIds.gridViewport, { container: "body" });
  if (!tree) {
    // A TreeTable has no pass-through for its rows
    pt.bodyRow = ({ context }) => {
      const rowId = getRowId?.();
      return testHook(TestIds.gridRow, {
        selected: context.selected,
        editing: isRowEditing ? !!isRowEditing() : undefined,
        attributes: rowId !== undefined ? { "row-id": rowId } : {}
      });
    };
  }
  return pt;
}

/**
 * Pass-through of the header cell of a column
 * @param {string} columnId Column identifier
 * @returns {object} Column pt
 */
export function headerCellPassThrough(columnId) {
  return { headerCell: testHook(TestIds.gridHeaderCell, { attributes: { "column-id": columnId } }) };
}

/**
 * Pass-through of the body cells of a column
 * @param {string} columnId Column identifier
 * @returns {object} Column pt
 */
export function bodyCellPassThrough(columnId) {
  return {
    bodyCell: ({ state, context }) => {
      const rowId = state?.editingRowData?.id;
      return testHook(TestIds.gridCell, {
        selected: context?.selected,
        attributes: { "column-id": columnId, ...(rowId !== undefined ? { "row-id": rowId } : {}) }
      });
    }
  };
}

/**
 * Pass-through of the selection column (checkboxes of the header and the rows)
 * @returns {object} Column pt
 */
export function selectionColumnPassThrough() {
  return {
    headerCheckbox: ({ context }) => ({ root: testHook(TestIds.gridHeaderCheckbox, { selected: context?.checked }) }),
    rowCheckbox: ({ context }) => ({ root: testHook(TestIds.gridRowCheckbox, { selected: context?.checked }) })
  };
}

/**
 * Pass-through of a tab menu: the tab list and one tab per value (the active one carries data-active)
 * @param {object[]} values Values of the tab component
 * @param {number} activeIndex Index of the active tab
 * @param {boolean} disabled The tab list is disabled
 * @returns {object} TabMenu pt
 */
export function tabMenuPassThrough(values, activeIndex, disabled) {
  return {
    menu: testHook(TestIds.tabList, { disabled: !!disabled }),
    menuitem: ({ context }) => testHook(TestIds.tab, {
      active: context.index === activeIndex,
      attributes: { "option-id": values[context.index]?.value }
    })
  };
}

/**
 * Attributes that identify an option of the application menu
 * @param {object} item Menu item (with the option "name")
 * @returns {object} Attributes
 */
const optionName = (item) => (item?.name ? { "option-name": item.name } : {});

/**
 * Pass-through of the vertical application menu (PanelMenu). The top level options are panels, the nested ones items.
 * @param {object[]} model Menu items of the first level
 * @returns {object} PanelMenu pt
 */
export function panelMenuPassThrough(model) {
  const link = (item) => testHook(TestIds.menuLink, { attributes: item?.name ? { name: item.name } : {} });
  return {
    root: testHook(TestIds.menu),
    panel: ({ context }) => testHook(TestIds.menuOption, {
      active: !!model[context.index]?.active,
      open: context.active,
      attributes: optionName(model[context.index])
    }),
    headerAction: ({ context }) => link(model[context.index]),
    toggleableContent: testHook(TestIds.menuDropdown),
    menu: testHook(TestIds.menuSubmenu),
    menuitem: ({ context }) => testHook(TestIds.menuOption, {
      active: !!context.item?.item?.active,
      open: context.active,
      attributes: optionName(context.item?.item)
    }),
    action: ({ context }) => link(context.item?.item)
  };
}

/**
 * Pass-through of the horizontal application menu (Menubar)
 * @returns {object} Menubar pt
 */
export function menubarPassThrough() {
  const item = ({ context }) => context?.item?.item;
  return {
    root: testHook(TestIds.menu),
    menuitem: (options) => testHook(TestIds.menuOption, {
      active: !!item(options)?.active,
      open: options.context?.active,
      attributes: optionName(item(options))
    }),
    action: (options) => testHook(TestIds.menuLink, { attributes: item(options)?.name ? { name: item(options).name } : {} }),
    submenu: testHook(TestIds.menuSubmenu)
  };
}

/**
 * Message severities of PrimeReact, mapped to the type names of the test vocabulary
 */
const ALERT_TYPES = { success: "success", info: "info", warn: "warning", error: "danger" };

/**
 * Pass-through of the toast that shows the messages (alerts)
 * @returns {object} Toast pt
 */
export function toastPassThrough() {
  return {
    message: ({ state, index }) => {
      const severity = state?.messages?.[index]?.message?.severity;
      return testHook(TestIds.alert, { type: ALERT_TYPES[severity] ?? severity });
    },
    summary: testHook(TestIds.alertTitle),
    detail: testHook(TestIds.alertMessage),
    closeButton: testHook(TestIds.alertClose)
  };
}

/**
 * Pass-through of a modal dialog
 * @param {string} owner Id of the dialog component
 * @returns {object} Dialog pt
 */
export function dialogPassThrough(owner) {
  return {
    root: testHook(TestIds.dialog, { owner }),
    closeButton: testHook(TestIds.dialogClose)
  };
}

/**
 * Pass-through of the context menu of a grid. PrimeReact does not expose the item to the pass-through of an option,
 * so the link (rendered through the item template) carries the "option-id".
 * @returns {object} ContextMenu pt
 */
export function contextMenuPassThrough() {
  return {
    root: testHook(TestIds.contextMenu),
    menuitem: testHook(TestIds.contextMenuOption),
    submenu: testHook(TestIds.contextSubmenu)
  };
}

/**
 * Item template of the context menu: the link of the item carries the test hook
 * @param {object} item Menu item
 * @param {object} options Template options (with the default element of the link)
 * @returns {object} Link element
 */
export function contextMenuLinkTemplate(item, options) {
  return React.cloneElement(options.element, testHook(TestIds.contextMenuLink, {
    disabled: !!item.disabled,
    attributes: { "option-id": item.optionId }
  }));
}
