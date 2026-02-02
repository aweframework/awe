import * as validationThunks from '../../../../src/redux/thunks/validate';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';

describe('awe-react-client/test/js/redux/thunks/validateThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockState;
  let mockAddress;

  beforeEach(() => {
    ComponentRegistry.clearAll();
    dispatch = jasmine.createSpy('dispatch');
    mockAddress = { view: 'base', component: 'comp1' };

    mockState = {
      components: {
        comp1: {
          address: mockAddress,
          context: {view: 'base', source: ['home', 'comp1']},
          attributes: {
            id: "comp1",
            checkEmpty: true
          },
          model: {
            values: [{value: 'value1', selected: true}]
          },
          storedModel: {
            values: [{value: 'value1', selected: true}]
          },
          initialModel: {
            values: [{value: 'value1', selected: true}]
          }
        },
        comp2: {
          address: { view: 'base', component: 'comp2' },
          context: {view: 'base', source: ['home', 'comp2']},
          attributes: {
            id: "comp2",
            error: { message: 'Invalid value' },
            checkEmpty: true
          },
          model: {
            values: []
          },
          storedModel: {
            values: []
          },
          initialModel: {
            values: []
          }
        }
      },
      settings: {
        serverActionKey: 'serverAction'
      }
    };

    getState = jasmine.createSpy('getState').and.callFake(() => mockState);
  });

  describe('validateComponents', () => {
    it('debería despachar validación de componentes y verificar validación', () => {
      const componentList = [mockState.components.comp1, mockState.components.comp2];

      validationThunks.validateComponents(componentList)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(2);

      const [[validateComponents], [addActionsTop]] = dispatch.calls.allArgs();
      expect(validateComponents.type).toBe('VALIDATE_COMPONENTS');
      expect(validateComponents.componentList).toBe(componentList);
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload[0].type).toBe('verify-validation');
    });
  });

  describe('validateRow', () => {
    it('debería despachar validación de fila y verificar validación', () => {
      validationThunks.validateRow(mockAddress)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(2);

      const [[validateRow], [addActionsTop]] = dispatch.calls.allArgs();
      expect(validateRow.type).toBe('VALIDATE_ROW');
      expect(validateRow.address).toBe(mockAddress);
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(addActionsTop.payload[0].type).toBe('verify-row-validation');
    });
  });

  describe('validateAction', () => {
    it('debería validar componentes en el contexto', () => {
      const action = {
        type: 'validate',
        address: mockAddress
      };

      validationThunks.validateAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(2);

      const [[validateThunk], [acceptAction]] = dispatch.calls.allArgs();
      // validateThunk es validateComponents
      const innerDispatch = jasmine.createSpy('innerDispatch');
      validateThunk(innerDispatch, getState);

      expect(innerDispatch.calls.count()).toBe(2);
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('debería usar ComponentRegistry cuando está habilitado', () => {
      ComponentRegistry.register('comp1', {
        address: mockAddress,
        context: {view: 'base', source: ['home', 'comp1']},
        attributes: {id: 'comp1'},
        model: {values: [{value: 'value1', selected: true}]}
      });
      ComponentRegistry.register('comp2', {
        address: { view: 'base', component: 'comp2' },
        context: {view: 'base', source: ['home', 'comp2']},
        attributes: {id: 'comp2'},
        model: {values: []}
      });

      mockState = {
        components: {},
        settings: {
          serverActionKey: 'serverAction',
          useComponentRegistry: true
        }
      };
      getState = jasmine.createSpy('getStateRegistry').and.callFake(() => mockState);

      const action = {
        type: 'validate',
        address: mockAddress
      };

      validationThunks.validateAction(action)(dispatch, getState);

      const [[validateThunk]] = dispatch.calls.allArgs();
      const innerDispatch = jasmine.createSpy('innerDispatch');
      validateThunk(innerDispatch, getState);

      const [[validateComponentsAction]] = innerDispatch.calls.allArgs();
      expect(validateComponentsAction.type).toBe('VALIDATE_COMPONENTS');
      expect(validateComponentsAction.componentList.length).toBe(2);
    });
  });

  describe('verifyValidationAction', () => {
    it('debería rechazar la acción si hay errores en componentes', () => {
      const action = {
        type: 'verify-validation'
      };

      validationThunks.verifyValidationAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[rejectAction]] = dispatch.calls.allArgs();
      expect(rejectAction.type).toBe('REJECT_ACTION');
    });

    it('debería aceptar la acción si no hay errores', () => {
      mockState.components.comp2.attributes.error = null;

      const action = {
        type: 'verify-validation'
      };

      validationThunks.verifyValidationAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[acceptAction]] = dispatch.calls.allArgs();
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('debería detectar errores desde ComponentRegistry cuando está habilitado', () => {
      ComponentRegistry.register('comp1', {
        address: mockAddress,
        context: {view: 'base', source: ['home', 'comp1']},
        attributes: {id: 'comp1', error: {message: 'Invalid value'}},
        model: {values: []}
      });

      mockState = {
        components: {},
        settings: {
          serverActionKey: 'serverAction',
          useComponentRegistry: true
        }
      };
      getState = jasmine.createSpy('getStateRegistry').and.callFake(() => mockState);

      const action = { type: 'verify-validation' };
      validationThunks.verifyValidationAction(action)(dispatch, getState);

      const [[rejectAction]] = dispatch.calls.allArgs();
      expect(rejectAction.type).toBe('REJECT_ACTION');
    });
  });

  describe('setValidAction', () => {
    it('debería limpiar el error del componente', () => {
      const action = {
        type: 'set-valid',
        address: mockAddress
      };

      validationThunks.setValidAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateAttributes], [acceptAction]] = dispatch.calls.allArgs();
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(updateAttributes.data.error).toBeNull();
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('setInvalidAction', () => {
    it('debería establecer el error del componente', () => {
      const action = {
        type: 'set-invalid',
        address: mockAddress,
        parameters: {
          message: 'Invalid value'
        }
      };

      validationThunks.setInvalidAction(action)(dispatch);

      expect(dispatch).toHaveBeenCalled();

      const [[updateAttributes], [acceptAction]] = dispatch.calls.allArgs();
      expect(updateAttributes.type).toBe('UPDATE_ATTRIBUTES');
      expect(updateAttributes.data.error).toEqual({ message: 'Invalid value' });
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('checkModelUpdatedAction', () => {
    it('debería despachar confirmación si el modelo ha sido modificado', () => {
      // Modificar el modelo para que sea diferente del inicial
      getState = jasmine.createSpy('getState2').and.callFake(() => ({
        ...mockState,
        components: {
          ...mockState.components,
          comp1: {
            ...mockState.components.comp1,
            model: { values: [{value: 'modified', selected: true}] },
            initialModel: mockState.components.comp1.model
          }
        }
      }));

      const action = {
        type: 'check-model-updated',
        address: mockAddress
      };

      validationThunks.checkModelUpdatedAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const calls = dispatch.calls.allArgs();
      expect(calls.length).toBeGreaterThanOrEqual(2);

      const [[addActionsTop], [acceptAction]] = calls;
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('no debería despachar confirmación si el modelo no ha sido modificado', () => {
      const action = {
        type: 'check-model-updated',
        address: mockAddress
      };

      validationThunks.checkModelUpdatedAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(1);

      const [[acceptAction]] = dispatch.calls.allArgs();
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('checkModelNoUpdatedAction', () => {
    it('debería despachar confirmación si el modelo no ha sido modificado', () => {
      const action = {
        type: 'check-model-no-updated',
        address: mockAddress
      };

      validationThunks.checkModelNoUpdatedAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const calls = dispatch.calls.allArgs();
      expect(calls.length).toBeGreaterThanOrEqual(2);

      const [[addActionsTop], [acceptAction]] = calls;
      expect(addActionsTop.type).toBe('ADD_ACTIONS_TOP');
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });

    it('no debería despachar confirmación si el modelo ha sido modificado', () => {
      getState = jasmine.createSpy('getState2').and.callFake(() => ({
        ...mockState,
        components: {
          ...mockState.components,
          comp1: {
            ...mockState.components.comp1,
            model: { values: [{value: 'modified', selected: true}] },
            initialModel: mockState.components.comp1.model
          }
        }
      }));

      const action = {
        type: 'check-model-no-updated',
        address: mockAddress
      };

      validationThunks.checkModelNoUpdatedAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();
      expect(dispatch.calls.count()).toBe(1);

      const [[acceptAction]] = dispatch.calls.allArgs();
      expect(acceptAction.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('checkModelEmptyAction', () => {
    it('debería despachar confirmación si el modelo está vacío', () => {
      getState = jasmine.createSpy('getState2').and.callFake(() => ({
        ...mockState,
        components: {
          ...mockState.components,
          comp1: {
            ...mockState.components.comp1,
            model: { values: [] },
            initialModel: mockState.components.comp1.model
          }
        }
      }));

      const action = {
        type: 'check-model-empty',
        address: mockAddress
      };

      validationThunks.checkModelEmptyAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const calls = dispatch.calls.allArgs();
      expect(calls.length).toBeGreaterThanOrEqual(1);

      const hasAddActionsTop = calls.some(([call]) => call.type === 'ADD_ACTIONS_TOP');
      expect(hasAddActionsTop).toBe(true);
    });

    it('debería despachar confirmación si modelo vacío', () => {
      getState = jasmine.createSpy('getState2').and.callFake(() => ({
        ...mockState,
        components: {
          ...mockState.components,
          comp1: {
            ...mockState.components.comp1,
            model: { values: [] },
            initialModel: { values: [{value: 'initial', selected: true}] },
          }
        }
      }));

      const action = {
        type: 'check-model-empty',
        address: mockAddress
      };

      validationThunks.checkModelEmptyAction(action)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const calls = dispatch.calls.allArgs();
      const addActionsTopCalls = calls.filter(([call]) => call.type === 'ADD_ACTIONS_TOP');
      expect(addActionsTopCalls.length).toBe(1);
    });

    it('no debería despachar nada si el modelo no está vacío', () => {
      const action = {
        type: 'check-model-empty',
        address: mockAddress
      };

      validationThunks.checkModelEmptyAction(action)(dispatch, getState);

      const calls = dispatch.calls.allArgs();
      const addActionsTopCalls = calls.filter(([call]) => call.type === 'ADD_ACTIONS_TOP');
      expect(addActionsTopCalls.length).toBe(0);
    });
  });
});
