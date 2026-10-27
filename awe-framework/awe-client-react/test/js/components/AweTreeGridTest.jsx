import React from 'react';
import {act, cleanup, fireEvent, screen} from '@testing-library/react';

import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

/**
 * Queued actions of a type, wherever the actions slice keeps them
 * @param store Redux store
 * @param type Action type
 * @returns {Object[]} Actions
 */
function queuedActions(store, type) {
  const found = [];
  const visit = (value) => {
    if (Array.isArray(value)) {
      value.forEach(visit);
    } else if (value && typeof value === "object") {
      value.type === type && "address" in value ? found.push(value) : Object.values(value).forEach(visit);
    }
  };
  visit(store.getState().actions);
  return found;
}

describe('awe-react-client/test/js/components/AweTreeGridTest.jsx', () => {

  beforeEach(() => {
    jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  // Row actions (save and cancel) added to the top of the action stack
  const rowActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === 'ADD_ACTIONS_TOP')
    .flatMap(action => action.payload)
    .filter(action => ["save-row", "cancel-row"].includes(action.type));

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

  it('applies the column layout of the user and shows the toolbar', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {values: []},
          attributes: {
            treegrid: true,
            columnModel: [{name: "a", label: "A"}, {name: "b", label: "B"}, {name: "c", label: "C"}],
            columnLayout: {order: ["c", "b", "a"], hidden: ["b"]},
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

    expect(screen.getAllByRole("columnheader").map(header => header.textContent.trim()).filter(Boolean)).toEqual(["C", "A"]);
    expect(document.querySelector('[data-testid="grid-toolbar"]')).not.toBeNull();
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

  it('exposes every row of the tree, leaves included, with its identifier', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {id: "Root", parent: "", name: "Root", $row: {expanded: true}},
              {id: "Leaf", parent: "Root", name: "Leaf"}
            ]
          },
          attributes: {
            treeId: "id", treeParent: "parent", expandColumn: "name", loadAll: true, max: 30,
            columnModel: [{name: "name"}], headerModel: [], buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    const {container} = renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

    const rows = container.querySelectorAll("[tree-grid-id='grid'] [data-testid='grid-row']");
    expect(Array.from(rows).map(row => row.getAttribute("row-id"))).toEqual(["Root", "Leaf"]);
    // The hook is the row itself, so a context menu or a click on the row reaches the grid, not only its first cell
    expect(Array.from(rows).map(row => row.tagName)).toEqual(["TR", "TR"]);
    expect(container.querySelectorAll("[tree-grid-id='grid'] [data-testid='grid-row'] [data-testid='grid-row']")).toHaveLength(0);

    // Each level indents the toggle of its row by the size of a toggle
    const toggles = Array.from(container.querySelectorAll("[tree-grid-id='grid'] .p-treetable-toggler"));
    expect(toggles.map(toggle => toggle.style.marginLeft)).toEqual(["calc(0 * var(--awe-space-6))", "calc(1 * var(--awe-space-6))"]);
  });

  it('tells which row of the tree is selected', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {id: "Root", parent: "", name: "Root", selected: true, $row: {expanded: true}},
              {id: "Leaf", parent: "Root", name: "Leaf"}
            ]
          },
          attributes: {
            treeId: "id", treeParent: "parent", expandColumn: "name", loadAll: true, max: 30,
            columnModel: [{name: "name"}], headerModel: [], buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    const {container} = renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState});

    const selected = Array.from(container.querySelectorAll("[tree-grid-id='grid'] [data-testid='grid-row']"))
      .map(row => row.getAttribute("data-selected"));
    expect(selected).toEqual(["true", "false"]);
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
      jest.useFakeTimers();
      const {dispatchSpy} = renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

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

      // The row is saved after the focus has left the cell
      expect(rowActions(dispatchSpy)).toEqual([]);
      act(() => {
        jest.advanceTimersByTime(100);
      });
      expect(rowActions(dispatchSpy)).toEqual([{type: "save-row", address}]);
    });

    it('... and cancel row with keyboard', () => {
      jest.useFakeTimers();
      const {dispatchSpy} = renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

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

      expect(rowActions(dispatchSpy)).toEqual([{type: "cancel-row", address}]);
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
      jest.useFakeTimers();
      const {dispatchSpy} = renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

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

      // The row is saved after the focus has left the cell
      expect(rowActions(dispatchSpy)).toEqual([]);
      act(() => {
        jest.advanceTimersByTime(100);
      });
      expect(rowActions(dispatchSpy)).toEqual([{type: "save-row", address}]);
    });

    it('... and cancel row with keyboard', () => {
      jest.useFakeTimers();
      const {dispatchSpy} = renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

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

      expect(rowActions(dispatchSpy)).toEqual([{type: "cancel-row", address}]);
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

  describe('tree options (icons, tree-leaf, initial-level)', () => {
    const treeState = (values, attributes = {}) => ({
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {values},
          attributes: {
            treeId: "id", treeParent: "parent", expandColumn: "name", max: 30,
            columnModel: [{name: "name"}], headerModel: [], buttonModel: [], ...attributes
          },
          specificAttributes: {sort: []}
        }
      }
    });
    const renderTree = (values, attributes) => renderWithProviders(<AweTreeGrid id="grid"/>, {preloadedState: treeState(values, attributes)});
    const rowIds = (container) => Array.from(container.querySelectorAll("[data-testid='grid-row']")).map(row => row.getAttribute("row-id"));
    const iconOf = (container, id) => container.querySelector(`[data-testid='tree-icon'][row-id='${id}'] i`);
    // The Lucide icon of the sprite that the toggle draws (#885: the Font Awesome names resolve to Lucide)
    const iconName = (container, id) => iconOf(container, id)?.querySelector("use")?.getAttribute("href")?.split("#")[1];
    const family = [
      {id: "Root", parent: "", name: "Root"},
      {id: "Child", parent: "Root", name: "Child"},
      {id: "Grand", parent: "Child", name: "Grand"}
    ];

    it('shows the default Font Awesome icons of the AngularJS client for expand, collapse and leaf', () => {
      const {container} = renderTree([
        {id: "Open", parent: "", name: "Open", $row: {expanded: true}},
        {id: "Leaf", parent: "Open", name: "Leaf"},
        {id: "Closed", parent: "", name: "Closed"},
        {id: "Hidden", parent: "Closed", name: "Hidden"}
      ], {loadAll: true});

      expect(iconName(container, "Open")).toBe("square-minus");
      expect(iconName(container, "Closed")).toBe("square-plus");
      expect(iconName(container, "Leaf")).toBe("circle-dot");
    });

    it('shows the configured icon-expand, icon-collapse and icon-leaf', () => {
      const {container} = renderTree([
        {id: "Open", parent: "", name: "Open", $row: {expanded: true}},
        {id: "Leaf", parent: "Open", name: "Leaf"},
        {id: "Closed", parent: "", name: "Closed"},
        {id: "Hidden", parent: "Closed", name: "Hidden"}
      ], {loadAll: true, iconExpand: "plus", iconCollapse: "minus", iconLeaf: "circle"});

      expect(iconName(container, "Open")).toBe("minus");
      expect(iconName(container, "Open")).not.toBe("square-minus");
      expect(iconName(container, "Closed")).toBe("plus");
      expect(iconName(container, "Leaf")).toBe("circle");
    });

    it('uses the tree-leaf attribute to tell the leaves, parsing its value as a boolean', () => {
      const {container} = renderTree([
        {id: "A", parent: "", name: "A", isLeaf: "true", mine: "false"},
        {id: "B", parent: "", name: "B", isLeaf: "false", mine: "true"},
        {id: "C", parent: "", name: "C", isLeaf: false, mine: true}
      ], {treeLeaf: "mine"});

      expect(iconName(container, "A")).toBe("square-plus");
      expect(iconName(container, "B")).toBe("circle-dot");
      expect(iconName(container, "C")).toBe("circle-dot");
    });

    it('takes 1 and "1" as a leaf and 0 and "0" as a branch, like the AngularJS parseBoolean', () => {
      const {container} = renderTree([
        {id: "A", parent: "", name: "A", isLeaf: 1},
        {id: "B", parent: "", name: "B", isLeaf: " 1 "},
        {id: "C", parent: "", name: "C", isLeaf: 0},
        {id: "D", parent: "", name: "D", isLeaf: "0"}
      ]);

      expect(iconName(container, "A")).toBe("circle-dot");
      expect(iconName(container, "B")).toBe("circle-dot");
      expect(iconName(container, "C")).toBe("square-plus");
      expect(iconName(container, "D")).toBe("square-plus");
    });

    it('does not take the string "false" of the default isLeaf field as a leaf', () => {
      const {container} = renderTree([
        {id: "A", parent: "", name: "A", isLeaf: "false"},
        {id: "B", parent: "", name: "B", isLeaf: "true"}
      ]);

      expect(iconName(container, "A")).toBe("square-plus");
      expect(iconName(container, "B")).toBe("circle-dot");
    });

    it('expands the loaded nodes above initial-level (level 0 are the roots): 2 opens the roots only', () => {
      const {container} = renderTree(family, {loadAll: true, initialLevel: 2});

      expect(rowIds(container)).toEqual(["Root", "Child"]);
      expect(iconName(container, "Root")).toBe("square-minus");
      expect(iconName(container, "Child")).toBe("square-plus");
    });

    it('expands two levels with initial-level 3 and nothing without it', () => {
      expect(rowIds(renderTree(family, {loadAll: true, initialLevel: 3}).container)).toEqual(["Root", "Child", "Grand"]);
      cleanup();
      expect(rowIds(renderTree(family, {loadAll: true}).container)).toEqual(["Root"]);
    });

    it('does not expand by initial-level a node whose children are not loaded', () => {
      const {container} = renderTree([{id: "Root", parent: "", name: "Root", isLeaf: false}], {initialLevel: 5});

      expect(iconName(container, "Root")).toBe("square-plus");
    });

    it('lets the expanded field of a row win over initial-level', () => {
      const {container} = renderTree([
        {id: "Root", parent: "", name: "Root", expanded: "false"},
        {id: "Child", parent: "Root", name: "Child"},
        {id: "Other", parent: "", name: "Other", expanded: "true"},
        {id: "Inner", parent: "Other", name: "Inner"}
      ], {loadAll: true, initialLevel: 2});

      expect(rowIds(container)).toEqual(["Root", "Other", "Inner"]);
    });

    it('keeps the toggled state of the user over initial-level', () => {
      const values = family.map(row => row.id === "Root" ? {...row, $row: {expanded: false}} : row);
      const {container} = renderTree(values, {loadAll: true, initialLevel: 3});

      expect(rowIds(container)).toEqual(["Root"]);
    });

    it('tells the toggle action the state that is shown, so the first click collapses an initially expanded node', () => {
      const {container, store} = renderTree(family, {loadAll: true, initialLevel: 2});

      fireEvent.click(container.querySelector("[data-testid='tree-icon'][row-id='Root']"));

      expect(queuedActions(store, "tree-branch")).toEqual([expect.objectContaining({
        parameters: expect.objectContaining({row: "Root", expanded: true})})]);
    });

    it('collapses locally a node opened by its expanded field even when its branch was not loaded', () => {
      const {container, store} = renderTree([
        {id: "Root", parent: "", name: "Root", isLeaf: false, expanded: "true"}
      ], {targetAction: "TreeBranch"});

      fireEvent.click(container.querySelector("[data-testid='tree-icon'][row-id='Root']"));

      expect(queuedActions(store, "tree-branch")).toEqual([expect.objectContaining({
        parameters: expect.objectContaining({row: "Root", expanded: true})})]);
      expect(queuedActions(store, "server")).toEqual([]);
    });
  });
});
