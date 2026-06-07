const {createContractScope} = require("../shared/contractSharedTestUtils.js");

function getDefaultValues() {
  return [
    {id: 1, value: "tutu", other: null, RowTyp: "INSERT"},
    {id: 2, value: "lala", other: null},
    {id: 4, value: "lele", other: "asda", RowTyp: "UPDATE"},
    {id: 5, value: "lili", other: null},
    {id: 6, value: "lolo", other: null, RowTyp: "DELETE"},
    {id: 7, value: "lulu", other: null}
  ];
}

function getDefaultColumns() {
  return [{
    id: "id",
    hidden: true,
    component: "text",
    enableFiltering: true,
    charlength: "0",
    sendable: true,
    printable: true
  }, {
    id: "value",
    label: "Value",
    hidden: false,
    component: "text",
    enableFiltering: true,
    sortField: "value",
    charlength: 20,
    sortable: true,
    sendable: true,
    printable: true
  }, {
    id: "other",
    label: "Other",
    hidden: false,
    component: "text",
    sortable: false,
    sortField: "other",
    charlength: 20,
    sendable: true,
    printable: true
  }];
}

function createGridHarness({$rootScope, GridCommons, currentModels = {}, currentControllers = {}, currentApis = {}, options = {}}) {
  const id = options.id || "GrdContract";
  const scope = createContractScope($rootScope, {
    ...options,
    scope: {
      charSize: 7,
      gridOptions: {}
    }
  });

  currentModels[id] = {
    records: 6,
    total: 1,
    page: 1,
    values: options.values || getDefaultValues(),
    selected: options.selection === undefined ? [4] : options.selection,
    ...options.model
  };

  currentControllers[id] = {
    columnModel: options.columns || getDefaultColumns(),
    headerModel: [],
    sendAll: false,
    loadAll: false,
    ...options.controller
  };

  const component = {
    constants: {
      ROW_IDENTIFIER: "id",
      CELL_VALUE: "value",
      CELL_LABEL: "label",
      CELL_TITLE: "title",
      CELL_STYLE: "style",
      CELL_ICON: "icon",
      CELL_IMAGE: "image"
    },
    controller: currentControllers[id],
    enableSorting: true,
    scope,
    listeners: {},
    api: {},
    model: currentModels[id],
    address: {view: scope.view, component: id},
    resetSelection: () => null,
    selectRow: () => null,
    storeEvent: () => null,
    attributeMethods: {},
    getSpecificFields: () => options.specificFields || {max: 20, page: 1, sort: []}
  };

  const commons = new GridCommons(component);
  commons.init();
  currentApis[id] = component;

  return {
    id,
    scope,
    component,
    model: currentModels[id],
    controller: currentControllers[id],
    api: currentApis[id]
  };
}

module.exports = {
  createGridHarness
};
