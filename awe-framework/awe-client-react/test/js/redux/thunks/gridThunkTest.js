import * as gridThunks from '../../../../src/redux/thunks/grid';
import {RowPositionType} from '../../../../src/utilities/grid';

describe('awe-react-client/test/js/redux/thunks/gridThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockAddress;
  let mockComponent;
  let mockState;
  // Plain actions dispatched, without the thunks that raise events of the dependencies
  const plainActions = () => dispatch.mock.calls.map(([action]) => action).filter(action => typeof action !== 'function');

  beforeEach(() => {
    dispatch = jest.fn();

    mockAddress = { view: 'base', component: 'grid1' };
    mockComponent = {
      address: mockAddress,
      attributes: {
        columnModel: [
          { name: 'id', label: 'ID', hidden: false },
          { name: 'name', label: 'Nombre', hidden: false }
        ],
        editable: true,
        multioperation: false,
        treegrid: false,
      },
      model: {
        values: [
          { id: 1, name: 'Row 1', selected: false },
          { id: 2, name: 'Row 2', selected: true },
          { id: 3, name: 'Row 3', selected: false }
        ]
      }
    };

    mockState = { components: { grid1: mockComponent } };
    getState = jest.fn().mockReturnValue(mockState);
  });

  // -------------------------------
  // ✅ selectRowGridAction
  // -------------------------------
  describe('selectRowGridAction', () => {
    it('debería aceptar la acción y actualizar el modelo con los valores seleccionados', () => {
      const action = {
        parameters: { values: [{ id: 2 }] }
      };

      gridThunks.selectRowGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel], [setRuntimeEvent]] = innerDispatch.mock.calls;

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(setRuntimeEvent.type).toBe('SET_RUNTIME_EVENT');
      expect(updateModel.data.selected).toEqual([{ id: 2 }]);
    });
  });

  describe('selectRowGridAction while a row is being edited', () => {
    const selectRows = (values) => {
      dispatch.mockClear();
      gridThunks.selectRowGridAction({ parameters: { values }, address: mockAddress })(dispatch, getState);
      const [, [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      return innerDispatch.mock.calls[0][0].data.selected;
    };

    beforeEach(() => {
      mockComponent.model.values = [
        { id: 1, name: 'Row 1', selected: false },
        { id: 2, name: 'Row 2', selected: true, $row: { editing: true } },
        { id: 3, name: 'Row 3', selected: false }
      ];
    });

    it('keeps the row being edited selected when another row is selected', () => {
      expect(selectRows([3])).toEqual([2]);
    });

    it('keeps the row being edited selected when the selection is cleared', () => {
      expect(selectRows([])).toEqual([2]);
    });

    it('selects the rows asked for when no row is being edited', () => {
      mockComponent.model.values[1].$row = { editing: false };

      expect(selectRows([3])).toEqual([3]);
    });

    it('selects the rows asked for in a grid with multiple selection', () => {
      mockComponent.attributes = { ...mockComponent.attributes, multiselect: true };

      expect(selectRows([3])).toEqual([3]);
    });

    it('reads the row being edited before accepting the action, as accepting it can change the state', () => {
      // Accepting the action runs the queue, which can finish the edition of the row
      dispatch.mockImplementation((item) => {
        if (item?.type === 'ACCEPT_ACTION') {
          mockComponent.model.values[1].$row = { editing: false };
        }
      });

      expect(selectRows([3])).toEqual([2]);
    });
  });

  // -------------------------------
  // ✅ selectFirstRowGridAction
  // -------------------------------
  describe('selectFirstRowGridAction', () => {
    it('debería seleccionar la primera fila', () => {
      const action = { parameters: {}, address: mockAddress };

      gridThunks.selectFirstRowGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel], [setEvent]] = innerDispatch.mock.calls;

      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(setEvent.event).toBe('select-row');

      const selected = updateModel.data.values.filter(r => r.selected);
      expect(selected.length).toBe(1);
      expect(selected[0].id).toBe(1);
    });
  });

  // -------------------------------
  // ✅ selectLastRowGridAction
  // -------------------------------
  describe('selectLastRowGridAction', () => {
    it('debería seleccionar la última fila', () => {
      const action = { parameters: {}, address: mockAddress };
      gridThunks.selectLastRowGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateModel.data.values.find(r => r.selected).id).toBe(3);
    });
  });

  // -------------------------------
  // ✅ selectAllRowsGridAction
  // -------------------------------
  describe('selectAllRowsGridAction', () => {
    it('debería marcar todas las filas como seleccionadas', () => {
      const action = { parameters: {}, address: mockAddress };

      gridThunks.selectAllRowsGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

      expect(updateModel.data.values.every(r => r.selected)).toBe(true);
    });
  });

  // -------------------------------
  // ✅ unselectAllRowsGridAction
  // -------------------------------
  describe('unselectAllRowsGridAction', () => {
    it('debería desmarcar todas las filas', () => {
      const action = { parameters: {}, address: mockAddress };

      gridThunks.unselectAllRowsGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

      expect(updateModel.data.values.every(r => !r.selected)).toBe(true);
    });
  });

  // -------------------------------
  // ✅ toggleColumnVisibilityGridAction
  // -------------------------------
  describe('toggleColumnVisibilityGridAction', () => {
    it('debería alternar la visibilidad de columnas indicadas', () => {
      const action = {
        address: mockAddress,
        parameters: { columns: ['id'], show: false }
      };

      gridThunks.toggleColumnVisibilityGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateAttributes]] = dispatch.mock.calls;

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');

      const updatedCols = updateAttributes.data.columnModel;
      expect(updatedCols.find(c => c.name === 'id').hidden).toBe(true);
    });
  });

  // -------------------------------
  // ✅ addRowGridAction
  // -------------------------------
  describe('addRowGridAction', () => {
    it('debería añadir una fila nueva', () => {
      const action = {
        parameters: { rowId: 2, row: { name: 'Nueva' } },
        address: mockAddress
      };

      gridThunks.addRowGridAction(action, RowPositionType.AFTER)(dispatch, getState);

      // Recuperación de acciones
      const [ [updateModelWithDependencies1], [acceptAction], [updateModelWithDependencies2]] = dispatch.mock.calls;
      const innerDispatch1 = jest.fn();
      updateModelWithDependencies1(innerDispatch1, getState);
      const [[addEvent1]] = innerDispatch1.mock.calls;
      const innerDispatch2 = jest.fn();
      updateModelWithDependencies2(innerDispatch2, getState);
      const [[updateModel2], [addEvent]] = innerDispatch2.mock.calls;

      expect(addEvent1.type).toBe('SET_RUNTIME_EVENT'); // add-row
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateModel2.type).toBe('UPDATE_MODEL');
      expect(addEvent.type).toBe('SET_RUNTIME_EVENT');
    });
  });

  describe('addRowGridAction identifiers and tree branches', () => {
    beforeEach(() => {
      // The identifier of the rows is not one of the columns of the grid
      mockComponent.attributes = { ...mockComponent.attributes, columnModel: [{ name: 'name', label: 'Nombre' }] };
    });

    const addRow = (action, position) => {
      dispatch.mockClear();
      gridThunks.addRowGridAction(action, position)(dispatch, getState);
      const [, , [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      return innerDispatch.mock.calls[0][0].data.values;
    };

    it('numbers the new rows of a grid from zero, like the AngularJS client', () => {
      const values = addRow({ parameters: { rowId: 2 }, address: mockAddress }, RowPositionType.AFTER);

      expect(values.find(row => row.selected).id).toBe('new-row-0');
    });

    it('does not count the rows added to other grids or in previous calls', () => {
      addRow({ parameters: { rowId: 2 }, address: mockAddress }, RowPositionType.AFTER);
      const values = addRow({ parameters: { rowId: 2 }, address: mockAddress }, RowPositionType.AFTER);

      expect(values.find(row => row.selected).id).toBe('new-row-0');
    });

    it('numbers a new row after the new rows the grid already has', () => {
      mockComponent.model.values.push({ id: 'new-row-0', name: 'New', selected: false });
      mockComponent.model.values.push({ id: 'new-row-3', name: 'New', selected: false });

      const values = addRow({ parameters: { rowId: 2 }, address: mockAddress }, RowPositionType.AFTER);

      expect(values.find(row => row.selected).id).toBe('new-row-4');
    });

    it('expands the branch of a tree grid when a child is added to it', () => {
      mockComponent.attributes = { ...mockComponent.attributes, treegrid: true, treeParent: 'parent' };
      mockComponent.model.values = [
        { id: 'Root', parent: '', name: 'Root', selected: true, $row: { expanded: false } },
        { id: 'Other', parent: '', name: 'Other', $row: { expanded: false } }
      ];

      const values = addRow({ parameters: { rowId: 'Root' }, address: mockAddress }, RowPositionType.CHILD);

      expect(values.find(row => row.id === 'Root').$row).toEqual(expect.objectContaining({ expanded: true, loaded: true }));
      expect(values.find(row => row.id === 'Other').$row.expanded).toBe(false);
      expect(values.find(row => row.selected)).toEqual(expect.objectContaining({ id: 'new-row-0', parent: 'Root' }));
    });
  });

  describe('verifyRowValidationGridAction', () => {
    const verify = (row) => {
      dispatch.mockClear();
      const action = { type: 'verify-row-validation', address: { ...mockAddress, row }, parameters: {} };
      gridThunks.verifyRowValidationGridAction(action)(dispatch, getState);
      return dispatch.mock.calls.map(([call]) => call.type);
    };

    it('accepts the action when the validated row has no errors', () => {
      mockComponent.model.values = [{ id: 1, name: 'Row 1', $attrs: { name: { error: null } } }];

      expect(verify(1)).toEqual(['ACCEPT_ACTION']);
    });

    it('rejects the action when the validated row has errors', () => {
      mockComponent.model.values = [{ id: 1, name: 'Row 1', $attrs: { name: { error: { message: 'Required' } } } }];

      expect(verify(1)).toEqual(['REJECT_ACTION']);
    });

    it('finds the row of a tree grid by the tree identifier, not by an id column', () => {
      // New rows of a tree grid only carry the tree identifier
      mockComponent.attributes = { ...mockComponent.attributes, treegrid: true, treeId: 'treeId', treeParent: 'parent' };
      mockComponent.model.values = [
        { treeId: 'Root', parent: '', $attrs: {} },
        { treeId: 'new-row-0', parent: 'Root', $attrs: { name: { error: { message: 'Required' } } } }
      ];

      expect(verify('new-row-0')).toEqual(['REJECT_ACTION']);
    });

    it('accepts the action when the validated row does not exist any more', () => {
      mockComponent.model.values = [{ id: 1, name: 'Row 1' }];

      expect(verify('gone')).toEqual(['ACCEPT_ACTION']);
    });
  });

  // -------------------------------
  // ✅ deleteRowGridAction
  // -------------------------------
  describe('deleteRowGridAction', () => {
    it('debería eliminar una fila seleccionada', () => {
      const action = { parameters: { rowId: 2 }, address: mockAddress };

      gridThunks.deleteRowGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateModel.type).toBe('UPDATE_MODEL');

      const newValues = updateModel.data.values;
      expect(newValues.length).toBe(2);
      expect(newValues.find(r => r.id === 2)).toBeUndefined();
    });
  });

  // -------------------------------
  // ✅ checkOneSelectedGridAction
  // -------------------------------
  describe('checkOneSelectedGridAction', () => {
    it('debería aceptar la acción si hay una fila seleccionada', () => {
      const action = { parameters: {}, address: mockAddress };

      gridThunks.checkOneSelectedGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [acceptAction] = plainActions();

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('debería lanzar rejectAction si no hay ninguna fila seleccionada', () => {
      const stateWithoutSelection = {
        components: {
          grid1: {
            ...mockComponent,
            model: {
              values: mockComponent.model.values.map(v => ({ ...v, selected: false }))
            }
          }
        }
      };
      getState.mockReturnValue(stateWithoutSelection);
      const action = { parameters: {}, address: mockAddress };

      gridThunks.checkOneSelectedGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [rejectAction] = plainActions();

      expect(rejectAction.type).toBe('REJECT_ACTION');
    });

    it('debería lanzar rejectAction si hay más de una fila seleccionada', () => {
      const stateWithMultiple = {
        components: {
          grid1: {
            ...mockComponent,
            model: {
              values: mockComponent.model.values.map(v => ({ ...v, selected: true }))
            }
          }
        }
      };
      getState.mockReturnValue(stateWithMultiple);
      const action = { parameters: {}, address: mockAddress };

      gridThunks.checkOneSelectedGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [rejectAction] = plainActions();

      expect(rejectAction.type).toBe('REJECT_ACTION');
    });
  });

  // -------------------------------
  // ✅ checkSomeSelectedGridAction
  // -------------------------------
  describe('checkSomeSelectedGridAction', () => {
    it('debería aceptar si hay al menos una fila seleccionada', () => {
      const action = { parameters: {}, address: mockAddress };
      gridThunks.checkSomeSelectedGridAction(action)(dispatch, getState);
      expect(plainActions()[0].type).toBe('ACCEPT_ACTION');
    });

    it('debería rechazar si no hay ninguna seleccionada', () => {
      const state = {
        components: {
          grid1: {
            ...mockComponent,
            model: { values: mockComponent.model.values.map(v => ({ ...v, selected: false })) }
          }
        }
      };
      getState.mockReturnValue(state);

      gridThunks.checkSomeSelectedGridAction({address: mockAddress})(dispatch, getState);

      // Recuperación de acciones
      const [rejectAction] = plainActions();

      expect(rejectAction.type).toBe('REJECT_ACTION');
    });
  });

  // -------------------------------
  // ✅ changePageGridAction
  // -------------------------------
  describe('changePageGridAction', () => {
    it('debería cambiar de página correctamente', () => {
      const action = { parameters: { page: 2 }, address: mockAddress };
      gridThunks.changePageGridAction(action)(dispatch, getState);

      const [[acceptAction], [updateSpecificAttributes]] = dispatch.mock.calls;

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateSpecificAttributes.type).toBe('UPDATE_SPECIFIC_ATTRIBUTES');
      expect(updateSpecificAttributes.data.page).toBe(2);
    });
  });

  // -------------------------------
  // copySelectedRowsToClipboardGridAction
  // -------------------------------
  describe('copySelectedRowsToClipboardGridAction', () => {
    const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));
    let writeText;

    beforeEach(() => {
      writeText = jest.fn();
      Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    });

    afterEach(() => {
      jest.restoreAllMocks();
      delete navigator.clipboard;
    });

    it('copies the headers and the selected rows to the clipboard and accepts the action', async () => {
      writeText.mockResolvedValue();
      const action = { address: mockAddress };

      gridThunks.copySelectedRowsToClipboardGridAction(action, o => o)(dispatch, getState);
      await flushPromises();

      expect(writeText).toHaveBeenCalledWith('ID\tNombre\n2\tRow 2');
      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ type: 'ACCEPT_ACTION' }));
    });

    it('logs the error when the clipboard cannot be written', async () => {
      const error = new Error('clipboard denied');
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
      writeText.mockRejectedValue(error);
      const action = { address: mockAddress };

      gridThunks.copySelectedRowsToClipboardGridAction(action, o => o)(dispatch, getState);
      await flushPromises();

      expect(consoleError).toHaveBeenCalledWith('Error copying the selected rows to the clipboard:', error);
    });
  });

  describe('moveRowGridAction', () => {
    const moveRow = (action, position) => {
      dispatch.mockClear();
      gridThunks.moveRowGridAction(action, position)(dispatch, getState);
      return dispatch.mock.calls;
    };

    const movedValues = (calls) => {
      const innerDispatch = jest.fn();
      calls[1][0](innerDispatch, getState);
      return innerDispatch.mock.calls[0][0].data.values;
    };

    it('moves the selected row and keeps it selected', () => {
      const calls = moveRow({ parameters: {}, address: mockAddress }, RowPositionType.FIRST);

      expect(calls[0][0].type).toBe('ACCEPT_ACTION');
      const values = movedValues(calls);
      expect(values.map(row => row.id)).toEqual([2, 1, 3]);
      expect(values[0].selected).toBe(true);
    });

    it('moves the row of the selectedRow parameter instead of the selected one', () => {
      const values = movedValues(moveRow({ parameters: { selectedRow: 1 }, address: mockAddress }, RowPositionType.LAST));

      expect(values.map(row => row.id)).toEqual([2, 3, 1]);
    });

    it('moves a row one position up and down', () => {
      expect(movedValues(moveRow({ parameters: {}, address: mockAddress }, RowPositionType.BEFORE)).map(row => row.id))
        .toEqual([2, 1, 3]);
      expect(movedValues(moveRow({ parameters: {}, address: mockAddress }, RowPositionType.AFTER)).map(row => row.id))
        .toEqual([1, 3, 2]);
    });

    it('accepts the action without changing the model when the row cannot move', () => {
      const calls = moveRow({ parameters: { rowId: 1 }, address: mockAddress }, RowPositionType.FIRST);

      expect(calls).toHaveLength(1);
      expect(calls[0][0].type).toBe('ACCEPT_ACTION');
    });

    it('accepts the action without moving rows in a tree grid, as the AngularJS engine does', () => {
      mockComponent.attributes = { ...mockComponent.attributes, treegrid: true, treeParent: 'parent' };
      const calls = moveRow({ parameters: {}, address: mockAddress }, RowPositionType.FIRST);

      expect(calls).toHaveLength(1);
      expect(calls[0][0].type).toBe('ACCEPT_ACTION');
    });

    it('accepts the action without changing the model when no row is selected', () => {
      mockComponent.model.values = mockComponent.model.values.map(row => ({ ...row, selected: false }));
      const calls = moveRow({ parameters: {}, address: mockAddress }, RowPositionType.FIRST);

      expect(calls).toHaveLength(1);
    });
  });

  describe('toggleBranchGridAction', () => {
    const toggledRow = (row, parameters, {loadAll = true, others = [{ id: 2 }]} = {}) => {
      dispatch.mockClear();
      // A new state on every call: the component selectors are memoized on the state
      const component = {
        ...mockComponent,
        attributes: { ...mockComponent.attributes, treegrid: true, loadAll, treeId: 'id', treeParent: 'parent' },
        model: { ...mockComponent.model, values: [{ id: 1, $row: row }, ...others] }
      };
      getState.mockReturnValue({ components: { grid1: component } });
      gridThunks.toggleBranchGridAction({ address: mockAddress, parameters: { row: 1, ...parameters } })(dispatch, getState);
      const updateModelWithDependencies = dispatch.mock.calls.map(([action]) => action).find(action => typeof action === 'function');
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      return innerDispatch.mock.calls[0][0].data.values[0].$row;
    };
    const toggled = (row, parameters) => toggledRow(row, parameters).expanded;

    it('opens a node the view shows closed, whatever the row stored', () => {
      expect(toggled({ expanded: true }, { expanded: false })).toBe(true);
    });

    it('does not mark as loaded a node collapsed locally whose branch was never loaded, so reopening it asks the server', () => {
      expect(toggledRow(undefined, { expanded: true }, { loadAll: false }).loaded).toBeFalsy();
    });

    it('marks as loaded a node whose branch the server answered empty, so it does not ask again', () => {
      expect(toggledRow(undefined, { datalist: { rows: [] } }, { loadAll: false }).loaded).toBe(true);
    });

    it('marks as loaded a collapsed node whose children are already in the grid', () => {
      expect(toggledRow(undefined, { expanded: true }, { loadAll: false, others: [{ id: 2, parent: 1 }] }).loaded).toBe(true);
    });

    it('toggles the state stored in the row when the view does not tell what it shows', () => {
      expect(toggled({ expanded: true })).toBe(false);
      expect(toggled(undefined)).toBe(true);
    });

    it('toggles the state the view shows, such as a node expanded by initial-level that stores nothing', () => {
      expect(toggled(undefined, { expanded: true })).toBe(false);
    });
  });

});
