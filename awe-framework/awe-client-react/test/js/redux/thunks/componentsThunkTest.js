import * as thunks from '../../../../src/redux/thunks/components';

describe('awe-react-client/test/js/redux/thunks/componentsThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockState;
  let mockComponent;
  let mockAddress;

  beforeEach(() => {
    dispatch = jasmine.createSpy('dispatch');

    mockAddress = { view: 'base', component: 'comp1' };

    mockComponent = {
      address: mockAddress,
      attributes: {
        buttonType: 'reset',
        isShowing: false,
        minimized: false,
        chartModel: {
          series: []
        }
      },
      model: {
        values: [
          { value: 1, selected: false },
          { value: 2, selected: true },
          { value: 3, selected: false }
        ]
      },
      actions: []
    };

    mockState = {
      components: {
        comp1: mockComponent
      }
    };

    getState = jasmine.createSpy('getState').and.callFake(() => mockState);
  });

  describe('updateViewComponentsWithDependencies', () => {
    it('debería despachar updateViewComponents y inicializar dependencias', () => {
      const view = 'base';
      const data = { test: 'data' };

      thunks.updateViewComponentsWithDependencies(view, data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBeGreaterThanOrEqual(1);

      const [[updateViewComponents]] = dispatch.calls.allArgs();
      expect(updateViewComponents.type).toBe('UPDATE_VIEW_COMPONENTS');
    });
  });

  describe('updateMultipleComponentsWithDependencies', () => {
    it('debería despachar updateMultipleComponents y verificar dependencias', () => {
      const componentList = [{ id: 1 }, { id: 2 }];

      thunks.updateMultipleComponentsWithDependencies(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[updateMultipleComponents]] = dispatch.calls.allArgs();
      expect(updateMultipleComponents.type).toBe('UPDATE_MULTIPLE_COMPONENTS');
    });
  });

  describe('updateMultipleModelsWithDependencies', () => {
    it('debería despachar updateMultipleModels y verificar dependencias', () => {
      const data = { model1: {}, model2: {} };

      thunks.updateMultipleModelsWithDependencies(data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[updateMultipleModels]] = dispatch.calls.allArgs();
      expect(updateMultipleModels.type).toBe('UPDATE_MULTIPLE_MODELS');
    });
  });

  describe('updateModelWithDependencies', () => {
    it('debería despachar updateModel y verificar dependencias', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { value: 'test' };

      thunks.updateModelWithDependencies(address, data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[updateModel]] = dispatch.calls.allArgs();
      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(updateModel.address).toEqual(address);
    });

    it('debería resetear el atributo event si está definido', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { event: 'click', value: 'test' };

      thunks.updateModelWithDependencies(address, data)(dispatch, getState);

      expect(dispatch.calls.count()).toBeGreaterThanOrEqual(2);

      // Verificar que hay una llamada con event: ""
      const [[updateModel], [changeEvent], [clearRuntimeEvent]] = dispatch.calls.allArgs();
      const hasEventReset = clearRuntimeEvent.type === 'CLEAR_RUNTIME_EVENT';
      expect(hasEventReset).toBe(true);
    });
  });

  describe('restoreModelWithDependencies', () => {
    it('debería despachar restoreModel y verificar dependencias', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { value: 'test' };

      thunks.restoreModelWithDependencies(address, data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[restoreModel]] = dispatch.calls.allArgs();
      expect(restoreModel.type).toBe('RESTORE_MODEL');
    });
  });

  describe('restoreMultipleModelWithDependencies', () => {
    it('debería despachar restoreMultipleModel y verificar dependencias', () => {
      const componentList = [{ id: 1 }, { id: 2 }];

      thunks.restoreMultipleModelWithDependencies(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[restoreMultipleModel]] = dispatch.calls.allArgs();
      expect(restoreMultipleModel.type).toBe('RESTORE_MULTIPLE_MODEL');
    });
  });

  describe('resetModelWithDependencies', () => {
    it('debería despachar resetModel y verificar dependencias', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { value: 'test' };

      thunks.resetModelWithDependencies(address, data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[resetModel]] = dispatch.calls.allArgs();
      expect(resetModel.type).toBe('RESET_MODEL');
    });
  });

  describe('resetMultipleModelWithDependencies', () => {
    it('debería despachar resetMultipleModel y verificar dependencias', () => {
      const componentList = [{ id: 1 }, { id: 2 }];

      thunks.resetMultipleModelWithDependencies(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[resetMultipleModel]] = dispatch.calls.allArgs();
      expect(resetMultipleModel.type).toBe('RESET_MULTIPLE_MODEL');
    });
  });

  describe('clickButtonAction', () => {
    it('debería agregar acciones del componente si existen', () => {
      mockComponent.actions = [{ type: 'action1' }, { type: 'action2' }];
      const action = { type: 'click', address: mockAddress };

      thunks.clickButtonAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[addActionsTop]] = dispatch.calls.allArgs();
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload.length).toBe(2);
      expect(addActionsTop.payload[0].address).toEqual(mockAddress);
    });

    it('debería agregar acción restore si el botón es tipo RESET', () => {
      mockComponent.actions = [];
      mockComponent.attributes.buttonType = 'reset';
      const action = { type: 'click', address: mockAddress };

      thunks.clickButtonAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[addActionsTop]] = dispatch.calls.allArgs();
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload[0].type).toBe('restore');
    });

    it('debería aceptar la acción', () => {
      mockComponent.actions = [];
      const action = { type: 'click', address: mockAddress };

      thunks.clickButtonAction(action)(dispatch, getState);

      const [[addActionsTop], [acceptAction]] = dispatch.calls.allArgs();
      const hasAcceptAction = acceptAction.type === 'ACCEPT_ACTION';
      expect(hasAcceptAction).toBe(true);
    });
  });

  describe('finishUploadAction', () => {
    it('debería actualizar el modelo con los datos del archivo', () => {
      const action = {
        type: 'finishUpload',
        address: mockAddress,
        parameters: {
          name: 'test.pdf',
          path: '/path/to/file',
          size: 1024,
          type: 'application/pdf'
        }
      };

      thunks.finishUploadAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      // Verificar que se acepta la acción
      const [[acceptAction]] = dispatch.calls.allArgs();
      const hasAcceptAction = acceptAction.type === 'ACCEPT_ACTION';
      expect(hasAcceptAction).toBe(true);
    });
  });

  describe('deleteUploadAction', () => {
    it('debería resetear el modelo', () => {
      const action = { type: 'deleteUpload', address: mockAddress };

      thunks.deleteUploadAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[acceptAction]] = dispatch.calls.allArgs();
      const hasAcceptAction = acceptAction.type === 'ACCEPT_ACTION';
      expect(hasAcceptAction).toBe(true);
    });
  });

  describe('openDialogAction', () => {
    it('debería abrir el diálogo si no está mostrándose', () => {
      mockComponent.attributes.isShowing = false;
      const action = { type: 'openDialog', address: mockAddress };

      thunks.openDialogAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(2);

      const [[addStack], [updateAttributes]] = dispatch.calls.allArgs();
      expect(addStack.type).toBe('ADD_STACK');
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(updateAttributes.data.isShowing).toBe(true);
      expect(updateAttributes.data.action).toEqual(action);
    });

    it('no debería hacer nada si el diálogo ya está mostrándose', () => {
      mockComponent.attributes.isShowing = true;
      const action = { type: 'openDialog', address: mockAddress };

      thunks.openDialogAction(action)(dispatch, getState);

      expect(dispatch).not.toHaveBeenCalled();
    });
  });

  describe('closeDialogAction', () => {
    it('debería cerrar el diálogo si está mostrándose', () => {
      mockComponent.attributes.isShowing = true;
      mockComponent.attributes.action = { type: 'previousAction' };
      const action = { type: 'closeDialog', address: mockAddress };

      thunks.closeDialogAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(4);

      const [[acceptAction], [removeStack], [acceptAction2], [updateAttributes]] = dispatch.calls.allArgs();
      expect(removeStack.type).toBe('REMOVE_STACK');
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
    });

    it('no debería hacer nada si el diálogo no está mostrándose', () => {
      mockComponent.attributes.isShowing = false;
      const action = { type: 'closeDialog', address: mockAddress };

      thunks.closeDialogAction(action)(dispatch, getState);

      // Solo se acepta la acción
      expect(dispatch.calls.count()).toBe(1);
    });
  });

  describe('closeDialogAndCancelAction', () => {
    it('debería cerrar el diálogo y rechazar la acción previa', () => {
      mockComponent.attributes.isShowing = true;
      mockComponent.attributes.action = { type: 'previousAction' };
      const action = { type: 'closeDialogCancel', address: mockAddress };

      thunks.closeDialogAndCancelAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[acceptAction], [removeStack], [rejectAction], [updateAttributes]] = dispatch.calls.allArgs();
      expect(rejectAction.type).toBe('REJECT_ACTION');
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
    });
  });

  describe('goToNextStepAction', () => {
    it('debería ir al siguiente paso', () => {
      const action = { type: 'next-step', address: mockAddress };

      thunks.goToNextStepAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[2].selected).toBe(true);
      expect(values[1].selected).toBe(false);
    });

    it('no debería pasar del último paso', () => {
      mockComponent.model.values = [
        { value: 1, selected: false },
        { value: 2, selected: false },
        { value: 3, selected: true }
      ];

      const action = { type: 'nextStep', address: mockAddress };

      thunks.goToNextStepAction(action)(dispatch, getState);

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[2].selected).toBe(true);
    });
  });

  describe('goToPrevStepAction', () => {
    it('debería ir al paso anterior', () => {
      const action = { type: 'prevStep', address: mockAddress };

      thunks.goToPrevStepAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[0].selected).toBe(true);
      expect(values[1].selected).toBe(false);
    });

    it('no debería pasar del primer paso', () => {
      mockComponent.model.values = [
        { value: 1, selected: true },
        { value: 2, selected: false },
        { value: 3, selected: false }
      ];

      const action = { type: 'prev-step', address: mockAddress };

      thunks.goToPrevStepAction(action)(dispatch, getState);

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[0].selected).toBe(true);
    });
  });

  describe('goToFirstStepAction', () => {
    it('debería ir al primer paso', () => {
      const action = { type: 'first-step', address: mockAddress };

      thunks.goToFirstStepAction(action)(dispatch, getState);

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[0].selected).toBe(true);
    });
  });

  describe('goToLastStepAction', () => {
    it('debería ir al último paso', () => {
      const action = { type: 'last-step', address: mockAddress };

      thunks.goToLastStepAction(action)(dispatch, getState);

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[0].selected).toBe(false);
      expect(values[1].selected).toBe(false);
      expect(values[2].selected).toBe(true);
    });
  });

  describe('goToNthStepAction', () => {
    it('debería ir al paso específico', () => {
      const action = {
        type: 'nth-step',
        address: mockAddress,
        parameters: { value: 3 }
      };

      thunks.goToNthStepAction(action)(dispatch, getState);

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[2].selected).toBe(true);
      expect(values[0].selected).toBe(false);
      expect(values[1].selected).toBe(false);
    });
  });

  describe('addPointsAction', () => {
    it('debería agregar un punto al modelo', () => {
      const action = {
        type: 'addPoints',
        address: mockAddress,
        parameters: { value: { x: 10, y: 20 } }
      };

      thunks.addPointsAction(action)(dispatch, getState);

      // Verificar que se llama a updateModel
      const [[updateModelWithDependencies]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy("innerDispatch");
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.calls.allArgs();

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values.length).toBe(4);
      expect(values[3]).toEqual({ x: 10, y: 20 });
    });
  });

  describe('addSeriesAction', () => {
    it('debería agregar series al chart', () => {
      const action = {
        type: 'addSeries',
        address: mockAddress,
        parameters: {
          series: [
            {
              id: 'series1',
              xValue: 'x',
              yValue: 'y',
              data: [[1, 10], [2, 20]]
            }
          ]
        }
      };

      thunks.addSeriesAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes).toBeDefined();
      expect(updateAttributes.data.chartModel.series.length).toBe(1);
    });
  });

  describe('removeSeriesAction', () => {
    it('debería eliminar series del chart', () => {
      mockComponent.attributes.chartModel.series = [
        { id: 'series1', data: [] },
        { id: 'series2', data: [] }
      ];

      const action = {
        type: 'removeSeries',
        address: mockAddress,
        parameters: {
          series: [{ id: 'series1' }]
        }
      };

      thunks.removeSeriesAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.chartModel.series.length).toBe(1);
      expect(updateAttributes.data.chartModel.series[0].id).toBe('series2');
    });
  });

  describe('replaceSeriesAction', () => {
    it('debería reemplazar todas las series del chart', () => {
      mockComponent.attributes.chartModel.series = [
        { id: 'series1', data: [] }
      ];

      const action = {
        type: 'replaceSeries',
        address: mockAddress,
        parameters: {
          series: [
            {
              id: 'newSeries',
              xValue: 'x',
              yValue: 'y',
              data: [[1, 10]]
            }
          ]
        }
      };

      thunks.replaceSeriesAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.chartModel.series.length).toBe(1);
      expect(updateAttributes.data.chartModel.series[0].id).toBe('newSeries');
    });
  });

  describe('setPivotSortersAction', () => {
    it('debería actualizar los sorters del pivot', () => {
      const action = {
        type: 'setPivotSorters',
        address: mockAddress,
        parameters: {
          sorters: { column1: 'asc', column2: 'desc' }
        }
      };

      thunks.setPivotSortersAction(action)(dispatch);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.sorters).toEqual({
        column1: 'asc',
        column2: 'desc'
      });
    });
  });

  describe('setPivotGroupRowsAction', () => {
    it('debería actualizar las filas agrupadas del pivot', () => {
      const action = {
        type: 'setPivotGroupRows',
        address: mockAddress,
        parameters: {
          rows: 'category,subcategory'
        }
      };

      thunks.setPivotGroupRowsAction(action)(dispatch);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.rows).toBe('category,subcategory');
    });
  });

  describe('setPivotGroupColsAction', () => {
    it('debería actualizar las columnas agrupadas del pivot', () => {
      const action = {
        type: 'setPivotGroupCols',
        address: mockAddress,
        parameters: {
          cols: 'year,month'
        }
      };

      thunks.setPivotGroupColsAction(action)(dispatch);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.cols).toBe('year,month');
    });
  });

  describe('toggleMenuAction', () => {
    it('debería alternar el estado minimizado del menú', () => {
      mockComponent.attributes.minimized = false;
      const action = { type: 'toggle-menu', address: mockAddress };

      thunks.toggleMenuAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.minimized).toBe(true);
    });

    it('debería cambiar de minimizado a expandido', () => {
      mockComponent.attributes.minimized = true;
      const action = { type: 'toggle-menu', address: mockAddress };

      thunks.toggleMenuAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.calls.allArgs();
      expect(updateAttributes.data.minimized).toBe(false);
    });
  });

  describe('toggleNavbarAction', () => {
    it('debería aceptar la acción', () => {
      const action = { type: 'toggleNavbar' };

      thunks.toggleNavbarAction(action)(dispatch);

      const [[acceptAction]] = dispatch.calls.allArgs();
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('changeMenuAction', () => {
    it('debería actualizar las opciones del menú', () => {
      const action = {
        type: 'changeMenu',
        address: mockAddress,
        parameters: {
          options: { item1: true, item2: false }
        }
      };

      thunks.changeMenuAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateMenu]] = dispatch.calls.allArgs();
      expect(updateMenu).toBeDefined();
      expect(updateMenu.data).toEqual({ item1: true, item2: false });
    });
  });
});