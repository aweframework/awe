// Type-level assertions for utilities/testPassThrough, checked by `npm run typecheck` (not run by Jest)
import type {ReactElement} from "react";
import {
  alertType,
  autoCompletePassThrough,
  bodyCellPassThrough,
  buttonGroupPassThrough,
  calendarPassThrough,
  contextMenuLinkTemplate,
  contextMenuPassThrough,
  dialogPassThrough,
  dropdownPassThrough,
  gridPassThrough,
  headerCellPassThrough,
  isDaySelected,
  menubarPassThrough,
  multiSelectPassThrough,
  paginatorPassThrough,
  panelMenuPassThrough,
  popoverPassThrough,
  selectValueTemplate,
  selectionColumnPassThrough,
  sidePanelPassThrough,
  tabMenuPassThrough,
  toastPassThrough
} from "../../../../src/utilities/testPassThrough";
import type {TestHook} from "../../../../src/utilities/testIds";

// A day is selected by a date, a list of dates or nothing
const selected: boolean = isDaySelected(new Date(), {day: 1, month: 0, year: 2024});
const none: boolean = isDaySelected(null, {day: 1, month: 0, year: 2024});
// @ts-expect-error the day has a day, a month and a year
isDaySelected(new Date(), {day: 1});

// The sections are hooks, or functions of the options PrimeReact gives
const calendar = calendarPassThrough("Cal");
const panel: TestHook = calendar.panel;
const day: TestHook = calendar.day({props: {value: [new Date()]}, context: {date: {day: 1, month: 0, year: 2024, selectable: true}, otherMonth: false}});
const month: TestHook = calendar.month({context: {selected: true, disabled: false}});
// @ts-expect-error a day needs its context
calendar.day({});
// @ts-expect-error the owner is text
calendarPassThrough(1);

// The templates answer with elements
const value: ReactElement = selectValueTemplate({label: "A"}, {placeholder: "Pick"});
const nothing: ReactElement = selectValueTemplate(null, undefined);
const link: ReactElement = contextMenuLinkTemplate({optionId: "edit", disabled: false}, {element: value});

// Selects, suggests and grids
const dropdown: TestHook = dropdownPassThrough("Sel").root;
const item: TestHook = dropdownPassThrough("Sel").item({context: {selected: true}});
const multi: TestHook = multiSelectPassThrough("Sel").token;
const suggest: TestHook = autoCompletePassThrough("Sug", true).loadingIcon;
const grid = gridPassThrough({gridId: "Grd", tree: false, getRowId: () => 3, isRowEditing: () => true});
const row: TestHook | undefined = grid.bodyRow?.({context: {selected: true}});
// @ts-expect-error the grid needs its identifier
gridPassThrough({tree: true});
// @ts-expect-error the row identifier comes from a function
gridPassThrough({gridId: "Grd", getRowId: 3});
const paginator = paginatorPassThrough();
const previous: TestHook = paginator.prevPageButton({context: {disabled: true}});
const header: TestHook = headerCellPassThrough("name").headerCell;
const body: TestHook = bodyCellPassThrough("name").bodyCell({state: {editingRowData: {id: 1}}, context: {selected: false}});
const checkbox: TestHook = selectionColumnPassThrough().rowCheckbox({context: {checked: true}}).root;

// Tabs, menus and messages
const tab: TestHook = tabMenuPassThrough([{value: "one"}], 0, false, ["one"]).menuitem({context: {index: 0}});
const menu = panelMenuPassThrough([{name: "first", title: "First", active: true}]);
const option: TestHook = menu.panel({context: {index: 0, active: true}});
const bar = menubarPassThrough();
const barOption: TestHook = bar.menuitem({context: {active: false, item: {item: {name: "top"}}}});
const type: string = alertType("warn");
const popover: {root: TestHook} = popoverPassThrough("warn", "Txt");
const toast = toastPassThrough();
const alert: TestHook = toast.message({state: {messages: [{message: {severity: "error"}}]}, index: 0});
// @ts-expect-error the severity is text
alertType(1);

// Dialogs and menus of a grid
const dialog = dialogPassThrough("Dlg", false, "report");
const side = sidePanelPassThrough("Side");
const context = contextMenuPassThrough();
const optionHook: TestHook = buttonGroupPassThrough.button({context: {selected: true, option: {value: "A"}}});
// @ts-expect-error the dialog needs its owner
dialogPassThrough();

export {
  selected, none, panel, day, month, value, nothing, link, dropdown, item, multi, suggest, row, previous, header, body,
  checkbox, tab, option, barOption, type, popover, alert, dialog, side, context, optionHook
};
