import React from 'react';
import useGridService from "../../../../src/services/components/GridService";
import {renderWithProviders} from "../../test-utils";
import {MemoryRouter} from "react-router";
import {act, waitFor} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../../src/redux/actions/settings";
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

    expect(Object.keys(actions).length).toBe(38);
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
     const clipboard = spyOn(navigator.clipboard, "writeText");

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
});
