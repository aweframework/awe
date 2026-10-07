import * as thunks from '../../../../src/redux/thunks/components';
import MenuRegistry from '../../../../src/redux/registry/MenuRegistry';

describe('awe-react-client/test/js/redux/thunks/componentsThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockState;
  let mockComponent;
  let mockAddress;

  beforeEach(() => {
    dispatch = jest.fn();

    mockAddress = { view: 'base', component: 'comp1' };

    mockComponent = {
      address: mockAddress,
      attributes: {
        buttonType: 'reset',
        isShowing: false,
        minimized: false,
        echartsModel: {
          xAxis: [{type: 'time', awe: {axis: 'x'}}],
          yAxis: [{type: 'value', awe: {axis: 'y'}}],
          series: [],
          awe: {chartType: 'line'}
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

    getState = jest.fn(() => mockState);
  });

  describe('updateViewComponentsWithDependencies', () => {
    it('debería despachar updateViewComponents y inicializar dependencias', () => {
      const view = 'base';
      const data = { test: 'data' };

      thunks.updateViewComponentsWithDependencies(view, data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.mock.calls.length).toBeGreaterThanOrEqual(1);

      const [[updateViewComponents]] = dispatch.mock.calls;
      expect(updateViewComponents.type).toBe('UPDATE_VIEW_COMPONENTS');
    });
  });

  describe('updateMultipleComponentsWithDependencies', () => {
    it('debería despachar updateMultipleComponents y verificar dependencias', () => {
      const componentList = [{ id: 1 }, { id: 2 }];

      thunks.updateMultipleComponentsWithDependencies(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[updateMultipleComponents]] = dispatch.mock.calls;
      expect(updateMultipleComponents.type).toBe('UPDATE_MULTIPLE_COMPONENTS');
    });
  });

  describe('updateMultipleModelsWithDependencies', () => {
    it('debería despachar updateMultipleModels y verificar dependencias', () => {
      const data = { model1: {}, model2: {} };

      thunks.updateMultipleModelsWithDependencies(data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[updateMultipleModels]] = dispatch.mock.calls;
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

      const [[updateModel]] = dispatch.mock.calls;
      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(updateModel.address).toEqual(address);
    });

    it('debería resetear el atributo event si está definido', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { event: 'click', value: 'test' };

      thunks.updateModelWithDependencies(address, data)(dispatch, getState);

      expect(dispatch.mock.calls.length).toBeGreaterThanOrEqual(2);

      // Verificar que hay una llamada con event: ""
      const [[updateModel], [changeEvent], [clearRuntimeEvent]] = dispatch.mock.calls;
      const hasEventReset = clearRuntimeEvent.type === 'CLEAR_RUNTIME_EVENT';
      expect(hasEventReset).toBe(true);
    });

    it('no debería despachar updateModel si solo se pasa un evento', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { event: 'click' };

      thunks.updateModelWithDependencies(address, data)(dispatch, getState);

      const allCalls = dispatch.mock.calls.map(([action]) => action.type);

      // No debería contener UPDATE_MODEL
      expect(allCalls).not.toContain('UPDATE_MODEL');

      // Debería contener SET_RUNTIME_EVENT y CLEAR_RUNTIME_EVENT
      expect(allCalls).toContain('SET_RUNTIME_EVENT');
      expect(allCalls).toContain('CLEAR_RUNTIME_EVENT');
    });

    it('debería despachar updateModel y setRuntimeEvent si se pasan datos de modelo y evento', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { event: 'click', value: 'test' };

      thunks.updateModelWithDependencies(address, data)(dispatch, getState);

      const allCalls = dispatch.mock.calls.map(([action]) => action.type);

      // Debería contener UPDATE_MODEL, SET_RUNTIME_EVENT y CLEAR_RUNTIME_EVENT
      expect(allCalls).toContain('UPDATE_MODEL');
      expect(allCalls).toContain('SET_RUNTIME_EVENT');
      expect(allCalls).toContain('CLEAR_RUNTIME_EVENT');
    });
  });

  describe('restoreModelWithDependencies', () => {
    it('debería despachar restoreModel y verificar dependencias', () => {
      const address = { view: 'test', component: 'comp1' };
      const data = { value: 'test' };

      thunks.restoreModelWithDependencies(address, data)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[restoreModel]] = dispatch.mock.calls;
      expect(restoreModel.type).toBe('RESTORE_MODEL');
    });
  });

  describe('restoreMultipleModelWithDependencies', () => {
    it('debería despachar restoreMultipleModel y verificar dependencias', () => {
      const componentList = [{ id: 1 }, { id: 2 }];

      thunks.restoreMultipleModelWithDependencies(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[restoreMultipleModel]] = dispatch.mock.calls;
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

      const [[resetModel]] = dispatch.mock.calls;
      expect(resetModel.type).toBe('RESET_MODEL');
    });
  });

  describe('resetMultipleModelWithDependencies', () => {
    it('debería despachar resetMultipleModel y verificar dependencias', () => {
      const componentList = [{ id: 1 }, { id: 2 }];

      thunks.resetMultipleModelWithDependencies(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(getState).toHaveBeenCalled();

      const [[resetMultipleModel]] = dispatch.mock.calls;
      expect(resetMultipleModel.type).toBe('RESET_MULTIPLE_MODEL');
    });
  });

  describe('clickButtonAction', () => {
    it('debería agregar acciones del componente si existen', () => {
      mockComponent.actions = [{ type: 'action1' }, { type: 'action2' }];
      const action = { type: 'click', address: mockAddress };

      thunks.clickButtonAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[addActionsTop]] = dispatch.mock.calls;
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

      const [[addActionsTop]] = dispatch.mock.calls;
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload[0].type).toBe('restore');
    });

    it('debería aceptar la acción', () => {
      mockComponent.actions = [];
      const action = { type: 'click', address: mockAddress };

      thunks.clickButtonAction(action)(dispatch, getState);

      const [[addActionsTop], [acceptAction]] = dispatch.mock.calls;
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
      const [[acceptAction]] = dispatch.mock.calls;
      const hasAcceptAction = acceptAction.type === 'ACCEPT_ACTION';
      expect(hasAcceptAction).toBe(true);
    });
  });

  describe('deleteUploadAction', () => {
    it('debería resetear el modelo', () => {
      const action = { type: 'deleteUpload', address: mockAddress };

      thunks.deleteUploadAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[acceptAction]] = dispatch.mock.calls;
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
      expect(dispatch).toHaveBeenCalledTimes(2);

      const [[addStack], [updateAttributes]] = dispatch.mock.calls;
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
      expect(dispatch).toHaveBeenCalledTimes(4);

      const [[acceptAction], [removeStack], [acceptAction2], [updateAttributes]] = dispatch.mock.calls;
      expect(removeStack.type).toBe('REMOVE_STACK');
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
    });

    it('no debería hacer nada si el diálogo no está mostrándose', () => {
      mockComponent.attributes.isShowing = false;
      const action = { type: 'closeDialog', address: mockAddress };

      thunks.closeDialogAction(action)(dispatch, getState);

      // Solo se acepta la acción
      expect(dispatch).toHaveBeenCalledTimes(1);
    });
  });

  describe('closeDialogAndCancelAction', () => {
    it('debería cerrar el diálogo y rechazar la acción previa', () => {
      mockComponent.attributes.isShowing = true;
      mockComponent.attributes.action = { type: 'previousAction' };
      const action = { type: 'closeDialogCancel', address: mockAddress };

      thunks.closeDialogAndCancelAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[acceptAction], [removeStack], [rejectAction], [updateAttributes]] = dispatch.mock.calls;
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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

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
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      const [[updateModel]] = innerDispatch.mock.calls;

      // Recuperar los valores
      const values = updateModel.data.values;
      expect(values[2].selected).toBe(true);
      expect(values[0].selected).toBe(false);
      expect(values[1].selected).toBe(false);
    });
  });

  describe('addPointsAction', () => {
    const run = (parameters) => {
      thunks.addPointsAction({type: 'add-points', address: mockAddress, parameters})(dispatch, getState);
      const [[updateModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      updateModelWithDependencies(innerDispatch, getState);
      return innerDispatch.mock.calls[0][0].data.values;
    };

    it('should add the rows of the data list that the server sends', () => {
      const values = run({data: {rows: [{date: 1, serie1: 10}, {date: 2, serie1: 20}], records: 2}});

      expect(values.length).toBe(5);
      expect(values[3]).toEqual({date: 1, serie1: 10});
      expect(values[4]).toEqual({date: 2, serie1: 20});
    });

    it('should keep the original values before the new points', () => {
      const values = run({data: {rows: [{date: 1}]}});

      expect(values.slice(0, 3)).toEqual(mockComponent.model.values);
    });

    it('should keep accepting a single point in the value parameter', () => {
      const values = run({value: {x: 10, y: 20}});

      expect(values.length).toBe(4);
      expect(values[3]).toEqual({x: 10, y: 20});
    });

    it('should not change the values when the action has no parameters', () => {
      expect(run({}).length).toBe(3);
    });

    it('should not change the values when the data list has no rows', () => {
      expect(run({data: {}}).length).toBe(3);
    });

    it('should not change the values when the data list has no rows and nothing else', () => {
      expect(run({data: {total: 0}}).length).toBe(3);
    });

    it('should accept the action once the points are added', () => {
      run({data: {rows: [{date: 1}]}});

      expect(dispatch.mock.calls[1][0].type).toBe('ACCEPT_ACTION');
    });
  });

  describe('chart series actions', () => {
    const lineSeries = (id, extra = {}) => ({
      id, name: id, type: 'spline', xValue: 'date', yValue: id, data: [[1, 10], [2, 20]],
      echarts: {id, name: id, type: 'line', smooth: true, awe: {type: 'spline', xValue: 'date', yValue: id}},
      ...extra
    });
    const echartsSeries = (call) => call.data.echartsModel.series;
    const valuesOf = (thunkDispatch) => {
      const innerDispatch = jest.fn();
      thunkDispatch(innerDispatch, getState);
      return innerDispatch.mock.calls[0][0].data.values;
    };

    describe('addSeriesAction', () => {
      it('should add the ECharts series that the server sends and keep the model', () => {
        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [lineSeries('s1')]}})(dispatch, getState);

        const [[updateAttributes]] = dispatch.mock.calls;
        expect(echartsSeries(updateAttributes)).toEqual([lineSeries('s1').echarts]);
        expect(updateAttributes.data.echartsModel.xAxis).toEqual(mockComponent.attributes.echartsModel.xAxis);
        expect(updateAttributes.data.echartsModel.awe).toEqual({chartType: 'line'});
      });

      it('should replace a series with the same identifier and keep the others', () => {
        mockComponent.attributes.echartsModel.series = [{id: 's1', type: 'bar'}, {id: 's2', type: 'bar'}];

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [lineSeries('s1')]}})(dispatch, getState);

        const [[updateAttributes]] = dispatch.mock.calls;
        expect(echartsSeries(updateAttributes).map(serie => serie.id)).toEqual(['s2', 's1']);
        expect(echartsSeries(updateAttributes)[1].type).toBe('line');
      });

      it('should merge the points of the series into the values by position', () => {
        mockComponent.model.values = [{date: 1, other: 'a'}, {date: 2, other: 'b'}];

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [lineSeries('s1')]}})(dispatch, getState);

        expect(valuesOf(dispatch.mock.calls[1][0])).toEqual([
          {date: 1, other: 'a', s1: 10},
          {date: 2, other: 'b', s1: 20}
        ]);
      });

      it('should bind the third coordinate of a series with z value', () => {
        mockComponent.model.values = [];
        const bubble = lineSeries('b1', {zValue: 'size', data: [[1, 10, 5]]});

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [bubble]}})(dispatch, getState);

        expect(valuesOf(dispatch.mock.calls[1][0])).toEqual([{date: 1, b1: 10, size: 5}]);
      });

      it('should swap the axis of the series of an inverted chart', () => {
        mockComponent.attributes.echartsModel.awe = {inverted: true};
        const serie = lineSeries('s1');
        serie.echarts.xAxisIndex = 1;
        serie.echarts.yAxisIndex = 0;

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [serie]}})(dispatch, getState);

        const added = echartsSeries(dispatch.mock.calls[0][0])[0];
        expect(added.xAxisIndex).toBe(0);
        expect(added.yAxisIndex).toBe(1);
      });

      it('should build a minimal series from the Highcharts fields when the server sent no translation', () => {
        const serie = lineSeries('s1');
        delete serie.echarts;
        serie.type = 'column';

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [serie]}})(dispatch, getState);

        expect(echartsSeries(dispatch.mock.calls[0][0])[0]).toEqual({
          id: 's1', name: 's1', type: 'bar', awe: {type: 'column', xValue: 'date', yValue: 's1'}
        });
      });

      it('should update only the values when the chart has no echartsModel', () => {
        const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
        delete mockComponent.attributes.echartsModel;
        mockComponent.model.values = [];

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [lineSeries('s1')]}})(dispatch, getState);

        expect(dispatch.mock.calls[0][0].data?.echartsModel).toBeUndefined();
        expect(valuesOf(dispatch.mock.calls[0][0])).toHaveLength(2);
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('echartsModel'));
        warn.mockRestore();
      });

      it('should not write points without the fields they are bound to', () => {
        mockComponent.model.values = [];

        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [{id: 'x', data: [[1, 2]]}]}})(dispatch, getState);

        expect(valuesOf(dispatch.mock.calls[1][0])).toEqual([]);
      });

      it('should accept the action', () => {
        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {series: [lineSeries('s1')]}})(dispatch, getState);

        expect(dispatch.mock.calls[dispatch.mock.calls.length - 1][0].type).toBe('ACCEPT_ACTION');
      });

      it('should work without series', () => {
        thunks.addSeriesAction({type: 'add-chart-series', address: mockAddress, parameters: {}})(dispatch, getState);

        expect(echartsSeries(dispatch.mock.calls[0][0])).toEqual([]);
      });
    });

    describe('removeSeriesAction', () => {
      it('should remove series of the ECharts model', () => {
        mockComponent.attributes.echartsModel.series = [{id: 'series1'}, {id: 'series2'}];

        thunks.removeSeriesAction({type: 'remove-chart-series', address: mockAddress, parameters: {series: [{id: 'series1'}]}})(dispatch, getState);

        const [[updateAttributes]] = dispatch.mock.calls;
        expect(echartsSeries(updateAttributes)).toEqual([{id: 'series2'}]);
        expect(dispatch.mock.calls[1][0].type).toBe('ACCEPT_ACTION');
      });

      it('should not fail when the chart has no echartsModel', () => {
        delete mockComponent.attributes.echartsModel;

        thunks.removeSeriesAction({type: 'remove-chart-series', address: mockAddress, parameters: {series: [{id: 'series1'}]}})(dispatch, getState);

        expect(dispatch.mock.calls[dispatch.mock.calls.length - 1][0].type).toBe('ACCEPT_ACTION');
      });
    });

    describe('replaceSeriesAction', () => {
      it('should replace all the series and rebuild the values from the new ones', () => {
        mockComponent.attributes.echartsModel.series = [{id: 'series1'}];

        thunks.replaceSeriesAction({type: 'replace-chart-series', address: mockAddress, parameters: {series: [lineSeries('newSeries')]}})(dispatch, getState);

        const [[updateAttributes]] = dispatch.mock.calls;
        expect(echartsSeries(updateAttributes)).toEqual([lineSeries('newSeries').echarts]);
        expect(valuesOf(dispatch.mock.calls[1][0])).toEqual([
          {date: 1, newSeries: 10},
          {date: 2, newSeries: 20}
        ]);
      });
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

      const [[updateAttributes]] = dispatch.mock.calls;
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

      const [[updateAttributes]] = dispatch.mock.calls;
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

      const [[updateAttributes]] = dispatch.mock.calls;
      expect(updateAttributes.data.cols).toBe('year,month');
    });
  });

  describe('toggleMenuAction', () => {
    it('debería alternar el estado minimizado del menú', () => {
      mockComponent.attributes.minimized = false;
      const action = { type: 'toggle-menu', address: mockAddress };

      thunks.toggleMenuAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.mock.calls;
      expect(updateAttributes.data.minimized).toBe(true);
    });

    it('debería cambiar de minimizado a expandido', () => {
      mockComponent.attributes.minimized = true;
      const action = { type: 'toggle-menu', address: mockAddress };

      thunks.toggleMenuAction(action)(dispatch, getState);

      const [[updateAttributes]] = dispatch.mock.calls;
      expect(updateAttributes.data.minimized).toBe(false);
    });
  });

  describe('toggleNavbarAction', () => {
    it('debería aceptar la acción', () => {
      const action = { type: 'toggleNavbar' };

      thunks.toggleNavbarAction(action)(dispatch);

      const [[acceptAction]] = dispatch.mock.calls;
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('changeMenuAction', () => {
    it('debería actualizar las opciones del menú', () => {
      const setOptionsSpy = jest.spyOn(MenuRegistry, 'setOptions');
      const action = {
        type: 'changeMenu',
        address: mockAddress,
        parameters: {
          options: [
            { key: 'item1', visible: true, restricted: false, options: [] },
            { key: 'item2', visible: true, restricted: true, options: [] }
          ]
        }
      };

      thunks.changeMenuAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();
      expect(setOptionsSpy).toHaveBeenCalledWith(action.parameters.options);

      const [[acceptAction]] = dispatch.mock.calls;
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });
});
