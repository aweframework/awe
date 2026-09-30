import {
  CLEAR_MENU,
  clearMenu,
  SELECT_OPTION,
  selectOption,
  UPDATE_ALL_OPTIONS,
  UPDATE_BREADCRUMBS,
  UPDATE_MENU,
  UPDATE_OPTION,
  UPDATE_OPTIONS,
  UPDATE_STATUS,
  updateAllOptions,
  updateBreadcrumbs,
  updateMenu,
  updateOption,
  updateOptions,
  updateStatus
} from "../../../../src/redux/actions/menu";

describe("awe-react-client/test/js/redux/actions/menuTest.js", () => {
  const data = {visible: true};

  it("updateMenu carries the menu data", () => {
    expect(updateMenu(data)).toEqual({type: UPDATE_MENU, data});
  });

  it("updateOption carries the option and its data", () => {
    expect(updateOption("screen", data)).toEqual({type: UPDATE_OPTION, option: "screen", data});
  });

  it("updateOptions carries the options and their data", () => {
    expect(updateOptions(["a", "b"], data)).toEqual({type: UPDATE_OPTIONS, options: ["a", "b"], data});
  });

  it("updateAllOptions carries the data for every option", () => {
    expect(updateAllOptions(data)).toEqual({type: UPDATE_ALL_OPTIONS, data});
  });

  it("updateStatus carries the status data", () => {
    expect(updateStatus(data)).toEqual({type: UPDATE_STATUS, data});
  });

  it("selectOption carries the selected option", () => {
    expect(selectOption(data)).toEqual({type: SELECT_OPTION, data});
  });

  it("clearMenu has no payload", () => {
    expect(clearMenu()).toEqual({type: CLEAR_MENU});
  });

  it("updateBreadcrumbs carries the option and its items", () => {
    expect(updateBreadcrumbs("screen", [{label: "Home"}])).toEqual({
      type: UPDATE_BREADCRUMBS,
      option: "screen",
      items: [{label: "Home"}]
    });
  });
});
