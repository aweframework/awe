import "./gridTestUtils";
import {DefaultSettings} from "../../../../main/resources/js/awe/data/options";

describe('awe-framework/awe-client-angular/src/test/jest/services/grid/unsaved (#918)', function () {
  let GridCommons, GridEvents, $timeout;

  const UNSAVED = "records-unsaved";
  const SELECT_TAB = "select-current-tab";

  /**
   * Build an editable grid component whose save button is already rendered
   * @param {object} row Element of the row being edited
   * @returns {object} Grid component
   */
  const buildEditableGrid = (row) => {
    const component = {
      id: "GrdEdit",
      constants: {ROW_IDENTIFIER: "id"},
      controller: {columnModel: [], headerModel: []},
      scope: {$on: () => null, $emit: jest.fn(), gridOptions: {}},
      listeners: {},
      api: {},
      model: {values: [{id: 2}], selected: []},
      address: {view: "report", component: "GrdEdit"},
      storeEvent: jest.fn(),
      attributeMethods: {},
      editable: true
    };
    new GridCommons(component).init();
    component.layers = {save: {css: jest.fn()}, content: {find: jest.fn(() => ({last: () => [row]}))}};
    component.getButtonPosition = () => ({top: 0, left: 0});
    return component;
  };

  beforeEach(function () {
    angular.mock.module('aweApplication');

    inject(["$injector", function ($injector) {
      GridCommons = $injector.get("GridCommons");
      GridEvents = $injector.get("GridEvents");
      $timeout = $injector.get("$timeout");
      $injector.get("$httpBackend").when("POST", "settings").respond(DefaultSettings);
    }]);
  });

  afterEach(function () {
    jest.restoreAllMocks();
  });

  it('reports unsaved rows to its containers while a row is edited, and clears them once it is saved or cancelled',
    function () {
      const component = buildEditableGrid({scrollIntoView: jest.fn()});

      component.currentSelection = [2];
      component.repositionSaveButton();
      component.repositionSaveButton();
      // Saving or cancelling the row unselects it
      component.currentSelection = [];
      component.repositionSaveButton();

      const reports = component.scope.$emit.mock.calls.filter(([event]) => event === UNSAVED);
      expect(reports).toEqual([[UNSAVED, {id: "GrdEdit", invalid: true}], [UNSAVED, {id: "GrdEdit", invalid: false}]]);
    });

  it('check-records-saved keeps the warning, opens the containers of the grid and scrolls the edited row into view',
    function () {
      const row = {scrollIntoView: jest.fn()};
      const component = buildEditableGrid(row);
      const sendGridMessage = jest.spyOn(GridEvents, "sendGridMessage").mockImplementation(() => null);
      component.currentSelection = [2];
      component.repositionSaveButton();

      GridEvents.onCheckRecordSaved({}, {component});
      $timeout.flush();

      expect(sendGridMessage).toHaveBeenCalledWith(component, 'warning', 'GRID_CHECK_ALL_SAVED_TITLE',
        'GRID_CHECK_ALL_SAVED_MESSAGE');
      expect(component.scope.$emit).toHaveBeenCalledWith(SELECT_TAB);
      expect(component.layers.content.find).toHaveBeenCalledWith(".ui-grid-row[id='2']");
      expect(row.scrollIntoView).toHaveBeenCalledWith({block: "center", behavior: "smooth"});
    });
});
