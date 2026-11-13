import "./gridTestUtils";
import {ClientActions} from "../../../../main/resources/js/awe/data/actions";

describe('awe-framework/awe-client-angular/src/test/jest/services/grid/events.js', function () {
  let GridEvents, $log, writeText;

  function flushPromises() {
    return new Promise(resolve => setTimeout(resolve, 0));
  }

  function buildScope() {
    return {
      component: {
        constants: {SELECTED_TAIL: "_selected"},
        hideContextMenu: jest.fn(),
        controller: {columnModel: [{id: "name", label: "NAME"}]},
        getColumnPrintData: jest.fn().mockReturnValue({name_selected: ["Alice", "Bob"]})
      }
    };
  }

  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["GridEvents", "$log", function (_GridEvents_, _$log_) {
      GridEvents = _GridEvents_;
      $log = _$log_;
    }]);

    writeText = jest.fn();
    Object.defineProperty(navigator, "clipboard", {value: {writeText}, configurable: true, writable: true});
    jest.spyOn($log, "error").mockImplementation(() => null);
  });

  afterEach(function () {
    delete navigator.clipboard;
    jest.restoreAllMocks();
  });

  it('copies the selected rows to the clipboard and logs nothing on success', async function () {
    writeText.mockResolvedValue(undefined);

    GridEvents.onCopySelectedRowsToClipboard({}, buildScope());
    await flushPromises();

    expect(writeText).toHaveBeenCalledWith("NAME\nAlice\nBob");
    expect($log.error).not.toHaveBeenCalled();
  });

  it('does not copy the row detail toggle column', async function () {
    writeText.mockResolvedValue(undefined);
    const scope = buildScope();
    scope.component.controller.columnModel = [{id: "rowDetailToggle", label: ""}, {id: "name", label: "NAME"}];

    GridEvents.onCopySelectedRowsToClipboard({}, scope);
    await flushPromises();

    expect(writeText).toHaveBeenCalledWith("NAME\nAlice\nBob");
    expect(scope.component.getColumnPrintData).not.toHaveBeenCalledWith("rowDetailToggle");
  });

  it('logs the rejection when the clipboard write fails', async function () {
    const error = new Error("Write permission denied");
    writeText.mockRejectedValue(error);

    GridEvents.onCopySelectedRowsToClipboard({}, buildScope());
    await flushPromises();

    expect($log.error).toHaveBeenCalledTimes(1);
    expect($log.error).toHaveBeenCalledWith(expect.stringContaining("clipboard"), error);
  });

  it.each([
    ["onMoveRowTop", "first"],
    ["onMoveRowBeforeSelected", "before"],
    ["onMoveRowAfterSelected", "after"],
    ["onMoveRowBottom", "last"]
  ])('%s moves the row to the %s position and stores the event', function (method, position) {
    const scope = {component: {...buildScope().component, moveRow: jest.fn().mockReturnValue(true), storeEvent: jest.fn()}};

    GridEvents[method]({selectedRow: "5"}, scope);

    expect(scope.component.hideContextMenu).toHaveBeenCalled();
    expect(scope.component.moveRow).toHaveBeenCalledWith(position, "5");
    expect(scope.component.storeEvent).toHaveBeenCalledWith("move-row");
  });

  it('does not store the move event when the row did not move', function () {
    const scope = {component: {...buildScope().component, moveRow: jest.fn().mockReturnValue(false), storeEvent: jest.fn()}};

    GridEvents.onMoveRowTop({}, scope);

    expect(scope.component.storeEvent).not.toHaveBeenCalled();
  });

  describe('copy-rows', function () {
    let Control, $translate, changeModelAttribute, source, target, controllers;
    const TARGET = {view: "base", component: "GrdViw"};
    // The target grid: its columns have their own names (the names of the columns of a screen are unique)
    const targetScope = () => ({component: {
      address: TARGET,
      constants: {ROW_IDENTIFIER: "id"},
      controller: {columnModel: [{name: "rowNum"}, {name: "ViwSit"}, {name: "ViwKey", hidden: true}, {name: "ViwMod"}, {name: "ViwOrd"}]}
    }});
    const copyRowsAction = value => ({attr: name => ({value})[name]});

    beforeEach(function () {
      inject(["Control", "$translate", function (_Control_, _$translate_) {
        Control = _Control_;
        $translate = _$translate_;
      }]);
      source = {
        values: [
          {id: "1", IdeSit: 1, IdeMod: 28, Ord: 1, Sit: "Madrid", selected: true, $$hashKey: "uiGrid-1"},
          {id: "2", IdeSit: 2, IdeMod: 916, Ord: 2, RowTyp: "DELETE", RowIco: {icon: "fa-trash"}},
          {id: "new-row-0", IdeSit: "2", IdeMod: {value: 916, label: "Old"}, Ord: "3", RowTyp: "INSERT", RowIco: {icon: "fa-plus"}},
          {id: "3", IdeSit: 7, IdeMod: 28, Ord: 4, RowTyp: "UPDATE"}
        ]
      };
      target = {values: [{id: "old", IdeSit: {value: 9, label: "Old"}}]};
      controllers = {
        GrdEdi: {
          columnModel: [
            {name: "chkBox"},
            {name: "rowNum"},
            {name: "IdeSitModDbs", hidden: true},
            {name: "IdeSit", component: "select", model: {values: [{value: 1, label: "Madrid"}, {value: 2, label: "SITE_ONATE"}]}},
            {name: "IdeMod", component: "select", model: {values: [{value: 28, label: "Base"}, {value: 916, label: "Test"}]}},
            {name: "Ord", component: "text"},
            {name: "RowIco"}
          ]
        }
      };
      jest.spyOn(Control, "getAddressModel").mockImplementation(address => ({GrdEdi: source, GrdViw: target})[address.component]);
      jest.spyOn(Control, "getAddressController").mockImplementation(address => controllers[address.component]);
      changeModelAttribute = jest.spyOn(Control, "changeModelAttribute").mockImplementation(() => null);
      jest.spyOn($translate, "instant").mockImplementation(label => label === "SITE_ONATE" ? "Oñate" : label);
    });

    it('replaces the rows of the target with the rows of the value grid, column by shown column, with the texts of their select cells', function () {
      const before = _.cloneDeep(source);

      GridEvents.onCopyRows({}, targetScope(), TARGET, copyRowsAction("GrdEdi"));

      expect(changeModelAttribute).toHaveBeenCalledTimes(1);
      expect(changeModelAttribute).toHaveBeenCalledWith(TARGET, {
        values: [
          {id: "1", ViwSit: {value: 1, label: "Madrid"}, ViwMod: {value: 28, label: "Base"}, ViwOrd: 1},
          {id: "new-row-0", ViwSit: {value: "2", label: "Oñate"}, ViwMod: {value: 916, label: "Test"}, ViwOrd: "3"},
          {id: "3", ViwSit: 7, ViwMod: {value: 28, label: "Base"}, ViwOrd: 4}
        ],
        records: 3,
        total: 1,
        page: 1
      }, true);
      // The value grid keeps its rows, its selection and its operations
      expect(source).toEqual(before);
    });

    it('empties the target when the value grid has no rows', function () {
      source.values = [];

      GridEvents.onCopyRows({}, targetScope(), TARGET, copyRowsAction("GrdEdi"));

      expect(changeModelAttribute).toHaveBeenCalledWith(TARGET, {values: [], records: 0, total: 1, page: 1}, true);
    });

    it('leaves the target as it is when the value grid is not on screen', function () {
      GridEvents.onCopyRows({}, targetScope(), TARGET, copyRowsAction("GrdMissing"));

      expect(changeModelAttribute).not.toHaveBeenCalled();
      expect(target.values).toEqual([{id: "old", IdeSit: {value: 9, label: "Old"}}]);
    });

    it('is the grid action copy-rows', function () {
      expect(ClientActions.grid.commons["copy-rows"]).toEqual(expect.objectContaining({method: "onCopyRows"}));
    });
  });
});
