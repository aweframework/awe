import * as gridThunks from '../../../../src/redux/thunks/grid';
import { RowPositionType } from '../../../../src/utilities/grid';

describe('awe-react-client/test/js/redux/thunks/gridThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockAddress;
  let mockComponent;
  let mockState;

  beforeEach(() => {
    dispatch = jasmine.createSpy('dispatch');

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
    getState = jasmine.createSpy('getState').and.returnValue(mockState);
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
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(updateModel.data.event).toBe('select-row');
      expect(updateModel.data.selected).toEqual([{ id: 2 }]);
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
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(updateModel.data.event).toBe('select-row');

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
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

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
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      expect(updateModel.data.values.every(r => r.selected)).toBeTrue();
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
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      expect(updateModel.data.values.every(r => !r.selected)).toBeTrue();
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
      const [[acceptAction], [updateAttributes]] = dispatch.calls.allArgs();

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');

      const updatedCols = updateAttributes.data.columnModel;
      expect(updatedCols.find(c => c.name === 'id').hidden).toBeTrue();
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
      const [ [updateModelWithDependencies1], [acceptAction], [updateModelWithDependencies2]] = dispatch.calls.allArgs();
      const innerDispatch1 = jasmine.createSpy("innerDispatch1");
      updateModelWithDependencies1(innerDispatch1, getState);
      const [[updateModel1]] = innerDispatch1.calls.allArgs();
      const innerDispatch2 = jasmine.createSpy("innerDispatch2");
      updateModelWithDependencies2(innerDispatch2, getState);
      const [[updateModel2]] = innerDispatch2.calls.allArgs();

      expect(updateModel1.type).toBe('UPDATE_MODEL'); // add-row
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateModel2.type).toBe('UPDATE_MODEL');
      expect(updateModel2.data.event).toBe('after-add-row');
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
      const [[acceptAction], [updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

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
      const [[acceptAction]] = dispatch.calls.allArgs();

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
      getState.and.returnValue(stateWithoutSelection);
      const action = { parameters: {}, address: mockAddress };

      gridThunks.checkOneSelectedGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[rejectAction]] = dispatch.calls.allArgs();

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
      getState.and.returnValue(stateWithMultiple);
      const action = { parameters: {}, address: mockAddress };

      gridThunks.checkOneSelectedGridAction(action)(dispatch, getState);

      // Recuperación de acciones
      const [[rejectAction]] = dispatch.calls.allArgs();

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
      expect(dispatch.calls.argsFor(0)[0].type).toBe('ACCEPT_ACTION');
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
      getState.and.returnValue(state);

      gridThunks.checkSomeSelectedGridAction({address: mockAddress})(dispatch, getState);

      // Recuperación de acciones
      const [[rejectAction]] = dispatch.calls.allArgs();

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

      const [[acceptAction], [updateSpecificAttributes]] = dispatch.calls.allArgs();

      expect(acceptAction.type).toBe('ACCEPT_ACTION');
      expect(updateSpecificAttributes.type).toBe('UPDATE_SPECIFIC_ATTRIBUTES');
      expect(updateSpecificAttributes.data.page).toBe(2);
    });
  });

});
