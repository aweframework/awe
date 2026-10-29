import React from "react";
import type { FocusEvent, KeyboardEvent, ReactElement, ReactNode } from "react";
import { testHook, TestAttributes, TestIds } from "./testIds";
import { createEnterHandler } from "./keyboard";
import type { TestHook } from "./testIds";

/**
 * PrimeReact pass-through ("pt") builders that add the test hooks (data-testid) to the parts of the components the
 * client renders. They use the official extension point of the library, so the markup of PrimeReact is never patched.
 * Overlays that PrimeReact renders outside of the component (panels, calendars) carry the owner component id.
 * @category Utilities
 * @namespace TestPassThrough
 */

/**
 * Day of a calendar, as PrimeReact gives it to the pass-through
 */
export interface CalendarDay {
  day: number;
  month: number;
  year: number;
  selectable?: boolean;
}

/**
 * Options of the pass-through of an option of an overlay list
 */
export interface OptionOptions {
  context?: { selected?: boolean; disabled?: boolean };
}

/**
 * Options of the pass-through of the day of a calendar
 */
export interface DayOptions {
  props?: { value?: Date | Date[] | null };
  context: { date: CalendarDay; otherMonth?: boolean };
}

/**
 * Options of the pass-through of the month or the year of a calendar
 */
export interface MonthYearOptions {
  context: { selected?: boolean; disabled?: boolean };
}

/**
 * Pass-through of a Calendar
 */
export interface CalendarPassThrough {
  input: {
    root: TestHook & {
      onKeyDownCapture: (event: KeyboardEvent<HTMLInputElement>) => void;
      onKeyDown?: (event: KeyboardEvent<HTMLInputElement>) => unknown;
    };
  };
  panel: TestHook & {
    onKeyDown: (event: KeyboardEvent<HTMLElement>) => unknown;
    onFocus: (event: FocusEvent<HTMLElement>) => void;
  };
  day: (options: DayOptions) => TestHook;
  month: (options: MonthYearOptions) => TestHook;
  year: (options: MonthYearOptions) => TestHook;
}

/**
 * Pass-through of a Dropdown
 */
export interface DropdownPassThrough {
  root: TestHook;
  trigger: TestHook;
  panel: TestHook;
  filterInput: TestHook;
  item: (options: OptionOptions) => TestHook;
}

/**
 * Pass-through of a MultiSelect
 */
export interface MultiSelectPassThrough {
  root: TestHook;
  token: TestHook;
  removeTokenIcon: TestHook;
  trigger: TestHook;
  panel: TestHook;
  filterInput: { root: TestHook };
  item: (options: OptionOptions) => TestHook;
}

/**
 * Pass-through of an AutoComplete
 */
export interface AutoCompletePassThrough {
  root: TestHook;
  input: TestHook | { root: TestHook };
  token: TestHook;
  removeTokenIcon: TestHook;
  loadingIcon: TestHook;
  panel: TestHook & { onMouseDown: (event: { preventDefault: () => void }) => void };
  item: (options: OptionOptions) => TestHook;
}

/**
 * Options of the pass-through of a button of the paginator
 */
export interface DisabledContextOptions {
  context?: { disabled?: boolean };
}

/**
 * Pass-through of the pagination of a grid
 */
export interface PaginatorPassThrough {
  root: TestHook;
  current: TestHook;
  prevPageButton: (options: DisabledContextOptions) => TestHook;
  nextPageButton: (options: DisabledContextOptions) => TestHook;
  RPPDropdown: { root: (options: { props?: { value?: unknown } }) => TestHook };
}

/**
 * Options of the pass-through of a row of a grid
 */
export interface RowOptions {
  context: { selected?: boolean };
}

/**
 * Options of a grid pass-through
 */
export interface GridPassThroughOptions {
  gridId: string;
  tree?: boolean;
  getRowId?: () => unknown;
  isRowEditing?: () => unknown;
  isRowDeleted?: () => unknown;
  getRowProps?: (rowId: unknown) => Record<string, unknown>;
  getDisplayRowHook?: () => TestHook | null | undefined;
}

/**
 * Pass-through of a grid (DataTable) or a tree grid (TreeTable): a DataTable has a "wrapper" and "bodyRow", a TreeTable
 * has a "scrollableBody" and "row"
 */
export interface GridPassThrough {
  root: TestHook;
  paginator: PaginatorPassThrough;
  wrapper?: TestHook;
  scrollableBody?: TestHook;
  bodyRow?: (options: RowOptions) => TestHook;
  row?: (options: RowOptions) => TestHook;
}

/**
 * Options of the pass-through of the body cell of a column
 */
export interface BodyCellOptions {
  state?: { editingRowData?: { id?: unknown } };
  context?: { selected?: boolean };
}

/**
 * Options of the pass-through of a checkbox of the selection column
 */
export interface CheckboxOptions {
  context?: { checked?: boolean };
}

/**
 * Value of a tab component, as far as the pass-through reads it
 */
export interface TabValue {
  value?: string;
}

/**
 * Menu item, as far as the pass-through reads it
 */
export interface MenuItem {
  name?: string;
  title?: string;
  label?: string;
  active?: boolean;
  divider?: boolean;
}

/**
 * Options of the pass-through of an option of the horizontal menu: PrimeReact gives the item inside another one
 */
export interface MenuOptions {
  context?: { active?: boolean; item?: { item?: MenuItem } };
}

/**
 * Options of the pass-through of an option of the vertical menu
 */
export interface PanelMenuOptions {
  context: { index: number; active?: boolean; item?: { item?: MenuItem } };
}

/**
 * Pass-through of the vertical application menu (PanelMenu)
 */
export interface PanelMenuPassThrough {
  root: TestHook;
  panel: (options: PanelMenuOptions) => TestHook;
  header: (options: PanelMenuOptions) => TestHook;
  headerAction: (options: PanelMenuOptions) => TestHook;
  toggleableContent: (options?: Partial<PanelMenuOptions>) => TestHook;
  menu: TestHook;
  menuitem: (options: PanelMenuOptions) => TestHook;
  action: (options: PanelMenuOptions) => TestHook;
}

/**
 * Options of the pass-through of the message of a toast
 */
export interface ToastMessageOptions {
  state?: { messages?: { message?: { severity?: string } }[] };
  index?: number;
}

/**
 * Check if a calendar value (date, date range or list of dates) selects a day
 * @param {Date|Date[]|null} value Calendar value
 * @param {{day: number, month: number, year: number}} day Calendar day
 * @returns {boolean} The day is selected
 */
export function isDaySelected(value: Date | Date[] | null | undefined, day: CalendarDay): boolean {
  const values = Array.isArray(value) ? value : [value];
  return values.some((date) => date instanceof Date
    && date.getDate() === day.day && date.getMonth() === day.month && date.getFullYear() === day.year);
}

/**
 * Enter on a part of the calendar panel (a day, a month or a year) chooses it and stays in the panel: it must not reach
 * the ancestors, as a grid that saves the row with Enter (the panel is a portal, but React bubbles through its owner).
 * The buttons of the panel are left alone: cancelling Enter on a button would stop its click.
 */
const panelEnterHandler = createEnterHandler<KeyboardEvent<HTMLElement>>(undefined, {
  when: (event) => !(event.target as HTMLElement).closest?.("button")
});

/**
 * Inputs of a calendar where the down arrow has been pressed: the focus goes into their panel on purpose
 */
const keyboardNavigation = new WeakSet<EventTarget>();

/**
 * Remember that the down arrow is pressed on the input of a calendar. It runs in the capture phase, before PrimeReact
 * handles the key and moves the focus into the panel.
 * @param {KeyboardEvent} event Key down event of the input
 */
const markKeyboardNavigation = (event: KeyboardEvent<HTMLInputElement>): void => {
  if (event.key === "ArrowDown") {
    keyboardNavigation.add(event.currentTarget);
  }
};

/**
 * PrimeReact moves the focus to the first day of the panel whenever it opens, also when it opens because the input got
 * the focus: what the user types then is lost. The focus goes back to the input unless the user went into the panel
 * with the down arrow, so the keyboard navigation of the calendar is kept.
 * @param {FocusEvent} event Focus event of the panel
 */
const keepFocusOnInput = (event: FocusEvent<HTMLElement>): void => {
  const input = event.relatedTarget;
  if (input instanceof HTMLInputElement && !event.currentTarget.contains(input) && !keyboardNavigation.delete(input)) {
    input.focus();
  }
};

/**
 * Pass-through of a Calendar. PrimeReact does not forward onKeyDown to the input of a Calendar, so the key handler of
 * the criterion goes through the pass-through of the input.
 * @param {string} owner Id of the component that owns the calendar overlay
 * @param {function(KeyboardEvent)} [onKeyDown] Key down handler of the input
 * @param {object} [inputHook] Test hook of the input (the criterion input by default)
 * @param {object} [inputAttributes] Extra attributes of the input (as its aria-required)
 * @returns {object} Calendar pt
 */
export function calendarPassThrough(
  owner: string,
  onKeyDown?: (event: KeyboardEvent<HTMLInputElement>) => unknown,
  inputHook: TestHook = testHook(TestIds.criterionInput),
  inputAttributes: Record<string, string> = {}
): CalendarPassThrough {
  return {
    input: { root: { ...inputHook, ...inputAttributes, onKeyDownCapture: markKeyboardNavigation, ...(onKeyDown && { onKeyDown }) } },
    panel: { ...testHook(TestIds.datepicker, { owner }), onKeyDown: panelEnterHandler, onFocus: keepFocusOnInput },
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
 * Keep the focus where it is when the mouse is pressed on an overlay (the default action of the press moves the focus)
 * @param {Event} event Mouse down event
 */
const keepInputFocus = (event: { preventDefault: () => void }): void => event.preventDefault();

/**
 * Option (item) of an overlay list of a select or a suggest
 * @param {string} owner Id of the component that owns the overlay
 * @returns {function} pt function
 */
const optionPassThrough = (owner: string) => ({ context }: OptionOptions): TestHook => testHook(TestIds.selectOption, {
  owner, selected: context?.selected, disabled: context?.disabled
});

/**
 * Value template of a Dropdown: the chosen label (or the placeholder) goes in an element with the test hook, because
 * PrimeReact renders the same pass-through section for the label and for its hidden input.
 * @param {object} option Chosen option
 * @param {object} props Dropdown props
 * @returns {object} Value element
 */
export function selectValueTemplate(
  option: { label?: ReactNode } | null | undefined,
  props?: { placeholder?: string }
): ReactElement {
  return <span {...testHook(TestIds.selectValue)}>{option ? option.label : (props?.placeholder || "\u00a0")}</span>;
}

/**
 * Pass-through of a Dropdown
 * @param {string} owner Id of the component that owns the dropdown panel
 * @returns {object} Dropdown pt
 */
export function dropdownPassThrough(owner: string): DropdownPassThrough {
  return {
    root: testHook(TestIds.select),
    trigger: testHook(TestIds.selectTrigger),
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
export function multiSelectPassThrough(owner: string): MultiSelectPassThrough {
  return {
    root: testHook(TestIds.select),
    token: testHook(TestIds.selectChoice),
    removeTokenIcon: testHook(TestIds.selectChoiceClose),
    trigger: testHook(TestIds.selectTrigger),
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
export function autoCompletePassThrough(owner: string, multiple = false): AutoCompletePassThrough {
  return {
    root: testHook(TestIds.select),
    input: multiple ? testHook(TestIds.selectSearch) : { root: testHook(TestIds.selectSearch) },
    token: testHook(TestIds.selectChoice),
    removeTokenIcon: testHook(TestIds.selectChoiceClose),
    loadingIcon: testHook(TestIds.loader),
    // Pressing the mouse on the list must not take the focus away from the input: the list is aligned again when the
    // input loses the focus (it can then flip from above to below it) and the click would land outside the result
    panel: { ...testHook(TestIds.selectDropdown, { owner }), onMouseDown: keepInputFocus },
    item: optionPassThrough(owner)
  };
}

/**
 * Pass-through of the pagination of a grid
 * @returns {object} Paginator pt
 */
export function paginatorPassThrough(): PaginatorPassThrough {
  return {
    root: testHook(TestIds.gridPagination),
    current: testHook(TestIds.gridPaginationInfo),
    prevPageButton: ({ context }) => testHook(TestIds.gridPagePrevious, { disabled: context?.disabled }),
    nextPageButton: ({ context }) => testHook(TestIds.gridPageNext, { disabled: context?.disabled }),
    // The text of the dropdown repeats the page size (its hidden native selector also holds it): expose it as a value
    RPPDropdown: { root: ({ props }) => testHook(TestIds.gridPageSize, { attributes: { [TestAttributes.value]: props?.value } }) }
  };
}

/**
 * Pass-through of a grid (DataTable) or a tree grid (TreeTable)
 * @param {object} options Grid options
 * @param {string} options.gridId Grid identifier
 * @param {boolean} [options.tree] The grid is a tree grid (the identifier is rendered as "tree-grid-id")
 * @param {function} [options.getRowId] Function that returns the id of the row PrimeReact is rendering (set by its "rowClassName")
 * @param {function} [options.isRowEditing] Function that tells if the row PrimeReact is rendering is being edited
 * @param {function} [options.isRowDeleted] Function that tells if the row PrimeReact is rendering is marked to be deleted
 * @param {function} [options.getRowProps] Function that returns extra props (style class, drag events) for the row with
 * the identifier it receives (DataTable only)
 * @param {function} [options.getDisplayRowHook] Function that returns the hook of the row PrimeReact is rendering when it
 * is a group or subtotal row of a grouped grid (set by its "rowClassName"), or nothing for a data row
 * @returns {object} DataTable or TreeTable pt
 */
export function gridPassThrough({ gridId, tree = false, getRowId, isRowEditing, isRowDeleted, getRowProps, getDisplayRowHook }: GridPassThroughOptions): GridPassThrough {
  const pt: GridPassThrough = {
    root: testHook(TestIds.grid, { attributes: { [tree ? "tree-grid-id" : "grid-id"]: gridId } }),
    paginator: paginatorPassThrough()
  };
  // A scrollable TreeTable renders its body in the "scrollableBody" part
  pt[tree ? "scrollableBody" : "wrapper"] = testHook(TestIds.gridViewport, { container: "body" });
  // The rows are "bodyRow" in a DataTable and "row" in a TreeTable
  pt[tree ? "row" : "bodyRow"] = ({ context }) => {
    // Group and subtotal rows (#833) are not data rows: they have their own hook and no row-id
    const displayRowHook = getDisplayRowHook?.();
    if (displayRowHook) {
      return displayRowHook;
    }
    const rowId = getRowId?.();
    return {
      ...testHook(TestIds.gridRow, {
        selected: context.selected,
        editing: isRowEditing ? !!isRowEditing() : undefined,
        deleted: isRowDeleted ? !!isRowDeleted() : undefined,
        attributes: rowId !== undefined && rowId !== null ? { "row-id": rowId } : {}
      }),
      ...getRowProps?.(rowId)
    };
  };
  return pt;
}

/**
 * Test hook of a group or subtotal row of a grouped grid (#833): its path and level, and whether a group is expanded
 * @param {object} row Display row: {$group: {path, level, expanded}} or {$subtotal: {path, level}}
 * @returns {object} Row hook
 */
export function displayRowHook(row: { $group?: { path: string; level: number; expanded: boolean };
  $subtotal?: { path: string; level: number } }): TestHook {
  const { path, level } = (row.$group ?? row.$subtotal) as { path: string; level: number };
  return testHook(row.$group ? TestIds.gridGroupRow : TestIds.gridSubtotalRow, {
    expanded: row.$group?.expanded,
    attributes: { "data-group-path": path, "data-group-level": String(level) }
  });
}

/**
 * Pass-through of the header cell of a column
 * @param {string} columnId Column identifier
 * @returns {object} Column pt
 */
export function headerCellPassThrough(columnId: string): { headerCell: TestHook } {
  return { headerCell: testHook(TestIds.gridHeaderCell, { attributes: { "column-id": columnId } }) };
}

/**
 * Pass-through of the body cells of a column
 * @param {string} columnId Column identifier
 * @returns {object} Column pt
 */
export function bodyCellPassThrough(columnId: string): { bodyCell: (options: BodyCellOptions) => TestHook } {
  return {
    bodyCell: ({ state, context }: BodyCellOptions) => {
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
export function selectionColumnPassThrough(): {
  headerCheckbox: (options: CheckboxOptions) => { root: TestHook };
  rowCheckbox: (options: CheckboxOptions) => { root: TestHook };
} {
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
 * @param {string[]} invalid Values of the tabs that hold invalid fields
 * @returns {object} TabMenu pt
 */
export function tabMenuPassThrough(
  values: TabValue[],
  activeIndex: number,
  disabled: boolean,
  invalid: readonly unknown[] = []
): { menu: TestHook; menuitem: (options: { context: { index: number } }) => TestHook } {
  return {
    menu: testHook(TestIds.tabList, { disabled: !!disabled }),
    menuitem: ({ context }) => ({
      ...testHook(TestIds.tab, {
        active: context.index === activeIndex,
        invalid: invalid.includes(values[context.index]?.value),
        attributes: { "option-id": values[context.index]?.value }
      }),
      className: invalid.includes(values[context.index]?.value) ? "p-tab-invalid" : undefined
    })
  };
}

/**
 * Attributes that identify an option of the application menu
 * @param {object} item Menu item (with the option "name")
 * @returns {object} Attributes
 */
const optionName = (item: MenuItem | undefined): Record<string, string> => (item?.name ? { "option-name": item.name } : {});

/**
 * Attributes of the link of a menu option: its name and, when it has a description, the hint
 * @param {object} item Menu item
 * @returns {object} Link attributes
 */
const optionLink = (item: MenuItem | undefined): Record<string, string> => ({
  ...(item?.name ? { name: item.name } : {}),
  ...(item?.title ? { title: item.title } : {})
});

/**
 * Pass-through of the vertical application menu (PanelMenu). The top level options are panels, the nested ones items.
 * @param {object[]} model Menu items of the first level
 * @returns {object} PanelMenu pt
 */
export function panelMenuPassThrough(model: MenuItem[]): PanelMenuPassThrough {
  const link = (item: MenuItem | undefined) => testHook(TestIds.menuLink, { attributes: optionLink(item) });
  return {
    root: testHook(TestIds.menu),
    // A divider line is a separator, not an option: it has no test hook
    panel: ({ context }) => model[context.index]?.divider ? { role: "separator" } : testHook(TestIds.menuOption, {
      active: !!model[context.index]?.active,
      open: context.active,
      attributes: optionName(model[context.index])
    }),
    // The empty header of a divider line is no button for assistive technologies
    header: ({ context }) => model[context.index]?.divider ? {
      role: undefined, "aria-expanded": undefined, "aria-controls": undefined, "aria-disabled": undefined,
      "aria-hidden": true
    } : {},
    headerAction: ({ context }) => link(model[context.index]),
    // The flyout of the collapsed rail is titled with its option (shell.css); the nested levels have no context
    toggleableContent: (options) => {
      const label = options?.context ? model[options.context.index]?.label : undefined;
      return { ...testHook(TestIds.menuDropdown), ...(label ? { "data-flyout-title": label } : {}) };
    },
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
export function menubarPassThrough(): {
  root: TestHook;
  menuitem: (options: MenuOptions) => TestHook;
  action: (options: MenuOptions) => TestHook;
  submenu: TestHook;
} {
  const item = ({ context }: MenuOptions) => context?.item?.item;
  return {
    root: testHook(TestIds.menu),
    menuitem: (options) => testHook(TestIds.menuOption, {
      active: !!item(options)?.active,
      open: options.context?.active,
      attributes: optionName(item(options))
    }),
    action: (options) => testHook(TestIds.menuLink, { attributes: optionLink(item(options)) }),
    submenu: testHook(TestIds.menuSubmenu)
  };
}

/**
 * Message severities of PrimeReact, mapped to the type names of the test vocabulary
 */
const ALERT_TYPES: Record<string, string> = { success: "success", info: "info", warn: "warning", error: "danger" };

/**
 * Type of a message in the test hooks and the styles, as the AngularJS client names it
 * @param {string} severity Message severity (success, info, warn, error)
 * @returns {string} Message type (success, info, warning, danger)
 */
export function alertType(severity: string): string {
  return ALERT_TYPES[severity] ?? severity;
}

/**
 * Pass-through of the popover of a target message
 * @param {string} severity Message severity
 * @param {string} [owner] Id of the component the message points at
 * @returns {object} OverlayPanel pt
 */
export function popoverPassThrough(severity: string, owner?: string): { root: TestHook } {
  return { root: testHook(TestIds.popover, { owner, type: alertType(severity) }) };
}

/**
 * Pass-through of the toast that shows the messages (alerts)
 * @returns {object} Toast pt
 */
export function toastPassThrough(): {
  message: (options: ToastMessageOptions) => TestHook;
  summary: TestHook;
  detail: TestHook;
  closeButton: TestHook;
} {
  return {
    message: ({ state, index }: ToastMessageOptions) => {
      const severity = state?.messages?.[index as number]?.message?.severity;
      return testHook(TestIds.alert, { type: alertType(severity as string) });
    },
    summary: testHook(TestIds.alertTitle),
    detail: testHook(TestIds.alertMessage),
    closeButton: testHook(TestIds.alertClose)
  };
}

/**
 * Pass-through of a modal dialog
 * @param {string} owner Id of the dialog component
 * @param {boolean} [open] Whether the dialog is open: the tests find the open dialog by it, as in the AngularJS client
 * @param {string} [view] View of the dialog: it is rendered outside its view, so it carries the view marker itself
 * @returns {object} Dialog pt
 */
export function dialogPassThrough(
  owner: string,
  open = true,
  view: string | undefined = undefined
): { root: TestHook; closeButton: TestHook } {
  return {
    root: { ...testHook(TestIds.dialog, { owner, open }), ...(view && { "view-id": view }) },
    closeButton: testHook(TestIds.dialogClose)
  };
}

/**
 * Pass-through of a side panel
 * @param {string} owner Id of the side panel component
 * @param {boolean} [open] Whether the side panel is open
 * @returns {object} Sidebar pt
 */
export function sidePanelPassThrough(owner: string, open = true): { root: TestHook; closeButton: TestHook } {
  return {
    root: testHook(TestIds.sidePanel, { owner, open }),
    closeButton: testHook(TestIds.sidePanelClose)
  };
}

/**
 * Pass-through of the context menu of a grid. PrimeReact does not expose the item to the pass-through of an option,
 * so the link (rendered through the item template) carries the "option-id".
 * @returns {object} ContextMenu pt
 */
export function contextMenuPassThrough(): { root: TestHook; menuitem: TestHook; submenu: TestHook } {
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
export function contextMenuLinkTemplate(
  item: { optionId?: string; disabled?: boolean },
  options: { element: ReactElement }
): ReactElement {
  return React.cloneElement(options.element, testHook(TestIds.contextMenuLink, {
    disabled: !!item.disabled,
    attributes: { "option-id": item.optionId }
  }));
}

/**
 * Pass-through of the button group of a button checkbox or a button radio: every option is the control of one choice,
 * it carries its value as "option-id" and tells if it is selected
 */
export const buttonGroupPassThrough = {
  button: ({ context }: { context: { selected?: boolean; option?: { value?: unknown } } }): TestHook => testHook(TestIds.criterionInput, {
    selected: context.selected,
    attributes: { "option-id": context.option?.value }
  })
};
