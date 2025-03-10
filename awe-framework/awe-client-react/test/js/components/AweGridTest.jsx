import React, {Suspense} from 'react';
import {fireEvent, screen} from '@testing-library/react';

import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

describe('awe-react-client/test/js/components/AweGridTest.jsx', () => {

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
    expect(screen.getAllByRole("button")[0]).toHaveClass("p-column-filter-menu-button");
  });

  it('renders Awe Grid component with footer', () => {
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
            ]
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
    expect(screen.findByText("55,40 EUR")).toBeDefined();
    expect(screen.findByText("$535,853.1")).toBeDefined();
    expect(screen.findByText("35,0000 EUR")).toBeDefined();
    expect(screen.findByText("8 EUR")).toBeDefined();
  });

  it('renders Awe Grid component with footer without rows', () => {
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
          model: {values: []},
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
    expect(screen.findByText("0,00 EUR")).toBeDefined();
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
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

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
    });

    it('... and cancel row with keyboard', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

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
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

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
    });

    it('... and cancel row with keyboard', () => {
      renderWithProviders(<AweGrid id="grid"/>, {preloadedState});

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
      expect(screen.getByLabelText("Page 3")).toBeDefined();

      fireEvent.click(screen.getByLabelText("Page 3"));
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

      fireEvent.click(screen.getAllByRole("button")[0]);

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
      expect(screen.getByLabelText("Page 3")).toBeDefined();

      fireEvent.click(screen.getByLabelText("Page 3"));
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

      fireEvent.click(screen.getAllByRole("button")[0]);

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

    //console.info(screen.getByRole("menubar"));
    expect(screen.getAllByRole("menuitem")[0]).toHaveClass("p-menuitem");
  });

});
