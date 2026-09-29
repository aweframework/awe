import React from 'react';
import {fireEvent, screen} from '@testing-library/react';

import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

describe('awe-react-client/test/js/components/AweTreeGridTest.jsx', () => {

  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders Awe Tree Grid component', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          screen: {
            breadcrumbs: [],
            report: {name: "opcion", option: "opcion"}
          },
          model: {values: []},
          attributes: {
            columnModel: [],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

    // check
    expect(screen.getByRole("grid-container")).toBeDefined();
  });

  it('renders Awe Tree Grid component with buttons', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        "button-add": {
          address: {component: 'button-add', view: 'report'},
          attributes: {
            "label": "Add",
            "icon": "add"
          }
        },
        "button-delete": {
          address: {component: 'button-delete', view: 'report'},
          attributes: {
            "label": "Delete",
            "icon": "mdi:delete"
          }
        },
        "grid": {
          address: {component: 'grid', view: 'report'},
          model: {values: []},
          attributes: {
            columnModel: [],
            headerModel: [],
            buttonModel: [{
              "actions": [{
                "elementType": "ButtonAction",
                "type": "add-row",
                "elementList": [],
                "target": "grid",
                "silent": true,
                "async": false,
                "parameters": {
                  "target": "grid",
                  "value": null,
                  "label": null,
                  "serverAction": null,
                  "targetAction": null
                }
              }],
              "autoload": false,
              "buttonType": "button",
              "checkEmpty": false,
              "checkInitial": true,
              "checked": false,
              "contextMenu": [],
              "dependencies": [],
              "elementList": [{
                "elementType": "ButtonAction",
                "type": "add-row",
                "elementList": [],
                "target": "grid",
                "silent": true,
                "async": false,
                "parameters": {
                  "target": "grid",
                  "value": null,
                  "label": null,
                  "serverAction": null,
                  "targetAction": null
                }
              }],
              "elementType": "Button",
              "icon": "plus-circle",
              "id": "button-add",
              "label": "BUTTON_NEW",
              "loadAll": false,
              "optional": false,
              "printable": true,
              "readonly": false,
              "required": false,
              "strict": true,
              "visible": true
            }, {
              "actions": [{
                "elementType": "ButtonAction",
                "type": "delete-row",
                "elementList": [],
                "target": "grid",
                "silent": true,
                "async": false,
                "parameters": {
                  "target": "grid",
                  "value": null,
                  "label": null,
                  "serverAction": null,
                  "targetAction": null
                }
              }],
              "autoload": false,
              "buttonType": "button",
              "checkEmpty": false,
              "checkInitial": true,
              "checked": false,
              "contextMenu": [],
              "dependencies": [{
                "elementType": "Dependency",
                "type": "and",
                "elementList": [{
                  "elementType": "DependencyElement",
                  "id": "grid",
                  "elementList": [],
                  "condition": "eq",
                  "value": "1",
                  "attribute1": "selectedRows",
                  "checkChanges": true
                }],
                "initial": true,
                "invert": false,
                "target": "enable",
                "silent": false,
                "async": false,
                "elements": [{
                  "elementType": "DependencyElement",
                  "id": "grid",
                  "elementList": [],
                  "condition": "eq",
                  "value": "1",
                  "attribute1": "selectedRows",
                  "checkChanges": true
                }],
                "actions": []
              }],
              "elementList": [{
                "elementType": "ButtonAction",
                "type": "delete-row",
                "elementList": [],
                "target": "grid",
                "silent": true,
                "async": false,
                "parameters": {
                  "target": "grid",
                  "value": null,
                  "label": null,
                  "serverAction": null,
                  "targetAction": null
                }
              }, {
                "elementType": "Dependency",
                "type": "and",
                "elementList": [{
                  "elementType": "DependencyElement",
                  "id": "grid",
                  "elementList": [],
                  "condition": "eq",
                  "value": "1",
                  "attribute1": "selectedRows",
                  "checkChanges": true
                }],
                "initial": true,
                "invert": false,
                "target": "enable",
                "silent": false,
                "async": false,
                "elements": [{
                  "elementType": "DependencyElement",
                  "id": "grid",
                  "elementList": [],
                  "condition": "eq",
                  "value": "1",
                  "attribute1": "selectedRows",
                  "checkChanges": true
                }],
                "actions": []
              }],
              "elementType": "Button",
              "icon": "mdi:delete",
              "id": "button-delete",
              "label": "BUTTON_DELETE",
              "loadAll": false,
              "optional": false,
              "printable": true,
              "readonly": false,
              "required": false,
              "strict": true,
              "visible": true
            }]
          },
          specificAttributes: {sort: []}
        }
      }
    };
    renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

    // check
    expect(screen.getByRole("grid-container")).toBeDefined();
    expect(screen.getByLabelText("Add")).toBeDefined();
    expect(screen.getByLabelText("Delete")).toBeDefined();
  });

  describe('renders Awe Tree Grid component editable', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        size: {width: 100, height: 100},
        breadcrumbs: [],
        report: {name: "opcion", option: "opcion"}
      },
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {"id": 1, parent: "", "test": "test", "testSum": 12.4, "testAvg": 1212311.4, "testMax": 12.4, "testMin": 12.4},
              {
                "id": 2,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35},
                parent: ""
              },
              {
                "id": 3,
                "parent": 2,
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ]
          },
          attributes: {
            showTotals: true,
            rowNumbers: true,
            multiselect: false,
            multioperation: false,
            editable: true,
            loadAll: true,
            max: 30,
            columnModel: [
              {name: "test"},
              {
                name: "testSum",
                label: "testSum",
                component: "numeric",
                summaryType: "sum",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 2, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testAvg",
                label: "testAvg",
                component: "numeric",
                summaryType: "avg",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 1, aSign: '$', aSep: ',', pSign: 'p', aPad: true}
              },
              {
                name: "testMax",
                label: "testMax",
                component: "numeric",
                summaryType: "max",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 4, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testMin",
                label: "testMin",
                component: "numeric",
                summaryType: "min",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 0, aSign: ' EUR', pSign: 's', aPad: true}
              }],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    it('... and edit row', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.queryByRole("grid-container")).toBeDefined();
      expect(screen.queryByRole("save-edit-row")).toBeNull();
      expect(screen.queryByRole("cancel-edit-row")).toBeNull();

      fireEvent.click(screen.getAllByRole("edit-row")[0]);
    });
  });

  describe('renders Awe Tree Grid component editable being edited', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        size: {width: 100, height: 100},
        breadcrumbs: [],
        report: {name: "opcion", option: "opcion"}
      },
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {
                "id": 1,
                parent: "",
                "test": "test",
                "testSum": 12.4,
                "testAvg": 1212311.4,
                "testMax": 12.4,
                "testMin": 12.4,
                $row: {editing: true, id: 1}
              },
              {
                "id": 2,
                parent: 1,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35}
              },
              {
                "id": 3,
                parent: "2",
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ]
          },
          attributes: {
            showTotals: true,
            rowNumbers: true,
            multiselect: false,
            multioperation: false,
            editable: true,
            loadAll: true,
            max: 30,
            columnModel: [
              {name: "test"},
              {
                name: "testSum",
                label: "testSum",
                component: "numeric",
                summaryType: "sum",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 2, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testAvg",
                label: "testAvg",
                component: "numeric",
                summaryType: "avg",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 1, aSign: '$', aSep: ',', pSign: 'p', aPad: true}
              },
              {
                name: "testMax",
                label: "testMax",
                component: "numeric",
                summaryType: "max",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 4, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testMin",
                label: "testMin",
                component: "numeric",
                summaryType: "min",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 0, aSign: ' EUR', pSign: 's', aPad: true}
              }],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {
            sort: [],
            editingRow: {"id": 1, "test": "test", "testSum": 12.4, "testAvg": 10.23, "testMax": 12.4, "testMin": 12.4}
          }
        }
      }
    };

    it('... and save row', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("save-edit-row"));
    });

    it('... and cancel row', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("cancel-edit-row"));
    });

    it('... and save row with keyboard', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent(screen.getByRole("grid-container"), new KeyboardEvent('keydown', {
        code: 'Enter',
        key: 'Enter',
        charCode: 13,
        keyCode: 13,
        view: window,
        bubbles: true
      }));
    });

    it('... and cancel row with keyboard', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent(screen.getByRole("grid-container"), new KeyboardEvent('keydown', {
        code: 'Escape',
        key: 'Escape',
        charCode: 27,
        keyCode: 27,
        view: window,
        bubbles: true
      }));
    });
  });

  describe('renders Awe Tree Grid component multioperation being edited', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        size: {width: 100, height: 100},
        breadcrumbs: [],
        report: {name: "opcion", option: "opcion"}
      },
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {"id": 1, parent: null, "test": "test", "testSum": 12.4, "testAvg": 1212311.4, "testMax": 12.4, "testMin": 12.4},
              {
                "id": 2,
                parent: null,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35},
                $row: {editing: true}
              },
              {
                "id": 3,
                parent: null,
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ]
          },
          attributes: {
            showTotals: true,
            rowNumbers: true,
            multiselect: false,
            multioperation: true,
            editable: true,
            loadAll: false,
            max: 30,
            columnModel: [
              {name: "test", label: "test"},
              {
                name: "testSum",
                hidden: false,
                label: "testSum",
                component: "numeric",
                summaryType: "sum",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 2, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testAvg",
                hidden: false,
                label: "testAvg",
                component: "numeric",
                summaryType: "avg",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 1, aSign: '$', aSep: ',', pSign: 'p', aPad: true}
              },
              {
                name: "testMax",
                hidden: false,
                label: "testMax",
                component: "numeric",
                summaryType: "max",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 4, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testMin",
                hidden: false,
                label: "testMin",
                component: "numeric",
                summaryType: "min",
                dependencies: [],
                numberFormat: {vMin: '-999999', mDec: 0, aSign: ' EUR', pSign: 's', aPad: true}
              }],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {
            sort: [],
            editingRow: {
              "id": 2,
              "test": "test1",
              "testSum": {value: 35},
              "testAvg": {value: 332},
              "testMax": {value: 35},
              "testMin": {value: 35}
            }
          }
        }
      }
    };

    it('... and save row', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("save-edit-row"));
    });

    it('... and cancel row', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("cancel-edit-row"));
    });

    it('... and save row with keyboard', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent(screen.getByRole("grid-container"), new KeyboardEvent('keydown', {
        code: 'Enter',
        key: 'Enter',
        charCode: 13,
        keyCode: 13,
        view: window,
        bubbles: true
      }));
    });

    it('... and cancel row with keyboard', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent(screen.getByRole("grid-container"), new KeyboardEvent('keydown', {
        code: 'Escape',
        key: 'Escape',
        charCode: 27,
        keyCode: 27,
        view: window,
        bubbles: true
      }));
    });
  });

  describe('renders Awe Tree Grid component with some data and load all', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        size: {width: 100, height: 100},
        breadcrumbs: [],
        report: {name: "opcion", option: "opcion"}
      },
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {"id": 1, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 2, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 3, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 4, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 5, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 6, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 7, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 8, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 9, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 10, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 11, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 12, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 13, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 14, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 15, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 16, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 17, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 18, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 19, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 20, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 21, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 22, parent: 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 23, parent: 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 24, parent: 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 25, parent: "4", col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 26, parent: "4", col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 27, parent: 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 28, parent: 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 29, parent: 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 30, parent: 28, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 31, parent: 28, col1: "val1", col2: "val2", col3: "val3", col4: 12}
            ]
          },
          attributes: {
            showTotals: false,
            rowNumbers: true,
            multiselect: true,
            multioperation: false,
            editable: false,
            loadAll: true,
            enableFilters: true,
            max: 10,
            columnModel: [
              {name: "col1", label: "Column 1", sortable: true, dependencies: []},
              {name: "col2", label: "Column 2", sortable: true, dependencies: []},
              {name: "col3", label: "Column 3", sortable: true, dependencies: []},
              {name: "col4", label: "Column 4", sortable: true, dependencies: []}],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };
    it('... and check load', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
    });
  });

  describe('renders Awe Tree Grid component with some data', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        size: {width: 100, height: 100},
        breadcrumbs: [],
        report: {name: "opcion", option: "opcion"}
      },
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            records: 31, page: 1, total: 4, values: [
              {"id": 1, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 2, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 3, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 4, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 5, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 6, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 7, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 8, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 9, parent: null, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 10, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 11, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 12, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 13, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 14, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 15, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 16, parent: 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 17, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 18, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 19, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 20, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 21, parent: 2,col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 22, parent: 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 23, parent: 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 24, parent: 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 25, parent: "4", col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 26, parent: "4", col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 27, parent: 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 28, parent: 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 29, parent: 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 30, parent: 28, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 31, parent: 28, col1: "val1", col2: "val2", col3: "val3", col4: 12}
            ]
          },
          attributes: {
            showTotals: false,
            rowNumbers: true,
            multiselect: true,
            multioperation: false,
            editable: false,
            loadAll: false,
            enableFilters: true,
            max: 10,
            columnModel: [
              {name: "col1", label: "Column 1", sortable: true, dependencies: []},
              {name: "col2", label: "Column 2", sortable: true, dependencies: []},
              {name: "col3", label: "Column 3", sortable: true, dependencies: []},
              {name: "col4", label: "Column 4", sortable: true, dependencies: []}],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };
    it('... and check load', () => {
      renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("grid-container")).toBeDefined();
    });
  });
});
