import React, {Suspense} from 'react';
import {act, fireEvent, screen} from '@testing-library/react';

import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import {updateModel, updateSpecificAttributes} from "../../../src/redux/actions/components";

describe('awe-react-client/test/js/components/AweGridTest.jsx', () => {

  // Row actions (save and cancel) added to the top of the action stack
  const rowActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === 'ADD_ACTIONS_TOP')
    .flatMap(action => action.payload)
    .filter(action => ["save-row", "cancel-row"].includes(action.type));

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders Awe Grid component', () => {
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

    renderWithProviders(<Suspense fallback={<div>Loading...</div>}>
      <AweGrid id="grid"/>
    </Suspense>, {preloadedState});

    // check
    expect(screen.getByRole("table")).toBeDefined();
  });

  it('renders Awe Grid component with buttons', () => {
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
    renderWithProviders(<Suspense fallback={<div>Loading...</div>}>
      <AweGrid id="grid"/>
    </Suspense>, {preloadedState});

    // check
    expect(screen.getByRole("table")).toBeDefined();
    expect(screen.getByLabelText("Add")).toBeDefined();
    expect(screen.getByLabelText("Delete")).toBeDefined();
  });

  it('renders Awe Grid component with row numbers', () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {
        breadcrumbs: [],
        report: {name: "opcion", option: "opcion"},
        size: {width: 100, height: 100}
      },
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {values: [{id: 1, "test": "test"}]},
          attributes: {
            rowNumbers: true,
            loadAll: true,
            max: 30,
            columnModel: [{name: "test", hidden: false}],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

    // check
    expect(screen.getByRole("table")).toBeDefined();
    expect(screen.getAllByRole("columnheader")[0]).toHaveClass("p-row-number-header");
    expect(screen.getAllByRole("cell")[0]).toHaveClass("p-row-number-cell");
  });

  it('renders Awe Grid component with filters', () => {
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
          model: {values: [{id: 1, "test": "test"}]},
          attributes: {
            loadAll: true,
            enableFilters: true,
            rowNumbers: true,
            max: 30,
            columnModel: [{name: "test", label: "test", hidden: false}],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };
    renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

    //console.info(screen.getByRole("table"));

    // check
    expect(screen.getByRole("table")).toBeDefined();
    expect(screen.getAllByRole("cell")[0]).toHaveClass("p-row-number-cell");
    expect(document.querySelector(".p-column-filter-menu-button")).not.toBeNull();
  });

  it('renders Awe Grid component with footer', async () => {
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
              {id: 1, "test": "test", "testSum": 12.4, "testAvg": 1212311.4, "testMax": 12.4, "testMin": 12.4},
              {
                id: 2,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35}
              },
              {
                id: 3,
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ],
            footer: {
              "test": "test",
              "testSum": {value: 55.4, label: "55,40 EUR"},
              "testAvg": {value: 535853.1, label: "$535,853.1"},
              "testMax": {value: 35, label: "35,0000 EUR"},
              "testMin": {value: 8, label: "8 EUR"}
            }
          },
          attributes: {
            showTotals: true,
            rowNumbers: true,
            multiselect: true,
            multioperation: true,
            editable: true,
            loadAll: false,
            max: 30,
            columnModel: [
              {name: "test"},
              {
                name: "testSum",
                hidden: false,
                summaryType: "sum",
                numberFormat: {vMin: '-999999', mDec: 2, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testAvg",
                hidden: false,
                summaryType: "avg",
                numberFormat: {vMin: '-999999', mDec: 1, aSign: '$', aSep: ',', pSign: 'p', aPad: true}
              },
              {
                name: "testMax",
                hidden: false,
                summaryType: "max",
                numberFormat: {vMin: '-999999', mDec: 4, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testMin",
                hidden: false,
                summaryType: "min",
                numberFormat: {vMin: '-999999', mDec: 0, aSign: ' EUR', pSign: 's', aPad: true}
              }],
            headerModel: [],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };
    renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

    // check
    expect(screen.getByRole("table")).toBeDefined();
    expect(screen.getAllByRole("cell")[1]).toHaveClass("p-row-number-cell");
    expect(await screen.findByText("55,40 EUR")).toBeDefined();
    expect(await screen.findByText("$535,853.1")).toBeDefined();
    expect(await screen.findByText("35,0000 EUR")).toBeDefined();
    expect(await screen.findByText("8 EUR")).toBeDefined();
  });

  it('exposes the operation icon of every row of a multioperation grid', async () => {
    const preloadedState = {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            values: [
              {id: 1, test: "a", $row: {operation: "INSERT"}},
              {id: 2, test: "b", $row: {operation: "UPDATE"}},
              {id: 3, test: "c", $row: {operation: "DELETE"}}
            ]
          },
          attributes: {
            multioperation: true, editable: true, loadAll: true, max: 30,
            columnModel: [{name: "test"}], headerModel: [], buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };

    const {container} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

    const icons = container.querySelectorAll("[grid-id='grid'] [column-id='RowIco'] [data-testid='column-icon']");
    expect(Array.from(icons).map(icon => icon.getAttribute("data-icon"))).toEqual(["plus", "edit", "trash"]);
  });

  it('renders Awe Grid component with footer without rows', async () => {
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
            values: [],
            footer: {
              "test": "test",
              "testSum": {value: 0, label: "0,00 EUR"},
              "testAvg": {value: 0, label: "$0.0"},
              "testMax": {value: 0, label: "0,0000 EUR"},
              "testMin": {value: 0, label: "0 EUR"}
            }
          },
          attributes: {
            showTotals: true,
            loadAll: true,
            rowNumbers: false,
            multiselect: false,
            multioperation: false,
            editable: false,
            disablePagination: true,
            columnModel: [
              {
                name: "testSum",
                summaryType: "sum",
                numberFormat: {vMin: '-999999', mDec: 2, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testAvg",
                summaryType: "avg",
                numberFormat: {vMin: '-999999', mDec: 0, aSign: '$', pSign: 'p', aPad: true}
              },
              {
                name: "testMax",
                summaryType: "max",
                numberFormat: {vMin: '-999999', mDec: 4, aSign: ' EUR', pSign: 's', aPad: true}
              },
              {
                name: "testMin",
                summaryType: "min",
                numberFormat: {vMin: '-999999', mDec: 1, aSign: ' EUR', pSign: 's', aPad: true}
              }],
            headerModel: [{startColumnName: "testSum", numberOfColumns: 2}],
            buttonModel: []
          },
          specificAttributes: {sort: []}
        }
      }
    };
    renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

    // check
    expect(screen.getByRole("table")).toBeDefined();
    expect(screen.getAllByRole("cell")[1]).toHaveClass("p-column-footer");
    expect(await screen.findByText("0,00 EUR")).toBeDefined();
  });

  describe('renders Awe Grid component editable', () => {
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
              {"id": 1, "test": "test", "testSum": 12.4, "testAvg": 1212311.4, "testMax": 12.4, "testMin": 12.4},
              {
                "id": 2,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35}
              },
              {
                "id": 3,
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ],
            footer: {
              "test": "test",
              "testSum": {value: 55.4, label: "55,40 EUR"},
              "testAvg": {value: 535853.1, label: "$535,853.1"},
              "testMax": {value: 35, label: "35,0000 EUR"},
              "testMin": {value: 8, label: "8 EUR"}
            }
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
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.queryByRole("table")).toBeDefined();
      expect(screen.queryByRole("save-edit-row")).toBeNull();
      expect(screen.queryByRole("cancel-edit-row")).toBeNull();

      fireEvent.click(screen.getAllByRole("edit-row")[0]);
    });
  });

  describe('renders Awe Grid component editable being edited', () => {
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
                "test": "test",
                "testSum": 12.4,
                "testAvg": 1212311.4,
                "testMax": 12.4,
                "testMin": 12.4,
                $row: {editing: true, id: 1}
              },
              {
                "id": 2,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35}
              },
              {
                "id": 3,
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ],
            footer: {
              "test": "test",
              "testSum": {value: 55.4, label: "55,40 EUR"},
              "testAvg": {value: 535853.1, label: "$535,853.1"},
              "testMax": {value: 35, label: "35,0000 EUR"},
              "testMin": {value: 8, label: "8 EUR"}
            }
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
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("save-edit-row"));
    });

    it('... and cancel row', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("cancel-edit-row"));
    });

    it('... scrolls to the save button only when the row starts being edited', () => {
      const scrollIntoView = jest.fn();
      const original = Element.prototype.scrollIntoView;
      Element.prototype.scrollIntoView = scrollIntoView;
      try {
        const {store} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState});
        const address = preloadedState.components.grid.address;
        const values = preloadedState.components.grid.model.values;
        // The save button of the row being edited is shown
        expect(scrollIntoView).toHaveBeenCalled();
        scrollIntoView.mockClear();

        // A change of the grid that leaves the same row being edited (a click on another row, a typed value) must not
        // move the grid again: the cells would run away from under the pointer of the user
        act(() => {
          store.dispatch(updateModel(address, {values: values.map(item => ({...item, selected: item.id === 2}))}));
        });
        expect(scrollIntoView).not.toHaveBeenCalled();

        // The row being edited changes: its save button is shown
        act(() => {
          store.dispatch(updateModel(address, {
            values: values.map(item => ({...item, $row: {editing: item.id === 2, id: item.id}}))
          }));
        });
        expect(scrollIntoView).toHaveBeenCalled();
      } finally {
        Element.prototype.scrollIntoView = original;
      }
    });

    it('... and save row with keyboard', () => {
      jest.useFakeTimers();
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

      expect(screen.getByRole("table")).toBeDefined();
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
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

      expect(screen.getByRole("table")).toBeDefined();
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

  describe('renders Awe Grid component multioperation being edited', () => {
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
              {"id": 1, "test": "test", "testSum": 12.4, "testAvg": 1212311.4, "testMax": 12.4, "testMin": 12.4},
              {
                "id": 2,
                "test": "test",
                "testSum": {value: 35},
                "testAvg": {value: 312125},
                "testMax": {value: 35},
                "testMin": {value: 35},
                $row: {editing: true}
              },
              {
                "id": 3,
                "test": "test",
                "testSum": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testAvg": [{value: 83123, selected: true}, {value: 55212, selected: false}],
                "testMax": [{value: 8, selected: true}, {value: 55, selected: false}],
                "testMin": [{value: 8, selected: true}, {value: 55, selected: false}]
              }
            ],
            footer: {
              "test": "test",
              "testSum": {value: 35, label: "55,40 EUR"},
              "testAvg": {value: 312125, label: "$535,853.1"},
              "testMax": {value: 35, label: "35,0000 EUR"},
              "testMin": {value: 35, label: "8 EUR"}
            }
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
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("save-edit-row"));
    });

    it('... and cancel row', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      expect(screen.getByRole("save-edit-row")).toBeDefined();
      expect(screen.getByRole("cancel-edit-row")).toBeDefined();

      fireEvent.click(screen.getByRole("cancel-edit-row"));
    });

    it('... and save row with keyboard', () => {
      jest.useFakeTimers();
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

      expect(screen.getByRole("table")).toBeDefined();
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
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState, spyDispatch: true});
      const {address} = preloadedState.components.grid;

      expect(screen.getByRole("table")).toBeDefined();
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

  describe('renders Awe Grid component with some data and load all', () => {
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
              {"id": 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 2, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 4, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 5, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 6, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 7, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 8, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 9, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 10, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 11, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 13, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 14, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 15, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 16, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 17, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 18, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 19, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 20, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 21, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 22, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 23, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 24, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 25, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 26, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 27, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 28, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 29, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 30, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 31, col1: "val1", col2: "val2", col3: "val3", col4: 12}
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

    it('... and change page', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      const gotoPage = document.querySelector("[data-testid='grid-goto-page']");
      expect(gotoPage).not.toBeNull();

      fireEvent.change(gotoPage, {target: {value: "3"}});
      fireEvent.keyDown(gotoPage, {key: "Enter", code: "Enter", keyCode: 13});
    });

    it('... and sort', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      expect(screen.getByText("Column 1")).toBeDefined();

      fireEvent.click(screen.getByText("Column 1"));
    });

    it('... and filter', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();

      fireEvent.click(document.querySelector(".p-column-filter-menu-button"));

      //console.info(document.body);

      fireEvent.change(document.getElementsByTagName("input")[3], {target: {value: 'va'}});

      fireEvent.click(screen.getByText("Apply"));
    });

    it('... and select', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();

      fireEvent.click(document.getElementsByClassName("p-checkbox")[4]);
    });
  });

  describe('renders Awe Grid component with some data', () => {
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
              {"id": 1, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 2, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 3, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 4, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 5, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 6, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 7, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 8, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 9, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 10, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 11, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 12, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 13, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 14, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 15, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 16, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 17, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 18, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 19, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 20, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 21, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 22, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 23, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 24, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 25, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 26, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 27, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 28, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 29, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 30, col1: "val1", col2: "val2", col3: "val3", col4: 12},
              {"id": 31, col1: "val1", col2: "val2", col3: "val3", col4: 12}
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

    it('... and change page', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      const gotoPage = document.querySelector("[data-testid='grid-goto-page']");
      expect(gotoPage).not.toBeNull();

      fireEvent.change(gotoPage, {target: {value: "3"}});
      fireEvent.keyDown(gotoPage, {key: "Enter", code: "Enter", keyCode: 13});
    });

    it('... and sort', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();
      expect(screen.getByText("Column 1")).toBeDefined();

      fireEvent.click(screen.getByText("Column 1"));
    });

    it('... and filter', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();

      fireEvent.click(document.querySelector(".p-column-filter-menu-button"));

      //console.info(document.body);

      fireEvent.change(document.getElementsByTagName("input")[3], {target: {value: 'va'}});

      fireEvent.click(screen.getByText("Apply"));
    });

    it('... and select', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

      expect(screen.getByRole("table")).toBeDefined();

      fireEvent.click(document.getElementsByClassName("p-checkbox")[4]);
    });
  });

  it('renders Awe Grid component with context menu', () => {
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
          model: {values: [{id: 1, "test": "test"}]},
          attributes: {
            loadAll: true,
            enableFilters: true,
            rowNumbers: true,
            max: 30,
            columnModel: [{name: "test", label: "test", hidden: false}],
            headerModel: [],
            buttonModel: [],
            contextMenu: [{id: "contextMenuOption1"},{id: "contextMenuOption2"},{id: "contextMenuSeparator1"},{id: "contextMenuOption3"}]
          },
          specificAttributes: {sort: []}
        },
        contextMenuOption1: {
          address: {component: 'contextMenuOption1', view: 'report'},
          model: {values: []},
          attributes: { label: "context button 1", visible: true },
        },
        contextMenuOption2: {
          address: {component: 'contextMenuOption2', view: 'report'},
          model: {values: []},
          attributes: { label: "context button 2", visible: false },
        },
        contextMenuSeparator1: {
          address: {component: 'contextMenuSeparator1', view: 'report'},
          model: {values: []},
          attributes: { separator: true, visible: true },
        },
        contextMenuOption3: {
          address: {component: 'contextMenuOption3', view: 'report'},
          model: {values: []},
          attributes: { icon: "check", visible: true, actions: [{type:"add-row", target:"GrdSta"}] }
        }
      }
    };
    renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

    // check
    expect(screen.getByRole("table")).toBeDefined();
    expect(screen.getAllByRole("cell")[0]).toHaveClass("p-row-number-cell");

    fireEvent.contextMenu(document.getElementsByClassName("p-text-truncate")[0]);

    expect(document.querySelector(".p-contextmenu .p-menuitem")).not.toBeNull();
  });


  describe('moves rows with the keyboard', () => {
    const address = {component: 'grid', view: 'report'};

    const buildState = (attributes = {}) => ({
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
      components: {
        grid: {
          address,
          model: {
            values: [{id: 1, name: "one", selected: true}, {id: 2, name: "two"}, {id: 3, name: "three"}],
            records: 3
          },
          attributes: {
            editable: true,
            columnModel: [{name: "name", label: "Name"}],
            headerModel: [],
            buttonModel: [],
            ...attributes
          },
          specificAttributes: {sort: []}
        }
      }
    });

    const pressKey = (init) => fireEvent.keyDown(screen.getByRole("grid-container"), {bubbles: true, ...init});

    const movedActions = (dispatchSpy) => dispatchSpy.mock.calls
      .map(([action]) => action)
      .filter(action => action.type === 'ADD_ACTIONS_TOP')
      .flatMap(action => action.payload)
      .filter(action => action.type.startsWith("move-row"));

    it.each([
      ["ArrowUp", "move-row-up"],
      ["ArrowDown", "move-row-down"],
      ["Home", "move-row-top"],
      ["End", "move-row-bottom"]
    ])('moves the selected row of an editable grid with Alt+%s', (key, type) => {
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState: buildState(), spyDispatch: true});

      pressKey({key, altKey: true});

      expect(movedActions(dispatchSpy)).toEqual([{type, address}]);
    });

    it('does nothing without the Alt key', () => {
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState: buildState(), spyDispatch: true});

      pressKey({key: "ArrowUp"});

      expect(movedActions(dispatchSpy)).toEqual([]);
    });

    it('does nothing in a grid that is not editable', () => {
      const {dispatchSpy} = renderWithProviders(<AweGrid id="grid"/>, {
        preloadedState: buildState({editable: false}),
        spyDispatch: true
      });

      pressKey({key: "ArrowUp", altKey: true});

      expect(movedActions(dispatchSpy)).toEqual([]);
    });
  });

  describe('pagination range', () => {
    const PAGINATION_INFO = "[data-testid='grid-pagination-info']";
    const rangeState = ({ records = 25, first = 0, rows = 10, count = rows, attributes = {} } = {}) => ({
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            records,
            values: Array.from({length: count}, (_, index) => ({id: `r${index}`, name: `Name ${index}`}))
          },
          attributes: {
            columnModel: [{name: "name", label: "Name"}],
            headerModel: [],
            buttonModel: [],
            visible: true,
            max: rows,
            ...attributes
          },
          specificAttributes: {sort: [], first, rows}
        }
      }
    });
    const infoText = (container) => container.querySelector(PAGINATION_INFO).textContent.replace(/\s+/g, " ").trim();

    it.each([
      [0, 10, "1 - 10"],
      [10, 10, "11 - 20"],
      [20, 5, "21 - 25"]
    ])('shows the visible range and the total for the page starting at %i', (first, count, range) => {
      const {container} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState: rangeState({first, count})});

      expect(infoText(container)).toBe(`SCREEN_TEXT_GRID_VIEW ${range} SCREEN_TEXT_GRID_OF 25`);
    });

    it('shows an empty range when the grid has no records', () => {
      const {container} = renderWithProviders(<AweGrid id="grid"/>, {preloadedState: rangeState({records: 0, count: 0})});

      expect(infoText(container)).toBe("SCREEN_TEXT_GRID_VIEW 0 - 0 SCREEN_TEXT_GRID_OF 0");
    });

    it('does not show the range when the pagination is disabled', () => {
      const {container} = renderWithProviders(<AweGrid id="grid"/>, {
        preloadedState: rangeState({attributes: {disablePagination: true}})
      });

      expect(container.querySelector(PAGINATION_INFO)).toBeNull();
    });
  });

  describe('go to page', () => {
    const GOTO_PAGE = "[data-testid='grid-goto-page']";
    const gotoState = ({ records = 25, first = 0, rows = 10 } = {}) => ({
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {
            records,
            values: Array.from({length: Math.min(rows, records)}, (_, index) => ({id: `r${index}`, name: `Name ${index}`}))
          },
          attributes: {
            columnModel: [{name: "name", label: "Name"}],
            headerModel: [],
            buttonModel: [],
            visible: true,
            max: rows
          },
          specificAttributes: {sort: [], first, rows}
        }
      }
    });
    const pageChanges = (dispatchSpy) => dispatchSpy.mock.calls
      .map(([action]) => action)
      .filter(action => action.type === 'ADD_ACTIONS_TOP')
      .flatMap(action => action.payload)
      .filter(action => action.type === "change-page")
      .map(action => action.parameters);
    const renderGoto = (state = gotoState()) => {
      const rendered = renderWithProviders(<AweGrid id="grid"/>, {preloadedState: state, spyDispatch: true});
      const input = rendered.container.querySelector(GOTO_PAGE);
      // What the user reads: "Page [2] of 3"
      const label = [...(input?.closest(".pagination-goto-page")?.children || [])]
        .map(element => element.tagName === "INPUT" ? `[${element.value}]` : element.textContent).join(" ");
      return {...rendered, input, label};
    };
    const pressEnter = (input) => fireEvent.keyDown(input, {key: "Enter", code: "Enter", keyCode: 13});

    it('shows the current page and the total of pages between the arrows instead of the page links', () => {
      const {container, input, label} = renderGoto(gotoState({first: 10}));

      expect(input.value).toBe("2");
      expect(label).toBe("SCREEN_TEXT_GRID_PAGE [2] SCREEN_TEXT_GRID_OF 3");
      expect(container.querySelector(".p-paginator-pages")).toBeNull();
      const parts = [...container.querySelector(".p-paginator").children].map(element => element.className);
      const goto = parts.findIndex(className => className.includes("pagination-goto-page"));
      expect(parts[goto - 1]).toContain("p-paginator-prev");
      expect(parts[goto + 1]).toContain("p-paginator-next");
    });

    it('applies the typed page on Enter and cancels the event', () => {
      const {dispatchSpy, input} = renderGoto();

      fireEvent.change(input, {target: {value: "3"}});
      const notCancelled = pressEnter(input);

      expect(notCancelled).toBe(false);
      expect(pageChanges(dispatchSpy)).toEqual([{page: 3, first: 20, rows: 10, max: 10}]);
    });

    it('does not let Enter reach the grid container, which would save the row', () => {
      jest.useFakeTimers();
      try {
        const {dispatchSpy, input} = renderGoto();

        fireEvent.change(input, {target: {value: "2"}});
        pressEnter(input);
        act(() => jest.advanceTimersByTime(200));

        const actions = dispatchSpy.mock.calls.map(([action]) => action)
          .filter(action => action.type === 'ADD_ACTIONS_TOP').flatMap(action => action.payload);
        expect(actions.map(action => action.type)).toEqual(["change-page", "filter"]);
      } finally {
        jest.useRealTimers();
      }
    });

    it('requests the page once when the input loses the focus before the page is shown', () => {
      const {dispatchSpy, input} = renderGoto();

      fireEvent.change(input, {target: {value: "3"}});
      pressEnter(input);
      fireEvent.blur(input);

      expect(pageChanges(dispatchSpy)).toEqual([{page: 3, first: 20, rows: 10, max: 10}]);
    });

    it('does not apply the page while typing', () => {
      const {dispatchSpy, input} = renderGoto();

      fireEvent.change(input, {target: {value: "3"}});
      fireEvent.keyDown(input, {key: "3", code: "Digit3", keyCode: 51});

      expect(pageChanges(dispatchSpy)).toEqual([]);
      expect(input.value).toBe("3");
    });

    it('applies a different page on blur', () => {
      const {dispatchSpy, input} = renderGoto();

      fireEvent.change(input, {target: {value: "2"}});
      fireEvent.blur(input);

      expect(pageChanges(dispatchSpy)).toEqual([{page: 2, first: 10, rows: 10, max: 10}]);
    });

    it('does not apply the current page on Enter or blur', () => {
      const {dispatchSpy, input} = renderGoto(gotoState({first: 10}));

      pressEnter(input);
      fireEvent.blur(input);

      expect(pageChanges(dispatchSpy)).toEqual([]);
      expect(input.value).toBe("2");
    });

    it.each(["0", "4", ""])('ignores the out of range page "%s" and shows the current page again', (value) => {
      const {dispatchSpy, input} = renderGoto();

      fireEvent.change(input, {target: {value}});
      pressEnter(input);

      expect(pageChanges(dispatchSpy)).toEqual([]);
      expect(input.value).toBe("1");

      fireEvent.change(input, {target: {value}});
      fireEvent.blur(input);

      expect(pageChanges(dispatchSpy)).toEqual([]);
      expect(input.value).toBe("1");
    });

    it('shows the page of the grid when it changes', () => {
      const {input, store} = renderGoto();

      act(() => {
        store.dispatch(updateSpecificAttributes({component: "grid", view: "report"}, {first: 20}));
      });

      expect(input.value).toBe("3");
    });

    it('is a number input', () => {
      const {input} = renderGoto();

      expect(input.tagName).toBe("INPUT");
      expect(input.type).toBe("number");
    });

    it('is disabled with a single page', () => {
      const {input, label} = renderGoto(gotoState({records: 10}));

      expect(input.disabled).toBe(true);
      expect(input.value).toBe("1");
      expect(label).toBe("SCREEN_TEXT_GRID_PAGE [1] SCREEN_TEXT_GRID_OF 1");
    });

    it('is disabled without records', () => {
      const {input, label} = renderGoto(gotoState({records: 0}));

      expect(input.disabled).toBe(true);
      expect(input.value).toBe("0");
      expect(label).toBe("SCREEN_TEXT_GRID_PAGE [0] SCREEN_TEXT_GRID_OF 0");
    });
  });
});
