import * as formThunks from '../../../../src/redux/thunks/form';
import {addActionsTop} from '../../../../src/redux/actions/actions';
import {createStore} from '../../../../src/redux/store';

describe('awe-react-client/test/js/redux/thunks/formThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockState;
  let mockAddress;
  let mockComponent;

  beforeEach(() => {
    dispatch = jest.fn();
    mockAddress = { view: 'base', component: 'comp1' };

    mockComponent = {
      address: mockAddress,
      context: {view: 'base', source: ['home', 'comp1']},
      attributes: {
        serverAction: 'data',
        targetAction: 'target',
        loading: false
      },
      specificAttributes: {
        filter: 'test'
      },
      model: {
        values: []
      }
    };

    mockState = {
      components: {
        comp1: mockComponent,
        submitBtn: {
          address: { view: 'base', component: 'submitBtn' },
          context: {view: 'base', source: ['home', 'submitBtn']},
          attributes: {
            buttonType: 'submit'
          }
        }
      },
      settings: {
        serverActionKey: 'serverAction',
        targetActionKey: 'targetAction'
      }
    };

    getState = jest.fn(() => mockState);
  });

  describe('submitAction', () => {
    it('debería encontrar el botón submit y despachar click', () => {
      const action = {
        type: 'submit',
        address: mockAddress
      };

      formThunks.submitAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[addActionsTop], [acceptAction]] = dispatch.mock.calls;
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload.length).toBe(1);
      expect(addActionsTop.payload[0].type).toBe('click');
      expect(addActionsTop.payload[0].address).toEqual({ view: 'base', component: 'submitBtn' });
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('debería aceptar acción si no hay botón submit', () => {
      getState = jest.fn(() => ({...mockState, components: {comp1: mockComponent}}));

      const action = {
        type: 'submit',
        address: mockAddress
      };

      formThunks.submitAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalledTimes(1);
      const [[acceptAction]] = dispatch.mock.calls;
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('resetAction', () => {
    it('debería resetear múltiples componentes en el contexto', () => {
      const action = {
        type: 'reset',
        address: mockAddress
      };

      formThunks.resetAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[resetThunk], [acceptAction]] = dispatch.mock.calls;
      // resetThunk es un thunk, lo ejecutamos para ver qué despacha
      const innerDispatch = jest.fn();
      resetThunk(innerDispatch, getState);

      expect(innerDispatch).toHaveBeenCalled();
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('restoreAction', () => {
    it('debería restaurar múltiples componentes en el contexto', () => {
      const action = {
        type: 'restore',
        address: mockAddress
      };

      formThunks.restoreAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[restoreMultipleModelWithDependencies],[acceptAction]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      restoreMultipleModelWithDependencies(innerDispatch, getState);
      const [[restoreMultipleModel]] = innerDispatch.mock.calls;

      expect(restoreMultipleModel.type).toBe('RESTORE_MULTIPLE_MODEL');
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('restore flavours', () => {
    const restoreWith = (thunk) => {
      thunk({type: 'restore', address: mockAddress})(dispatch, getState);
      const [[restoreMultipleModelWithDependencies]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      restoreMultipleModelWithDependencies(innerDispatch, getState);
      return innerDispatch.mock.calls[0][0];
    };

    it('restoreAction restores the default model', () => {
      expect(restoreWith(formThunks.restoreAction).initial).toBeFalsy();
    });

    it('restoreTargetAction restores the first loaded model', () => {
      const restoreMultipleModel = restoreWith(formThunks.restoreTargetAction);
      expect(restoreMultipleModel.type).toBe('RESTORE_MULTIPLE_MODEL');
      expect(restoreMultipleModel.initial).toBe(true);
    });
  });

  describe('filterAction', () => {
    it('debería generar y despachar una acción de servidor', () => {
      const action = {
        type: 'filter',
        address: mockAddress,
        async: true,
        silent: false
      };

      formThunks.filterAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[updateAttributes], [addActionsTop], [acceptAction]] = dispatch.mock.calls;
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload.length).toBe(1);
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    describe('when the target component is not registered in the view', () => {
      // Like the AngularJS client garbage action collector: an action targeting a component which does not exist
      // is aborted silently and the actions queue goes on, as the scheduler does with the "reload-execution-data"
      // filter of the "report" view, which only exists while the execution data dialog is open
      it.each([
        ['silent', true],
        ['not silent', false]
      ])('ignores a %s filter without throwing nor sending any message', (_label, silent) => {
        const action = {
          type: 'filter',
          address: {view: 'report', component: 'reload-execution-data'},
          async: true,
          silent
        };

        expect(() => formThunks.filterAction(action)(dispatch, getState)).not.toThrow();

        expect(dispatch).toHaveBeenCalledTimes(1);
        expect(dispatch.mock.calls[0][0].type).toBe('ACCEPT_ACTION');
      });

      it('lets the next queued action run', () => {
        const store = createStore();
        store.dispatch(addActionsTop([
          {type: 'filter', address: {view: 'report', component: 'reload-execution-data'}, silent: true},
          {type: 'confirm-marker'}
        ]));
        const [filter] = store.getState().actions.sync[0];
        expect(filter.status).toBe('STATUS_RUNNING');

        store.dispatch(formThunks.filterAction(filter));

        const {sync, async} = store.getState().actions;
        expect(sync[0].map(({type, status}) => ({type, status}))).toEqual([
          {type: 'confirm-marker', status: 'STATUS_RUNNING'}
        ]);
        expect(async).toEqual([]);
      });
    });
  });

  describe('fillAction', () => {
    it('debería llenar el modelo con datos de datalist', () => {
      const action = {
        type: 'fill',
        address: mockAddress,
        parameters: {
          datalist: {
            page: 1,
            records: 10,
            rows: [
              { id: 1, name: 'Item 1' },
              { id: 2, name: 'Item 2' }
            ]
          }
        }
      };

      formThunks.fillAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[updateThunk], [updateAttributes], [acceptAction]] = dispatch.mock.calls;
      // updateThunk es un thunk
      const innerDispatch = jest.fn();
      const innerGetState = jest.fn().mockReturnValue(mockState);
      updateThunk(innerDispatch, innerGetState);

      const [[updateModel]] = innerDispatch.mock.calls;
      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(updateModel.data.values.length).toBe(2);
      expect(updateModel.data.page).toBe(1);
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('updateControllerAction', () => {
    it('debería actualizar atributo del controlador con valor del parámetro', () => {
      const action = {
        type: 'update-controller',
        address: mockAddress,
        parameters: {
          attribute: 'selectedValue',
          value: 'newValue'
        }
      };

      formThunks.updateControllerAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateAttributes], [acceptAction]] = dispatch.mock.calls;
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(updateAttributes.data.selectedValue).toBe('newValue');
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('debería usar el primer valor de datalist si no hay valor en parámetros', () => {
      const action = {
        type: 'update-controller',
        address: mockAddress,
        parameters: {
          attribute: 'selectedValue',
          datalist: {
            rows: [
              { value: 'firstValue', label: 'First' },
              { value: 'secondValue', label: 'Second' }
            ]
          }
        }
      };

      formThunks.updateControllerAction(action)(dispatch);

      const [[updateAttributes]] = dispatch.mock.calls;
      expect(updateAttributes.data.selectedValue).toBe('firstValue');
    });
  });

  describe('selectAction', () => {
    it('debería actualizar los valores seleccionados', () => {
      const action = {
        type: 'select',
        address: mockAddress,
        parameters: {
          values: ['value1', 'value2', 'value3']
        }
      };

      formThunks.selectAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateThunk], [updateAttributes], [acceptAction]] = dispatch.mock.calls;
      const innerDispatch = jest.fn();
      const innerGetState = jest.fn().mockReturnValue(mockState);
      updateThunk(innerDispatch, innerGetState);

      const [[updateModel]] = innerDispatch.mock.calls;
      expect(updateModel.type).toBe('UPDATE_MODEL');
      expect(updateModel.data.selected).toEqual(['value1', 'value2', 'value3']);
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('startLoadAction', () => {
    it('debería marcar el componente como cargando', () => {
      const action = {
        type: 'start-load',
        address: mockAddress
      };

      formThunks.startLoadAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateAttributes], [acceptAction]] = dispatch.mock.calls;
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(updateAttributes.data.loading).toBe(true);
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('endLoadAction', () => {
    it('debería marcar el componente como no cargando', () => {
      const action = {
        type: 'end-load',
        address: mockAddress
      };

      formThunks.endLoadAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateAttributes], [acceptAction]] = dispatch.mock.calls;
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(updateAttributes.data.loading).toBe(false);
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('keepAction', () => {
    it('debería mantener el modelo', () => {
      const action = {
        type: 'keep',
        address: mockAddress
      };

      formThunks.keepAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[keepModel], [acceptAction]] = dispatch.mock.calls;
      expect(keepModel.type).toBe('KEEP_MODEL');
      expect(keepModel.address).toEqual(mockAddress);
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });
});
