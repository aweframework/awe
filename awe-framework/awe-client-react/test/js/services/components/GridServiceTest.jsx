import GridService from "../../../../src/services/components/GridService";

describe('awe-react-client/test/js/services/components/GridServiceTest.jsx', function () {
  let gridService;
  let props;

  beforeEach(function () {
    gridService = new GridService();
    props = {
      acceptAction: jasmine.createSpy("accept"),
      rejectAction: jasmine.createSpy("reject"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      updateModelWithDependencies: jasmine.createSpy("updateModelWithDependencies"),
      updateSpecificAttributes: jasmine.createSpy("updateSpecificAttributes"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      validateRow: jasmine.createSpy("validateRow"),
      afterSaveRow: jasmine.createSpy("afterSaveRow"),
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
      },
      settings: {}
    };
  });

  // Get all screen actions
  it('should get all grid actions', function () {
    let actions = gridService.getActions();
    expect(Object.keys(actions).length).toBe(38);
  });

  it('should launch an add-row action', function () {
    gridService.onGridAddRow({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: 'tutu',
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: 'tutu', col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an add-row action on last position', function () {
    gridService.onGridAddRowLast({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: 'tutu',
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: 'tutu', col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an add-row action after the selected row', function () {
    gridService.onGridAddRowAfter({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: 'tutu',
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: 'tutu', col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an add-row action before the selected row', function () {
    gridService.onGridAddRowBefore({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1"}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: 'tutu',
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: 'tutu', col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an add-row action on multioperation grid', function () {
    gridService.onGridAddRowFirst({
        address: {component: "grid", view: "report"},
        parameters: {row: {id: "tutu"}, rowId: "1"}
      },
      {...props, components: {grid: {...props.components.grid, attributes: {multioperation: true}}}});

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: 'tutu',
        $row: {
          operation: 'INSERT',
          editing: true,
          editingRow: {id: 'tutu', $row: {operation: 'INSERT', editing: false}}
        },
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an copy-row action', function () {
    gridService.onGridCopyRow({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: "copied-row-" + gridService.copiedRows,
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: "copied-row-" + gridService.copiedRows, col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an copy-row-top action', function () {
    gridService.onGridCopyRowFirst({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1, values: [{
        id: "copied-row-" + gridService.copiedRows,
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: "copied-row-" + gridService.copiedRows, col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }], event: 'after-add-row'
    });
  });

  it('should launch an copy-row-bottom action', function () {
    gridService.onGridCopyRowLast({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1, values: [{
        id: "copied-row-" + gridService.copiedRows,
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: "copied-row-" + gridService.copiedRows, col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }], event: 'after-add-row'
    });
  });

  it('should launch an copy-row-before action', function () {
    gridService.onGridCopyRowBefore({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1, values: [{
        id: "copied-row-" + gridService.copiedRows,
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: "copied-row-" + gridService.copiedRows, col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }], event: 'after-add-row'
    });
  });

  it('should launch an copy-row-after action', function () {
    gridService.onGridCopyRowAfter({
      address: {component: "grid", view: "report"},
      parameters: {row: {id: "tutu"}, rowId: "1", $row: {}}
    }, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: 'grid',
      view: 'report'
    }, {event: 'add-row'});
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'}, {
      records: 1,
      values: [{
        id: "copied-row-" + gridService.copiedRows,
        col1: null,
        col2: null,
        col3: null,
        $row: {editingRow: {id: "copied-row-" + gridService.copiedRows, col1: null, col2: null, col3: null, $row: {}}},
        selected: true
      }],
      event: 'after-add-row'
    });
  });

  it('should launch an delete-row action', function () {
    gridService.onGridDeleteRow({address: {component: "grid", view: "report"}, parameters: {rowId: "1"}}, props);

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"}, {
      records: 0,
      values: []
    });
  });

  it('should launch an delete-row action on multioperation grid', function () {
    gridService.onGridDeleteRow({address: {component: "grid", view: "report"}, parameters: {rowId: "1"}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid, attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
      component: "grid",
      view: "report"
    }, {event: 'after-delete-row', values: [{id: "1", tutu: "lala", $row: {operation: "DELETE"}}]});
  });

  it('should launch an update-row action', function () {
    gridService.onGridUpdateRow({
        address: {component: "grid", view: "report"},
        parameters: {row: {id: "1", lala: "tutu"}}
      },
      {
        ...props, components: {
          grid: {
            ...props.components.grid, model: {
              values: [
                {id: "1", tutu: "lala"},
                {id: "2", tutu: "lala2"},
                {id: "3", tutu: "lala3"}
              ]
            }
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'after-edit-row', values: [{id: "1", tutu: "lala", lala: "tutu", $row: {}},
          {id: "2", tutu: "lala2", $row: {}},
          {id: "3", tutu: "lala3", $row: {}}]
      });
  });

  it('should launch an update-row action on multioperation grid', function () {
    gridService.onGridUpdateRow({
        address: {component: "grid", view: "report"},
        parameters: {row: {id: "1", lala: "tutu"}}
      },
      {
        ...props, components: {
          grid: {
            ...props.components.grid, attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {values: [{id: "1", tutu: "lala", lala: "tutu", $row: {operation: "UPDATE"}}], event: 'after-edit-row'});
  });

  it('should launch an edit-row action on multioperation grid', function () {
    gridService.onGridEditRow({address: {component: "grid", view: "report"}, parameters: {row: 1}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'edit-row', values: [
          {id: "1", tutu: "lala", $row: {editing: true, editingRow: {id: "1", tutu: "lala"}}},
          {id: "2", tutu: "lala2", $row: {editing: false}},
          {id: "3", tutu: "lala3", $row: {editing: false}}
        ]
      });
  });

  it('should launch a save-row action on multioperation grid', function () {
    gridService.onGridSaveRow({address: {component: "grid", view: "report"}, parameters: {rowIndex: 0}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: 'grid', view: 'report'},
      {
        event: 'save-row', values: [
          {id: '1', tutu: 'lala', $row: {editing: true, editingRow: {id: '1', tutu: 'lale'}, operation: 'UPDATE'}},
          {id: '2', tutu: 'lala2', $row: {operation: undefined}},
          {id: '3', tutu: 'lala3', $row: {editing: false, operation: undefined}}
        ]
      });
  });

  it('should launch a cancel-row action on multioperation grid', function () {
    gridService.onGridCancelRow({address: {component: "grid", view: "report"}, parameters: {rowIndex: 0}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'cancel-row', values: [
          {id: "1", tutu: "lale", selected: false},
          {id: "2", tutu: "lala2"},
          {id: "3", tutu: "lala3", $row: {editing: false}}
        ]
      });
  });

  it('should launch a change-page action', function () {
    gridService.onGridChangePage({
        address: {component: "grid", view: "report"},
        parameters: {page: 1, first: 1, rows: 1, max: 100}
      },
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
            specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSpecificAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {page: 1, first: 1, rows: 1, max: 100});
  });

  it('should launch a change-sort action', function () {
    gridService.onGridChangeSort({address: {component: "grid", view: "report"}, parameters: {sort: []}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
            specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSpecificAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {sort: []});
  });

  it('should launch a change-filter action', function () {
    gridService.onGridChangeFilter({address: {component: "grid", view: "report"}, parameters: {filters: {}}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
            specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateSpecificAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {filters: {}});
  });

  it('should launch a check-records-saved action', function () {
    gridService.onGridCheckRecordsSaved({address: {component: "grid", view: "report"}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
            specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should launch a check-records-saved action failed', function () {
    gridService.onGridCheckRecordsSaved({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
            specificAttributes: {editingRow: {id: "1", tutu: "lale"}},
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala", $row: {editing: true}}]}
          }
        }
      });

    // Spies
    expect(props.rejectAction).toHaveBeenCalled();
    expect(props.addActionsTop).toHaveBeenCalled();
  });

  it('should launch a check-records-generated action', function () {
    gridService.onGridCheckRecordsGenerated({address: {component: "grid", view: "report"}},
      {
        ...props, components: {
          grid: {
            ...props.components.grid,
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala", $row: {operation: "INSERT"}}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should launch a check-records-generated action failed', function () {
    gridService.onGridCheckRecordsGenerated({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.rejectAction).toHaveBeenCalled();
    expect(props.addActionsTop).toHaveBeenCalled();
  });

  it('should launch a validate-row action', function () {
    gridService.validateRow({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.validateRow).toHaveBeenCalled();
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should launch a verify-row-validation action', function () {
    gridService.verifyRowValidation({address: {component: "grid", view: "report", row: "1"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: "lala"}]}
          }
        }
      });

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should launch a verify-row-validation action failed', function () {
    gridService.verifyRowValidation({address: {component: "grid", view: "report", row: "1"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
            attributes: {multioperation: true},
            model: {values: [{id: "1", tutu: {value: "lala", error: {message: "ERROR", values: []}}}]}
          }
        }
      });

    // Spies
    expect(props.rejectAction).toHaveBeenCalled();
  });

  it('should launch a copy-selected-rows-clipboard action', function () {
    // Spies
    const clipboard = spyOn(navigator.clipboard, "writeText");

    gridService.copySelectedRowsToClipboard({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(clipboard).toHaveBeenCalledWith("Column 1\tColumn 2\tColumn 3\n" +
      "lala2\ttutu2\t121\n" +
      "lala4\t\t21.2\n" +
      "\ttutu4\t1231\n" +
      "lala5\ttutu5\t");
  });

  it('should launch a select-first-row action', function () {
    gridService.onGridSelectFirstRow({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'select-row', values: [
          {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: true},
          {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: false},
          {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
          {id: "4", col1: "lala4", col3: 21.2, selected: false},
          {id: "5", col2: "tutu4", col3: 1231, selected: false},
          {id: "6", col1: "lala5", col2: "tutu5", selected: false}
        ]
      });
  });

  it('should launch a select-last-row action', function () {
    gridService.onGridSelectLastRow({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'select-row', values: [
          {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
          {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: false},
          {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
          {id: "4", col1: "lala4", col3: 21.2, selected: false},
          {id: "5", col2: "tutu4", col3: 1231, selected: false},
          {id: "6", col1: "lala5", col2: "tutu5", selected: true}
        ]
      });
  });

  it('should launch a select-all-rows action', function () {
    gridService.onGridSelectAllRows({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'select-row', values: [
          {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: true},
          {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: true},
          {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: true},
          {id: "4", col1: "lala4", col3: 21.2, selected: true},
          {id: "5", col2: "tutu4", col3: 1231, selected: true},
          {id: "6", col1: "lala5", col2: "tutu5", selected: true}
        ]
      });
  });

  it('should launch an unselect-all-rows action', function () {
    gridService.onGridUnselectAllRows({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        event: 'select-row', values: [
          {id: "1", col1: "lala1", col2: "tutu1", col3: 21.2, selected: false},
          {id: "2", col1: "lala2", col2: "tutu2", col3: 121, selected: false},
          {id: "3", col1: "lala3", col2: "tutu3", col3: 21.2, selected: false},
          {id: "4", col1: "lala4", col3: 21.2, selected: false},
          {id: "5", col2: "tutu4", col3: 1231, selected: false},
          {id: "6", col1: "lala5", col2: "tutu5", selected: false}
        ]
      });
  });

  it('should launch a check-one-selected action', function () {
    gridService.onGridCheckOneSelected({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should launch a check-one-selected action invalid', function () {
    gridService.onGridCheckOneSelected({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.rejectAction).toHaveBeenCalled();
    expect(props.addActionsTop).toHaveBeenCalled();
  });

  it('should launch a check-some-selected action', function () {
    gridService.onGridCheckSomeSelected({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
  });

  it('should launch a check-some-selected action invalid', function () {
    gridService.onGridCheckSomeSelected({address: {component: "grid", view: "report"}},
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.rejectAction).toHaveBeenCalled();
    expect(props.addActionsTop).toHaveBeenCalled();
  });

  it('should launch a change-column-label action', function () {
    gridService.onGridChangeColumnLabel({
        address: {component: "grid", view: "report"},
        parameters: {column: "tutu", label: "Nuevo tutu"}
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        columnModel: [{name: "tutu", label: "Nuevo tutu"},
          {name: "lala", label: "Lala"},
          {name: "lerele", label: "Lereele"}]
      });
  });

  it('should launch a show-columns action', function () {
    gridService.onGridShowColumns({
        address: {component: "grid", view: "report"},
        parameters: {columns: ["tutu", "tutu2"]}
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        columnModel: [
          {name: "tutu", label: "Tutu", hidden: false},
          {name: "tutu2", label: "Tutu2", hidden: false},
          {name: "tutu3", label: "Tutu3", hidden: true}]
      });
  });

  it('should launch a hide-columns action', function () {
    gridService.onGridHideColumns({
        address: {component: "grid", view: "report"},
        parameters: {columns: ["tutu", "tutu3"]}
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        columnModel: [
          {name: "tutu", label: "Tutu", hidden: true},
          {name: "tutu2", label: "Tutu2", hidden: false},
          {name: "tutu3", label: "Tutu3", hidden: true}]
      });
  });

  it('should launch a toggle-columns-visibility action', function () {
    gridService.onGridColumnVisibilityToggle({
        address: {component: "grid", view: "report"},
        parameters: {columns: ["tutu", "tutu3"], show: false}
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        columnModel: [
          {name: "tutu", label: "Tutu", hidden: true},
          {name: "tutu2", label: "Tutu2", hidden: false},
          {name: "tutu3", label: "Tutu3", hidden: true}]
      });
  });

  it('should launch an add-column action', function () {
    gridService.onGridAddColumns({
        address: {component: "grid", view: "report"},
        parameters: {
          columns: [{name: "tutu4", label: "Tutu 4", hidden: false}, {
            name: "tutu5",
            label: "Tutu 5",
            hidden: false
          }], show: false
        }
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        columnModel: [
          {name: "tutu", label: "Tutu", hidden: false},
          {name: "tutu2", label: "Tutu2", hidden: false},
          {name: "tutu3", label: "Tutu3", hidden: false},
          {name: "tutu4", label: "Tutu 4", hidden: false},
          {name: "tutu5", label: "Tutu 5", hidden: false}]
      });
  });

  it('should launch a replace-column action', function () {
    gridService.onGridReplaceColumns({
        address: {component: "grid", view: "report"},
        parameters: {
          columns: [{name: "tutu4", label: "Tutu 4", hidden: false}, {
            name: "tutu5",
            label: "Tutu 5",
            hidden: false
          }], show: false
        }
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateAttributes).toHaveBeenCalledWith({component: "grid", view: "report"},
      {
        columnModel: [
          {name: "tutu4", label: "Tutu 4", hidden: false},
          {name: "tutu5", label: "Tutu 5", hidden: false}]
      });
  });

  it('should launch an update-cell action', function () {
    gridService.onGridUpdateCell({
        address: {component: "grid", view: "report", column: 'tutu', row: '1'},
        parameters: {data: "LALA"}
      },
      {
        ...props, t: (text) => text, components: {
          grid: {
            ...props.components.grid,
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

    // Spies
    expect(props.acceptAction).toHaveBeenCalled();
    expect(props.updateModelWithDependencies).toHaveBeenCalledWith({
        component: "grid",
        view: "report",
        column: "tutu",
        row: "1"
      },
      {values: "LALA"});
  });
});
