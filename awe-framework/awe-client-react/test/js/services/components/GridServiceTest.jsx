import React from 'react';
import useGridService from "../../../../src/services/components/GridService";
import {renderWithProviders} from "../../test-utils";
import {MemoryRouter} from "react-router";
import {act, waitFor} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../../src/redux/actions/settings";
import {configureStore} from "@reduxjs/toolkit";
import createRootReducer from "../../../../src/redux/reducers";
import {initializeDependencies} from "../../../../src/redux/actions/dependencies";
import ComponentRegistry from "../../../../src/redux/registry/ComponentRegistry";

describe('awe-react-client/test/js/services/components/GridServiceTest.jsx', function () {
  let gridService;
  let actions;
  let props;
  let originalClipboard;

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      grid: {
        address: {component: "grid", view: "report"},
        model: {values: []},
        attributes: {
          columnModel: [
            {name: "col1", label: "Column 1"},
            {name: "col2", label: "Column 2"},
            {name: "col3", label: "Column 3"}
          ]
        },
      }
    }
  };

  const setup = (props = {}) => {
    return { ...renderWithProviders(<MemoryRouter initialEntries={["/"]}>
        <TestHarness />
      </MemoryRouter>, {preloadedState: props, spyDispatch: true})
    };
  };

  function TestHarness() {
    gridService = useGridService();
    actions = gridService.getActions();
    return null;
  }

  beforeEach(() => {
    ComponentRegistry.clearAll();
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    originalClipboard = navigator.clipboard;
    if (!navigator.clipboard) {
      Object.defineProperty(navigator, "clipboard", {
        value: {},
        configurable: true
      });
    }
    if (!navigator.clipboard.writeText) {
      navigator.clipboard.writeText = () => Promise.resolve();
    }
  });

  afterEach(() => {
    ComponentRegistry.clearAll();
    jest.restoreAllMocks();
    if (originalClipboard) {
      Object.defineProperty(navigator, "clipboard", {
        value: originalClipboard,
        configurable: true
      });
    } else if (navigator.clipboard) {
      delete navigator.clipboard;
    }
  });

  // Get all screen actions
  it('should get all grid actions', function () {
    setup(preloadedState);

    expect(Object.keys(actions).length).toBe(47);
  });

  it('should launch an add-row action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["add-row"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an add-row action on last position', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["add-row-bottom"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an add-row action after the selected row', async function () {
    const { store } = setup(preloadedState);
    act(() => actions["add-row-down"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an add-row action before the selected row', async function () {
    const { store } = setup(preloadedState);
    act(() => actions["add-row-up"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an add-row action on multioperation grid', async function () {
    const { store } = setup({...preloadedState,
      components: {grid: {...preloadedState.components.grid, attributes: {multioperation: true}}}});
    act(() => actions["add-row-top"]({
        address: {component: "grid", view: "report"},
        parameters: {row: {id: "tutu"}, rowId: "1"}
      }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an copy-row action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["copy-row"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an copy-row-top action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["copy-row-top"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it.each(["move-row-top", "move-row-up", "move-row-down", "move-row-bottom"])('should register and launch a %s action', async function (type) {
    const { store } = setup({
      ...preloadedState,
      components: {
        grid: {
          ...preloadedState.components.grid,
          model: {values: [{id: "1"}, {id: "2", selected: true}, {id: "3"}]}
        }
      }
    });

    expect(typeof actions[type]).toBe("function");
    act(() => actions[type]({address: {component: "grid", view: "report"}, parameters: {}}, props));

    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an copy-row-bottom action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["copy-row-bottom"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an copy-row-before action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["copy-row-up"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an copy-row-after action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["copy-row-down"]({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an delete-row action', async function () {
    const { store } = setup(preloadedState);

    act(() => actions["delete-row"]({address: {component: "grid", view: "report"}, parameters: {rowId: "1"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an delete-row action on multioperation grid', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid, attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["delete-row"]({address: {component: "grid", view: "report"}, parameters: {rowId: "1"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an update-row action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid, model: {
            values: [
              {id: "1", tutu: "lala"},
              {id: "2", tutu: "lala2"},
              {id: "3", tutu: "lala3"}
            ]
          }
        }
      }
    });

    act(() => actions["update-row"]({
        address: {component: "grid", view: "report"},
        parameters: {row: {id: "1", lala: "tutu"}}
      }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an update-row action on multioperation grid', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid, attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["update-row"]({
        address: {component: "grid", view: "report"},
        parameters: {row: {id: "1", lala: "tutu"}}
      }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch an edit-row action on multioperation grid', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          attributes: {multioperation: true},
          model: {
            values: [
              {id: "1", tutu: "lala"},
              {id: "2", tutu: "lala2"},
              {id: "3", tutu: "lala3", $row: {editing: false}}
            ]
          }
        }
      }
    });

    act(() => actions["edit-row"]({address: {component: "grid", view: "report"}, parameters: {row: 1}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a save-row action on multioperation grid', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          attributes: {multioperation: true},
          model: {
            values: [
              {id: "1", tutu: "lala", $row: {editing: true, editingRow: {id: "1", tutu: "lale"}}},
              {id: "2", tutu: "lala2"},
              {id: "3", tutu: "lala3", $row: {editing: false}}
            ]
          }
        }
      }
    });

    act(() => actions["save-row"]({address: {component: "grid", view: "report"}, parameters: {rowIndex: 0}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a cancel-row action on multioperation grid', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          attributes: {multioperation: true},
          model: {
            values: [
              {id: "1", tutu: "lala", $row: {editing: true, editingRow: {id: "1", tutu: "lale"}}},
              {id: "2", tutu: "lala2"},
              {id: "3", tutu: "lala3", $row: {editing: false}}
            ]
          }
        }
      }
    });

    act(() => actions["cancel-row"]({address: {component: "grid", view: "report"}, parameters: {rowIndex: 0}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a change-page action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["change-page"]({
        address: {component: "grid", view: "report"},
        parameters: {page: 1, first: 1, rows: 1, max: 100}
      }, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a change-sort action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["change-sort"]({address: {component: "grid", view: "report"}, parameters: {sort: []}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a change-filter action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["change-filter"]({address: {component: "grid", view: "report"}, parameters: {filters: {}}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a check-records-saved action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["check-records-saved"]({address: {component: "grid", view: "report"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a check-records-saved action failed', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala", $row: {editing: true}}]}
        }
      }
    });

    act(() => actions["check-records-saved"]({address: {component: "grid", view: "report"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a check-records-generated action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala", $row: {operation: "INSERT"}}]}
        }
      }
    });

    act(() => actions["check-records-generated"]({address: {component: "grid", view: "report"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a check-records-generated action failed', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          attributes: {multioperation: true},
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["check-records-generated"]({address: {component: "grid", view: "report"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a validate-row action', async function () {
    const { store } = setup({
      ...preloadedState, components: {
        grid: {
          ...preloadedState.components.grid,
          attributes: {
            ...preloadedState.components.grid.attributes,
            multioperation: true
          },
          model: {values: [{id: "1", tutu: "lala"}]}
        }
      }
    });

    act(() => actions["validate-row"]({address: {component: "grid", view: "report"}}, props));

    // Spies
    await waitFor(() => {
      expect(store.dispatch).toHaveBeenCalled();
    });
  });

  it('should launch a verify-row-validation action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             ...preloadedState.components.grid.attributes,
             multioperation: true
           },
           model: {values: [{id: "1", tutu: "lala"}]}
         }
       }
     });

    act(() => actions["verify-row-validation"]({address: {component: "grid", view: "report", row: "1"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a verify-row-validation action failed', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             ...preloadedState.components.grid.attributes,
             multioperation: true
           },
           model: {values: [{id: "1", tutu: {value: "lala", error: {message: "ERROR", values: []}}}]}
         }
       }
     });

     act(() => actions["verify-row-validation"]({address: {component: "grid", view: "report", row: "1"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a copy-selected-rows-clipboard action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             ...preloadedState.components.grid.attributes,
             multioperation: true,
             columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: true},
               {id: "5", col2: "tutu4", col3: 1231, selected: true},
               {id: "6", col1: "lala5", col2: "tutu5", selected: true}
             ]
           }
         }
       }
     });

     // Spies
     const clipboard = jest.spyOn(navigator.clipboard, "writeText");

     act(() => actions["copy-selected-rows-clipboard"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     expect(clipboard).toHaveBeenCalledWith("Column 1\tColumn 2\tColumn 3\n" +
       "lala2\ttutu2\t121\n" +
       "lala4\t\t21.2\n" +
       "\ttutu4\t1231\n" +
       "lala5\ttutu5\t");
   });

   it('should launch a select-first-row action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: true},
               {id: "5", col2: "tutu4", col3: 1231, selected: true},
               {id: "6", col1: "lala5", col2: "tutu5", selected: true}
             ]
           }
         }
       }
     });

     act(() => actions["select-first-row"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a select-last-row action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: true},
               {id: "5", col2: "tutu4", col3: 1231, selected: true},
               {id: "6", col1: "lala5", col2: "tutu5", selected: true}
             ]
           }
         }
       }
     });

     act(() => actions["select-last-row"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a select-all-rows action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: true},
               {id: "5", col2: "tutu4", col3: 1231, selected: true},
               {id: "6", col1: "lala5", col2: "tutu5", selected: true}
             ]
           }
         }
       }
     });

     act(() => actions["select-all-rows"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch an unselect-all-rows action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: true},
               {id: "5", col2: "tutu4", col3: 1231, selected: true},
               {id: "6", col1: "lala5", col2: "tutu5", selected: true}
             ]
           }
         }
       }
     });

     act(() => actions["unselect-all-rows"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a check-one-selected action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: false},
               {id: "5", col2: "tutu4", col3: 1231, selected: false},
               {id: "6", col1: "lala5", col2: "tutu5", selected: false}
             ]
           }
         }
       }
     });

     act(() => actions["check-one-selected"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a check-one-selected action invalid', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: true},
               {id: "5", col2: "tutu4", col3: 1231, selected: false},
               {id: "6", col1: "lala5", col2: "tutu5", selected: false}
             ]
           }
         }
       }
     });

     act(() => actions["check-one-selected"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a check-some-selected action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: false},
               {id: "5", col2: "tutu4", col3: 1231, selected: true},
               {id: "6", col1: "lala5", col2: "tutu5", selected: false}
             ]
           }
         }
       }
     });

     act(() => actions["check-some-selected"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a check-some-selected action invalid', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true, columnModel: [
               {name: "col1", label: "Column 1"},
               {name: "col2", label: "Column 2"},
               {name: "col3", label: "Column 3"}
             ]
           },
           model: {
             values: [
               {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
               {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: false},
               {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
               {id: "4", col1: "lala4", col3: 21.2, selected: false},
               {id: "5", col2: "tutu4", col3: 1231, selected: false},
               {id: "6", col1: "lala5", col2: "tutu5", selected: false}
             ]
           }
         }
       }
     });

     act(() => actions["check-some-selected"]({address: {component: "grid", view: "report"}}, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a change-column-label action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [{name: "tutu", label: "Tutu"},
               {name: "lala", label: "Lala"},
               {name: "lerele", label: "Lereele"}]
           },
           model: {values: [{id: "1", tutu: "lala"}]}
         }
       }
     });

     act(() => actions["change-column-label"]({
         address: {component: "grid", view: "report"},
         parameters: {column: "tutu", label: "Nuevo tutu"}
       }, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a show-columns action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [
               {name: "tutu", label: "Tutu", hidden: true},
               {name: "tutu2", label: "Tutu2", hidden: true},
               {name: "tutu3", label: "Tutu3", hidden: true}]
           },
           model: {values: [{id: "1", tutu: "lala", tutu2: "lala2", tutu3: "lala3"}]}
         }
       }
     });

     act(() => actions["show-columns"]({
         address: {component: "grid", view: "report"},
         parameters: {columns: ["tutu", "tutu2"]}
       }, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a hide-columns action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [
               {name: "tutu", label: "Tutu", hidden: false},
               {name: "tutu2", label: "Tutu2", hidden: false},
               {name: "tutu3", label: "Tutu3", hidden: false}]
           },
           model: {values: [{id: "1", tutu: "lala", tutu2: "lala2", tutu3: "lala3"}]}
         }
       }
     });

     act(() => actions["hide-columns"]({
         address: {component: "grid", view: "report"},
         parameters: {columns: ["tutu", "tutu3"]}
       }, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a toggle-columns-visibility action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [
               {name: "tutu", label: "Tutu", hidden: false},
               {name: "tutu2", label: "Tutu2", hidden: false},
               {name: "tutu3", label: "Tutu3", hidden: false}]
           },
           model: {values: [{id: "1", tutu: "lala", tutu2: "lala2", tutu3: "lala3"}]}
         }
       }
     });

     act(() => actions["toggle-columns-visibility"]({
         address: {component: "grid", view: "report"},
         parameters: {columns: ["tutu", "tutu3"], show: false}
       }, props
     ));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch an add-columns action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [
               {name: "tutu", label: "Tutu", hidden: false},
               {name: "tutu2", label: "Tutu2", hidden: false},
               {name: "tutu3", label: "Tutu3", hidden: false}]
           },
           model: {values: [{id: "1", tutu: "lala", tutu2: "lala2", tutu3: "lala3"}]}
         }
       }
     });

     act(() => actions["add-columns"]({
         address: {component: "grid", view: "report"},
         parameters: {
           columns: [{name: "tutu4", label: "Tutu 4", hidden: false}, {
             name: "tutu5",
             label: "Tutu 5",
             hidden: false
           }], show: false
         }
       }, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch a replace-columns action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [
               {name: "tutu", label: "Tutu", hidden: false},
               {name: "tutu2", label: "Tutu2", hidden: false},
               {name: "tutu3", label: "Tutu3", hidden: false}]
           },
           model: {values: [{id: "1", tutu: "lala", tutu2: "lala2", tutu3: "lala3"}]}
         }
       }
     });

     act(() => actions["replace-columns"]({
         address: {component: "grid", view: "report"},
         parameters: {
           columns: [{name: "tutu4", label: "Tutu 4", hidden: false}, {
             name: "tutu5",
             label: "Tutu 5",
             hidden: false
           }], show: false
         }
       }, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   it('should launch an update-cell action', async function () {
     const { store } = setup({
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {
             multioperation: true,
             columnModel: [
               {name: "tutu", label: "Tutu", hidden: false},
               {name: "tutu2", label: "Tutu2", hidden: false},
               {name: "tutu3", label: "Tutu3", hidden: false}]
           },
           model: {values: [{id: "1", tutu: "lala", tutu2: "lala2", tutu3: "lala3"}]}
         }
       }
     });

     act(() => actions["update-cell"]({
         address: {component: "grid", view: "report", column: 'tutu', row: '1'},
         parameters: {data: "LALA"}
       }, props));

     // Spies
     await waitFor(() => {
       expect(store.dispatch).toHaveBeenCalled();
     });
   });

   describe('after-* grid notifications (#802)', () => {
     const address = {component: "grid", view: "report"};
     const gridState = {
       ...preloadedState, components: {
         grid: {
           ...preloadedState.components.grid,
           attributes: {editable: true, columnModel: [{name: "col1", label: "Column 1"}]},
           model: {values: [{id: "1", col1: "a", selected: true}, {id: "2", col1: "b", selected: true}]}
         }
       }
     };
     let recorded;
     const setupRecorded = (state) => {
       recorded = [];
       const recorder = () => (next) => (action) => {
         recorded.push(action);
         return next(action);
       };
       const store = configureStore({
         reducer: createRootReducer({}),
         preloadedState: state,
         middleware: (getDefaultMiddleware) => getDefaultMiddleware({serializableCheck: false}).concat(recorder)
       });
       return renderWithProviders(<MemoryRouter initialEntries={["/"]}><TestHarness /></MemoryRouter>, {store});
     };
     const raisedEvents = () => recorded.filter(({type}) => type === "SET_RUNTIME_EVENT").map(({event}) => event);
     const accepted = (action) => recorded.some(({type, payload}) => type === "ACCEPT_ACTION" && payload === action);
     const selection = (store) => store.getState().components.grid.model.values.map(({selected}) => selected);

     it.each(["after-add-row", "after-delete-row"])('%s accepts the action and raises the event', async (name) => {
       const { store } = setupRecorded(gridState);
       const action = {type: name, address};

       act(() => actions[name](action, props));

       await waitFor(() => expect(accepted(action)).toBe(true));
       expect(raisedEvents()).toEqual([name]);
       expect(selection(store)).toEqual([true, true]);
     });

     const stateWith = (values, attributes = {}) => ({
       ...gridState,
       components: {
         grid: {
           ...gridState.components.grid,
           attributes: {editable: true, validateOnSave: false, columnModel: [{name: "col1", label: "Column 1"}], ...attributes},
           model: {values}
         }
       }
     });
     const editingValues = [
       {id: "1", col1: "new", selected: true, $row: {editing: true, editingRow: {id: "1", col1: "a"}}},
       {id: "2", col1: "b", selected: false}
     ];

     it('save-row raises before-save-row first, like AngularJS', async () => {
       setupRecorded(stateWith(editingValues));

       act(() => actions["save-row"]({type: "save-row", address}, props));

       await waitFor(() => expect(raisedEvents()).toContain("after-save-row"));
       expect(raisedEvents().slice(0, 3)).toEqual(["before-save-row", "save-row", "after-save-row"]);
     });

     it('save-row raises before-save-row even when the validation rejects the row', async () => {
       const invalid = [{...editingValues[0], $attrs: {col1: {error: true}}}, editingValues[1]];
       const { store } = setupRecorded(stateWith(invalid, {validateOnSave: true}));

       act(() => actions["save-row"]({type: "save-row", address}, props));

       await waitFor(() => expect(raisedEvents()).toContain("before-save-row"));
       expect(raisedEvents()).not.toContain("save-row");
       expect(store.getState().components.grid.model.values[0].$row.editing).toBe(true);
     });

     it('cancel-row raises before-cancel-row before the row is restored', async () => {
       setupRecorded(stateWith(editingValues));

       act(() => actions["cancel-row"]({type: "cancel-row", address}, props));

       await waitFor(() => expect(raisedEvents()).toContain("after-cancel-row"));
       expect(raisedEvents()[0]).toBe("before-cancel-row");
     });

     it('delete-row raises delete-row once the row is removed and before after-delete-row', async () => {
       const { store } = setupRecorded(stateWith([{id: "1", col1: "a", selected: true}, {id: "2", col1: "b"}]));

       act(() => actions["delete-row"]({type: "delete-row", address, parameters: {rowId: "1"}}, props));

       await waitFor(() => expect(raisedEvents()).toEqual(["delete-row", "after-delete-row"]));
       expect(store.getState().components.grid.model.values.map(({id}) => id)).toEqual(["2"]);
     });

     describe.each([
       ["check-records-saved", [{id: "1", col1: "a"}], [{id: "1", col1: "a", $row: {editing: true}}]],
       ["check-records-generated", [{id: "1", col1: "a", $row: {operation: "INSERT"}}], [{id: "1", col1: "a"}]],
       ["check-one-selected", [{id: "1", col1: "a", selected: true}], [{id: "1", col1: "a"}]],
       ["check-some-selected", [{id: "1", col1: "a", selected: true}], [{id: "1", col1: "a"}]]
     ])('%s', (name, passing, failing) => {
       it.each([["passes", passing, true], ["fails", failing, false]])('is raised when the check %s, like AngularJS', async (_, values, ok) => {
         setupRecorded(stateWith(values));
         const action = {type: name, address};

         act(() => actions[name](action, props));

         await waitFor(() => expect(raisedEvents()).toEqual([name]));
         expect(recorded.some(({type, payload}) => type === "REJECT_ACTION" && payload === action)).toBe(!ok);
       });
     });

     it('launches the dependency of a grid event with the real dependency check', async () => {
       const dependent = {
         address: {component: "other", view: "report"},
         attributes: {visible: true},
         model: {values: []},
         dependencies: [{
           type: "and",
           elements: [{id: "grid", event: "delete-row", checkChanges: false}],
           target: "label",
           source: "value",
           value: "deleted",
           initial: false
         }]
       };
       const state = stateWith([{id: "1", col1: "a", selected: true}]);
       state.components.other = dependent;
       state.view = {view: "report", report: {name: "Report"}};
       const { store } = setupRecorded(state);
       act(() => initializeDependencies("report", store.getState(), store.dispatch));

       act(() => actions["delete-row"]({type: "delete-row", address, parameters: {rowId: "1"}}, props));

       await waitFor(() => expect(store.getState().components.other.attributes.label).toBe("deleted"));
     });

     it('before-save-row does not check the column dependencies of the row being edited (AngularJS only checks listeners)', async () => {
       const mirror = (from) => [{
         source: "criteria-value", target: "input", query: "Cpy",
         elements: [{id: "grid", column1: from, attribute1: "selectedRowValue", alias: "Cpy"}]
       }];
       const state = stateWith([{id: "new-row-0", Num: 0, Num2: null, selected: true, $row: {editing: true, operation: "INSERT"}}], {
         columnModel: [
           {id: "Num", name: "Num", label: "Num", dependencies: mirror("Num2")},
           {id: "Num2", name: "Num2", label: "Num2", dependencies: mirror("Num")}
         ]
       });
       const { store } = setupRecorded(state);
       act(() => initializeDependencies("report", store.getState(), store.dispatch));
       // The user types in the cell: the model changes without checking the dependencies
       act(() => store.dispatch({type: "UPDATE_MODEL", address, data: {
         values: [{id: "new-row-0", Num: 0, Num2: 5, selected: true, $row: {editing: true, operation: "INSERT"}}]
       }}));

       act(() => actions["save-row"]({type: "save-row", address}, props));

       await waitFor(() => expect(raisedEvents()).toContain("after-save-row"));
       // The copy is launched by the change that save-row makes, not by the notification that precedes it
       const firstCopy = recorded.findIndex(({type}) => type === "UPDATE_MULTIPLE_MODELS");
       const saveRow = recorded.findIndex(({type, event}) => type === "SET_RUNTIME_EVENT" && event === "save-row");
       expect(saveRow).toBeGreaterThan(-1);
       expect(firstCopy).toBeGreaterThan(saveRow);
     });

     it('cancel-row raises after-cancel-row once the edition is cancelled, like AngularJS', async () => {
       const editing = {
         ...gridState,
         components: {
           grid: {
             ...gridState.components.grid,
             model: {
               values: [
                 {id: "1", col1: "new", selected: true, $row: {editing: true, editingRow: {id: "1", col1: "a"}}},
                 {id: "2", col1: "b", selected: false}
               ]
             }
           }
         }
       };
       const { store } = setupRecorded(editing);

       act(() => actions["cancel-row"]({type: "cancel-row", address}, props));

       await waitFor(() => expect(raisedEvents()).toEqual(["before-cancel-row", "cancel-row", "after-cancel-row", "select-row"]));
       expect(selection(store)).toEqual([false, false]);
     });

     it('save-row ends like the AngularJS after-save-row: event once and no row selected', async () => {
       const editing = {
         ...gridState,
         components: {
           grid: {
             ...gridState.components.grid,
             attributes: {editable: true, validateOnSave: false, columnModel: [{name: "col1", label: "Column 1"}]},
             model: {
               values: [
                 {id: "1", col1: "new", selected: true, $row: {editing: true, editingRow: {id: "1", col1: "a"}}},
                 {id: "2", col1: "b", selected: true}
               ]
             }
           }
         }
       };
       const { store } = setupRecorded(editing);

       act(() => actions["save-row"]({type: "save-row", address}, props));

       await waitFor(() => expect(raisedEvents()).toContain("after-save-row"));
       expect(raisedEvents().filter((event) => event === "after-save-row")).toHaveLength(1);
       expect(selection(store)).toEqual([false, false]);
     });

     it.each([true, false])('delete-row ends like the AngularJS after-delete-row (multioperation %s)', async (multioperation) => {
       const deleting = {
         ...gridState,
         components: {
           grid: {
             ...gridState.components.grid,
             attributes: {multioperation, columnModel: [{name: "col1", label: "Column 1"}]},
             model: {values: [{id: "1", col1: "a", selected: true}, {id: "2", col1: "b", selected: false}, {id: "3", col1: "c", selected: true}]}
           }
         }
       };
       const { store } = setupRecorded(deleting);

       act(() => actions["delete-row"]({type: "delete-row", address, parameters: {rowId: "1"}}, props));

       await waitFor(() => expect(raisedEvents()).toEqual(["delete-row", "after-delete-row"]));
       expect(selection(store).filter(Boolean)).toEqual([]);
     });

     it('add-row raises after-add-row once and keeps the selection, like AngularJS', async () => {
       const { store } = setupRecorded(gridState);

       act(() => actions["add-row"]({type: "add-row", address, parameters: {row: {id: "new", col1: "x"}}}, props));

       await waitFor(() => expect(raisedEvents()).toContain("after-add-row"));
       expect(raisedEvents().filter((event) => event === "after-add-row")).toHaveLength(1);
       // Only the new row, which add-row selects itself: the notification does not unselect it
       expect(selection(store).filter(Boolean)).toHaveLength(1);
     });

     it.each([
       ["a component that is not on screen", {...gridState, components: {}}],
       ["a model without values", {...gridState, components: {grid: {...gridState.components.grid, model: {}}}}]
     ])('after-save-row accepts the action for %s', async (_, state) => {
       setupRecorded(state);
       const action = {type: "after-save-row", address};

       act(() => actions["after-save-row"](action, props));

       await waitFor(() => expect(accepted(action)).toBe(true));
     });

     it.each(["after-save-row", "after-cancel-row"])('%s accepts the action, raises the event and unselects the rows', async (name) => {
       const { store } = setupRecorded(gridState);
       const action = {type: name, address};

       act(() => actions[name](action, props));

       await waitFor(() => expect(accepted(action)).toBe(true));
       expect(raisedEvents()).toEqual([name, "select-row"]);
       expect(selection(store)).toEqual([false, false]);
     });
   });
});
