// The button of a grid row selects that row (only that row) before its actions (#793)
import "./lightComponentTestUtils";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";

describe("grid selectButtonRow", () => {
  let GridCommons;

  beforeEach(() => {
    angular.mock.module("aweApplication");
    inject(["$injector", "$httpBackend", ($injector, $httpBackend) => {
      GridCommons = $injector.get("GridCommons");
      $httpBackend.when("POST", "settings").respond({...DefaultSettings, language: "en"});
    }]);
  });

  const createGrid = (selected = []) => {
    const component = {
      constants: {ROW_IDENTIFIER: "id"},
      scope: {$on: () => null, gridOptions: {}},
      listeners: {},
      api: {},
      controller: {columnModel: [], headerModel: [], contextMenu: []},
      model: {values: [{id: 1}, {id: 2}, {id: 3}], selected}
    };
    new GridCommons(component).init();
    component.setSelection = jest.fn();
    return component;
  };

  it("leaves only the row of the button selected", () => {
    const component = createGrid([1, 3]);

    component.selectButtonRow("2");

    expect(component.setSelection).toHaveBeenCalledWith(["2"]);
  });

  it("selects the row of the button when nothing is selected", () => {
    const component = createGrid([]);

    component.selectButtonRow("2");

    expect(component.setSelection).toHaveBeenCalledWith(["2"]);
  });

  it("does nothing when the row of the button is already the only selected one", () => {
    const component = createGrid([2]);

    component.selectButtonRow("2");

    expect(component.setSelection).not.toHaveBeenCalled();
  });

  it("does nothing for a row that is not in the grid (the footer)", () => {
    const component = createGrid([1]);

    component.selectButtonRow("footer");

    expect(component.setSelection).not.toHaveBeenCalled();
  });
});
