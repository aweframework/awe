import React from "react";
import {render} from "@testing-library/react";
import * as testPassThrough from "../../../src/utilities/testPassThrough";

// The builders take loose option objects (they are called by PrimeReact) and return pass-through objects whose parts
// are objects or functions: the characterisation reads them loosely, the typed contract is in types/testPassThroughTypes.ts
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Loose = (...args: any[]) => any;
const {
  alertType,
  autoCompletePassThrough,
  bodyCellPassThrough,
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
} = testPassThrough as unknown as Record<string, Loose>;
const buttonGroupPassThrough = (testPassThrough as unknown as {buttonGroupPassThrough: Record<string, Loose>}).buttonGroupPassThrough;

type Pt = Loose;
const call = (builder: unknown, options: Record<string, unknown>) => (builder as Pt)(options);

describe("awe-react-client/test/js/utilities/testPassThroughTest.tsx", () => {
  describe("isDaySelected", () => {
    const day = {day: 5, month: 2, year: 2024};

    it("matches a date, a list of dates and ignores other values", () => {
      expect(isDaySelected(new Date(2024, 2, 5), day)).toBe(true);
      expect(isDaySelected([new Date(2024, 2, 4), new Date(2024, 2, 5)], day)).toBe(true);
      expect(isDaySelected(new Date(2024, 2, 6), day)).toBe(false);
      expect(isDaySelected([null, new Date(2024, 2, 6)], day)).toBe(false);
      expect(isDaySelected(null, day)).toBe(false);
    });
  });

  describe("calendarPassThrough", () => {
    const pt = calendarPassThrough("calendar");

    it("hooks the input and the overlay", () => {
      expect(pt.input).toEqual({root: {"data-testid": "criterion-input"}});
      expect(pt.panel).toEqual({"data-testid": "datepicker", "data-testid-owner": "calendar"});
    });

    it("hooks a day with its state", () => {
      const context = {date: {day: 5, month: 2, year: 2024, selectable: false}, otherMonth: true};
      expect(call(pt.day, {props: {value: new Date(2024, 2, 5)}, context})).toEqual({
        "data-testid": "datepicker-day", "data-testid-owner": "calendar",
        "data-selected": "true", "data-disabled": "true", "data-outside-month": "true"
      });
      expect(call(pt.day, {context: {date: {day: 1, month: 0, year: 2024, selectable: true}}})).toEqual({
        "data-testid": "datepicker-day", "data-testid-owner": "calendar",
        "data-selected": "false", "data-disabled": "false", "data-outside-month": "false"
      });
    });

    it("hooks a month and a year", () => {
      const context = {selected: true, disabled: false};
      expect(call(pt.month, {context})).toEqual({
        "data-testid": "datepicker-month", "data-testid-owner": "calendar", "data-selected": "true", "data-disabled": "false"
      });
      expect(call(pt.year, {context})).toEqual({
        "data-testid": "datepicker-year", "data-testid-owner": "calendar", "data-selected": "true", "data-disabled": "false"
      });
    });
  });

  describe("selectValueTemplate", () => {
    it("renders the label of the option or the placeholder", () => {
      const {container, rerender} = render(<div>{selectValueTemplate({label: "Spain"}, {})}</div>);
      expect(container.querySelector("[data-testid=select-value]")?.textContent).toBe("Spain");
      rerender(<div>{selectValueTemplate(null, {placeholder: "Pick"})}</div>);
      expect(container.querySelector("[data-testid=select-value]")?.textContent).toBe("Pick");
      rerender(<div>{selectValueTemplate(undefined, undefined)}</div>);
      expect(container.querySelector("[data-testid=select-value]")?.textContent).toBe(" ");
    });
  });

  describe("selects and suggests", () => {
    it("hooks a dropdown", () => {
      const pt = dropdownPassThrough("sel");
      expect(pt.root).toEqual({"data-testid": "select"});
      expect(pt.trigger).toEqual({"data-testid": "select-trigger"});
      expect(pt.panel).toEqual({"data-testid": "select-dropdown", "data-testid-owner": "sel"});
      expect(pt.filterInput).toEqual({"data-testid": "select-search"});
      expect(call(pt.item, {context: {selected: true, disabled: false}})).toEqual({
        "data-testid": "select-option", "data-testid-owner": "sel", "data-selected": "true", "data-disabled": "false"
      });
      expect(call(pt.item, {})).toEqual({"data-testid": "select-option", "data-testid-owner": "sel"});
    });

    it("hooks a multiple select", () => {
      const pt = multiSelectPassThrough("sel");
      expect(pt.token).toEqual({"data-testid": "select-choice"});
      expect(pt.removeTokenIcon).toEqual({"data-testid": "select-choice-close"});
      expect(pt.filterInput).toEqual({root: {"data-testid": "select-search"}});
      expect(pt.panel).toEqual({"data-testid": "select-dropdown", "data-testid-owner": "sel"});
    });

    it("hooks a single and a multiple suggest", () => {
      const single = autoCompletePassThrough("sug");
      expect(single.input).toEqual({root: {"data-testid": "select-search"}});
      expect(single.loadingIcon).toEqual({"data-testid": "loader"});
      const multiple = autoCompletePassThrough("sug", true);
      expect(multiple.input).toEqual({"data-testid": "select-search"});
      const panel = multiple.panel as Record<string, unknown>;
      expect(panel["data-testid"]).toBe("select-dropdown");
      const event = {preventDefault: jest.fn()};
      (panel.onMouseDown as (event: unknown) => void)(event);
      expect(event.preventDefault).toHaveBeenCalledTimes(1);
    });
  });

  describe("grids", () => {
    it("hooks the paginator", () => {
      const pt = paginatorPassThrough();
      expect(pt.root).toEqual({"data-testid": "grid-pagination"});
      expect(call(pt.prevPageButton, {context: {disabled: true}})).toEqual({"data-testid": "grid-page-previous", "data-disabled": "true"});
      expect(call(pt.nextPageButton, {})).toEqual({"data-testid": "grid-page-next"});
      const dropdown = pt.RPPDropdown as {root: Pt};
      expect(dropdown.root({props: {value: 20}})).toEqual({"data-testid": "grid-page-size", "data-value": 20});
    });

    it("hooks a grid and its rows", () => {
      const pt = gridPassThrough({
        gridId: "GrdOrders", getRowId: () => 7, isRowEditing: () => true, isRowDeleted: () => false
      });
      expect(pt.root).toEqual({"data-testid": "grid", "grid-id": "GrdOrders"});
      expect(pt.wrapper).toEqual({"data-testid": "grid-viewport", "data-container": "body"});
      expect(call(pt.bodyRow, {context: {selected: false}})).toEqual({
        "data-testid": "grid-row", "data-selected": "false", "data-editing": "true", "data-deleted": "false", "row-id": 7
      });
    });

    it("hooks a tree grid and a row without identifier", () => {
      const pt = gridPassThrough({gridId: "Tree", tree: true});
      expect(pt.root).toEqual({"data-testid": "grid", "tree-grid-id": "Tree"});
      expect(pt.scrollableBody).toEqual({"data-testid": "grid-viewport", "data-container": "body"});
      expect(call(pt.row, {context: {selected: true}})).toEqual({"data-testid": "grid-row", "data-selected": "true"});
    });

    it("hooks the header, the cells and the selection of a column", () => {
      expect(headerCellPassThrough("name")).toEqual({headerCell: {"data-testid": "grid-header-cell", "column-id": "name"}});
      const body = bodyCellPassThrough("name");
      expect(call(body.bodyCell, {state: {editingRowData: {id: 3}}, context: {selected: true}})).toEqual({
        "data-testid": "grid-cell", "data-selected": "true", "column-id": "name", "row-id": 3
      });
      expect(call(body.bodyCell, {})).toEqual({"data-testid": "grid-cell", "column-id": "name"});
      const selection = selectionColumnPassThrough();
      expect(call(selection.headerCheckbox, {context: {checked: true}})).toEqual({
        root: {"data-testid": "grid-header-checkbox", "data-selected": "true"}
      });
      expect(call(selection.rowCheckbox, {})).toEqual({root: {"data-testid": "grid-row-checkbox"}});
    });
  });

  describe("tab menu", () => {
    it("marks the active and the invalid tabs", () => {
      const values = [{value: "one"}, {value: "two"}];
      const pt = tabMenuPassThrough(values, 1, true, ["one"]);
      expect(pt.menu).toEqual({"data-testid": "tab-list", "data-disabled": "true"});
      expect(call(pt.menuitem, {context: {index: 0}})).toEqual({
        "data-testid": "tab", "data-active": "false", "data-invalid": "true", "option-id": "one", className: "p-tab-invalid"
      });
      expect(call(pt.menuitem, {context: {index: 1}})).toEqual({
        "data-testid": "tab", "data-active": "true", "data-invalid": "false", "option-id": "two", className: undefined
      });
    });
  });

  describe("menus", () => {
    it("hooks the vertical menu", () => {
      const model = [{name: "first", title: "First", active: true}, {}];
      const pt = panelMenuPassThrough(model);
      expect(pt.root).toEqual({"data-testid": "menu"});
      expect(call(pt.panel, {context: {index: 0, active: true}})).toEqual({
        "data-testid": "menu-option", "data-active": "true", "data-open": "true", "option-name": "first"
      });
      expect(call(pt.headerAction, {context: {index: 0}})).toEqual({"data-testid": "menu-link", name: "first", title: "First"});
      expect(call(pt.menuitem, {context: {active: false, item: {item: {name: "nested", active: true}}}})).toEqual({
        "data-testid": "menu-option", "data-active": "true", "data-open": "false", "option-name": "nested"
      });
      expect(call(pt.action, {context: {}})).toEqual({"data-testid": "menu-link"});
      expect(pt.toggleableContent).toEqual({"data-testid": "menu-dropdown"});
      expect(pt.menu).toEqual({"data-testid": "menu-submenu"});
    });

    it("hooks the horizontal menu", () => {
      const pt = menubarPassThrough();
      const options = {context: {active: true, item: {item: {name: "top", title: "Top", active: false}}}};
      expect(call(pt.menuitem, options)).toEqual({
        "data-testid": "menu-option", "data-active": "false", "data-open": "true", "option-name": "top"
      });
      expect(call(pt.action, options)).toEqual({"data-testid": "menu-link", name: "top", title: "Top"});
      expect(call(pt.menuitem, {})).toEqual({"data-testid": "menu-option", "data-active": "false"});
      expect(pt.root).toEqual({"data-testid": "menu"});
      expect(pt.submenu).toEqual({"data-testid": "menu-submenu"});
    });
  });

  describe("messages", () => {
    it("maps the severities to the test vocabulary", () => {
      expect(["success", "info", "warn", "error", "other"].map(alertType))
        .toEqual(["success", "info", "warning", "danger", "other"]);
      expect(alertType(undefined as unknown as string)).toBeUndefined();
    });

    it("hooks the popover and the toast", () => {
      expect(popoverPassThrough("warn", "txt")).toEqual({
        root: {"data-testid": "popover", "data-testid-owner": "txt", "data-type": "warning"}
      });
      const toast = toastPassThrough();
      expect(call(toast.message, {state: {messages: [{message: {severity: "error"}}]}, index: 0})).toEqual({
        "data-testid": "alert", "data-type": "danger"
      });
      expect(call(toast.message, {})).toEqual({"data-testid": "alert"});
      expect(toast.summary).toEqual({"data-testid": "alert-title"});
      expect(toast.detail).toEqual({"data-testid": "alert-message"});
      expect(toast.closeButton).toEqual({"data-testid": "alert-close"});
    });
  });

  describe("dialogs", () => {
    it("hooks a dialog with its view", () => {
      expect(dialogPassThrough("dlg", false, "report")).toEqual({
        root: {"data-testid": "dialog", "data-testid-owner": "dlg", "data-open": "false", "view-id": "report"},
        closeButton: {"data-testid": "dialog-close"}
      });
      expect(dialogPassThrough("dlg").root).toEqual({"data-testid": "dialog", "data-testid-owner": "dlg", "data-open": "true"});
    });

    it("hooks a side panel", () => {
      expect(sidePanelPassThrough("side", false)).toEqual({
        root: {"data-testid": "side-panel", "data-testid-owner": "side", "data-open": "false"},
        closeButton: {"data-testid": "side-panel-close"}
      });
      expect(sidePanelPassThrough("side").root).toHaveProperty("data-open", "true");
    });
  });

  describe("context menu", () => {
    it("hooks the menu and the link of an item", () => {
      expect(contextMenuPassThrough()).toEqual({
        root: {"data-testid": "context-menu"},
        menuitem: {"data-testid": "context-menu-option"},
        submenu: {"data-testid": "context-submenu"}
      });
      const {container} = render(contextMenuLinkTemplate(
        {optionId: "edit", disabled: true},
        {element: <a href="#edit">Edit</a>}
      ));
      const link = container.querySelector("a");
      expect(link?.getAttribute("data-testid")).toBe("context-menu-link");
      expect(link?.getAttribute("option-id")).toBe("edit");
      expect(link?.getAttribute("data-disabled")).toBe("true");
    });
  });

  describe("button group", () => {
    it("hooks an option with its value and state", () => {
      expect(call(buttonGroupPassThrough.button, {context: {selected: true, option: {value: "A"}}})).toEqual({
        "data-testid": "criterion-input", "data-selected": "true", "option-id": "A"
      });
      expect(call(buttonGroupPassThrough.button, {context: {selected: false}})).toEqual({
        "data-testid": "criterion-input", "data-selected": "false", "option-id": undefined
      });
    });
  });
});
