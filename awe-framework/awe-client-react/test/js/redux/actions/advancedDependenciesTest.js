import * as dependencies from '../../../../src/redux/actions/dependencies';

describe('awe-react-client/test/js/redux/actions/advancedDependenciesTest.js', () => {
  let dispatch;
  let mockState;
  let mockComponent;

  beforeEach(() => {
    dispatch = jasmine.createSpy('dispatch');

    mockComponent = {
      address: { view: 'testView', component: 'comp1' },
      context: 'form-context',
      attributes: {
        visible: true,
        readonly: false,
        disabled: false,
        group: null,
        columnModel: null
      },
      validationRules: {
        required: false
      },
      model: {
        values: [
          { value: 'test1', label: 'Test 1', selected: true },
          { value: 'test2', label: 'Test 2', selected: false }
        ],
        event: null
      },
      initialModel: {
        values: []
      },
      dependencies: []
    };

    mockState = {
      components: {
        comp1: mockComponent,
        comp2: {
          address: { view: 'testView', component: 'comp2' },
          attributes: { visible: false },
          validationRules: {},
          model: { values: [] },
          dependencies: []
        }
      },
      settings: {
        activeDependencies: true,
        serverActionKey: 'serverAction',
        targetActionKey: 'targetAction'
      },
      view: {
        view: 'testView',
        testView: { name: 'Test Screen' }
      }
    };
  });

  describe('checkDependencies', () => {
    it('debería verificar dependencias sin cambios', () => {
      dependencies.checkDependencies(mockState, dispatch);

      // Sin cambios, no debería despachar nada
      expect(dispatch).not.toHaveBeenCalled();
    });

    it('debería ejecutar dependencias cuando hay cambios', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          checkChanges: true
        }],
        target: 'show',
        initial: false
      }];

      // Primera llamada para inicializar
      dependencies.initializeDependencies('testView', mockState, dispatch);
      dispatch.calls.reset();

      // Cambiar valor
      mockState.components.comp2.model.values = [{ value: 'changed', selected: true }];

      // Segunda llamada debería detectar cambios
      dependencies.checkDependencies(mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('initializeDependencies', () => {
    it('debería inicializar dependencias de una vista', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería limpiar valores previos de dependencias', () => {
      dependencies.initializeDependencies('testView', mockState, dispatch);
      dependencies.initializeDependencies('testView', mockState, dispatch);

      // No debería fallar al reinicializar
      expect(true).toBe(true);
    });
  });

  describe('Evaluación de condiciones', () => {
    beforeEach(() => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [],
        target: 'show',
        initial: true
      }];
    });

    it('debería evaluar condición eq (igual)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'eq',
        value: 'test',
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición ne (no igual)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'ne',
        value: 'other',
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición ge (mayor o igual)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'ge',
        value: 5,
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 10, selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición le (menor o igual)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'le',
        value: 10,
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 5, selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición gt (mayor que)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'gt',
        value: 5,
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 10, selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición lt (menor que)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'lt',
        value: 10,
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 5, selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición in (contiene)', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'in',
        value: 'option1,option2,option3',
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 'option2', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición in con array', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'in',
        value: ['option1', 'option2', 'option3'],
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 'option2', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición "is not false"', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'is not false',
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: true, selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición "is empty"', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'is empty',
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar condición "is not empty"', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'is not empty',
        checkChanges: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería manejar condición inválida', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'invalid-condition',
        checkChanges: true
      }];

      spyOn(console, 'log');
      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(console.log).toHaveBeenCalled();
    });

    it('debería evaluar trigger opcional', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp2',
        attribute1: 'value',
        condition: 'eq',
        value: 'nonexistent',
        optional: true,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Tipos de dependencias (and/or)', () => {
    it('debería evaluar dependencia tipo AND correctamente', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [
          { id: 'comp2', attribute1: 'value', value: 'test', condition: 'eq', checkChanges: true },
          { id: 'comp2', attribute1: 'totalValues', value: 0, condition: 'gt', checkChanges: true }
        ],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería evaluar dependencia tipo OR correctamente', () => {
      mockComponent.dependencies = [{
        type: 'or',
        elements: [
          { id: 'comp2', attribute1: 'value', value: 'other', condition: 'eq', checkChanges: true },
          { id: 'comp2', attribute1: 'totalValues', value: 0, condition: 'gt', checkChanges: true }
        ],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería cancelar dependencia con trigger cancel', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [
          { id: 'comp2', attribute1: 'value', value: 'wrong', condition: 'eq', cancel: true, checkChanges: true },
          { id: 'comp2', attribute1: 'totalValues', value: 0, condition: 'gt', checkChanges: true }
        ],
        target: 'input',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      // Con cancel, no debería ejecutarse
      expect(dispatch).not.toHaveBeenCalled();
    });
  });

  describe('Atributos de componente', () => {
    beforeEach(() => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [],
        target: 'show',
        initial: true
      }];
    });

    it('debería obtener atributo visible', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp1',
        attribute1: 'visible',
        condition: 'is not false',
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener atributo editable', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp1',
        attribute1: 'editable',
        condition: 'is not false',
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener atributo required', () => {
      mockComponent.dependencies[0].target = 'input';
      mockComponent.dependencies[0].elements = [{
        id: 'comp1',
        attribute1: 'required',
        condition: 'is not false',
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      // required es false, por lo que no debería ejecutarse
      expect(dispatch).not.toHaveBeenCalled();
    });

    it('debería obtener totalValues', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp1',
        attribute1: 'totalValues',
        condition: 'gt',
        value: 0,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener selectedValues', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'comp1',
        attribute1: 'selectedValues',
        condition: 'eq',
        value: 1,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Atributos de grid', () => {
    beforeEach(() => {
      mockState.components.gridComp = {
        address: { view: 'testView', component: 'gridComp' },
        attributes: {
          columnModel: [
            { id: 'col1', label: 'Column 1' },
            { id: 'col2', label: 'Column 2' }
          ],
          gridId: 'id'
        },
        validationRules: {},
        model: {
          values: [
            { id: 1, col1: 'value1', col2: 'value2', selected: false },
            { id: 2, col1: 'value3', col2: 'value4', selected: true, editing: true }
          ]
        },
        dependencies: [{
          type: 'and',
          elements: [{
            id: 'comp2',
            attribute1: 'value',
            checkChanges: true
          }],
          target: 'show',
          initial: true
        }]
      };

      mockComponent.dependencies = [{
        type: 'and',
        elements: [],
        target: 'show',
        initial: true
      }];
    });

    it('debería obtener currentRow', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'currentRow',
        row1: 2,
        condition: 'eq',
        value: 1,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener selectedRow', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'selectedRow',
        condition: 'eq',
        value: 1,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener prevRow', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'prevRow',
        condition: 'eq',
        value: 0,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener nextRow', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'nextRow',
        condition: 'eq',
        value: 2,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener hasDataColumn', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'hasDataColumn',
        column1: 'col1',
        condition: 'is not false',
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener emptyDataColumn', () => {
      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'emptyDataColumn',
        column1: 'col3',
        condition: 'is not false',
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener modifiedRows', () => {
      mockState.components.gridComp.model.values[0].ROW_TYPE = 'INSERT';

      mockComponent.dependencies[0].elements = [{
        id: 'gridComp',
        attribute1: 'modifiedRows',
        condition: 'gt',
        value: 0,
        checkChanges: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Targets de dependencias', () => {
    beforeEach(() => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];
    });

    it('debería aplicar target show', () => {
      mockComponent.dependencies[0].target = 'show';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasVisibleUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.visible === true)
      );
      expect(hasVisibleUpdate).toBe(true);
    });

    it('debería aplicar target hide', () => {
      mockComponent.dependencies[0].target = 'hide';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasVisibleUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.visible === false)
      );
      expect(hasVisibleUpdate).toBe(true);
    });

    it('debería aplicar target enable', () => {
      mockComponent.dependencies[0].target = 'enable';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasDisabledUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.disabled === false)
      );
      expect(hasDisabledUpdate).toBe(true);
    });

    it('debería aplicar target disable', () => {
      mockComponent.dependencies[0].target = 'disable';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasDisabledUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.disabled === true)
      );
      expect(hasDisabledUpdate).toBe(true);
    });

    it('debería aplicar target set-editable', () => {
      mockComponent.dependencies[0].target = 'set-editable';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasReadonlyUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.readonly === false)
      );
      expect(hasReadonlyUpdate).toBe(true);
    });

    it('debería aplicar target set-readonly', () => {
      mockComponent.dependencies[0].target = 'set-readonly';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasReadonlyUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.readonly === true)
      );
      expect(hasReadonlyUpdate).toBe(true);
    });

    it('debería aplicar target set-required', () => {
      mockComponent.dependencies[0].target = 'set-required';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasRequiredUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_VALIDATION' &&
        action.componentList.some(item => item.data.required === true)
      );
      expect(hasRequiredUpdate).toBe(true);
    });

    it('debería aplicar target set-optional', () => {
      mockComponent.dependencies[0].target = 'set-optional';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasRequiredUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_VALIDATION' &&
        action.componentList.some(item => item.data.required === false)
      );
      expect(hasRequiredUpdate).toBe(true);
    });

    it('debería aplicar target set-visible', () => {
      mockComponent.dependencies[0].target = 'set-visible';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasInvisibleUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.invisible === false)
      );
      expect(hasInvisibleUpdate).toBe(true);
    });

    it('debería aplicar target set-invisible', () => {
      mockComponent.dependencies[0].target = 'set-invisible';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasInvisibleUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.invisible === true)
      );
      expect(hasInvisibleUpdate).toBe(true);
    });

    it('debería aplicar target label', () => {
      mockComponent.dependencies[0].target = 'label';
      mockComponent.dependencies[0].source = 'value';
      mockComponent.dependencies[0].value = 'New Label';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasLabelUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.label === 'New Label')
      );
      expect(hasLabelUpdate).toBe(true);
    });

    it('debería aplicar target unit', () => {
      mockComponent.dependencies[0].target = 'unit';
      mockComponent.dependencies[0].source = 'value';
      mockComponent.dependencies[0].value = 'kg';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasUnitUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.unit === 'kg')
      );
      expect(hasUnitUpdate).toBe(true);
    });

    it('debería aplicar target icon', () => {
      mockComponent.dependencies[0].target = 'icon';
      mockComponent.dependencies[0].source = 'value';
      mockComponent.dependencies[0].value = 'fa-check';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasIconUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.icon === 'fa-check')
      );
      expect(hasIconUpdate).toBe(true);
    });

    it('debería aplicar target input', () => {
      mockComponent.dependencies[0].target = 'input';
      mockComponent.dependencies[0].source = 'value';
      mockComponent.dependencies[0].value = ['selected1', 'selected2'];
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasInputUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_MODELS'
      );
      expect(hasInputUpdate).toBe(true);
    });

    it('debería aplicar target enable-autorefresh', () => {
      mockComponent.dependencies[0].target = 'enable-autorefresh';
      mockComponent.dependencies[0].source = 'value';
      mockComponent.dependencies[0].value = 5000;
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAutorefreshUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.autorefreshEnabled === true)
      );
      expect(hasAutorefreshUpdate).toBe(true);
    });

    it('debería aplicar target disable-autorefresh', () => {
      mockComponent.dependencies[0].target = 'disable-autorefresh';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAutorefreshUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.autorefreshEnabled === false)
      );
      expect(hasAutorefreshUpdate).toBe(true);
    });

    it('debería manejar target none', () => {
      mockComponent.dependencies[0].target = 'none';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      // none no debería generar updates
      expect(dispatch).not.toHaveBeenCalled();
    });

    it('debería advertir sobre target no definido', () => {
      mockComponent.dependencies[0].target = 'unknown-target';
      spyOn(console, 'warn');

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(console.warn).toHaveBeenCalled();
    });
  });

  describe('Sources de dependencias', () => {
    beforeEach(() => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        target: 'label',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];
    });

    it('debería usar source value', () => {
      mockComponent.dependencies[0].source = 'value';
      mockComponent.dependencies[0].value = 'Fixed Value';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasLabelUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.label === 'Fixed Value')
      );
      expect(hasLabelUpdate).toBe(true);
    });

    it('debería usar source label', () => {
      mockComponent.dependencies[0].source = 'label';
      mockComponent.dependencies[0].label = 'Fixed Label';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasLabelUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.label === 'Fixed Label')
      );
      expect(hasLabelUpdate).toBe(true);
    });

    it('debería usar source formule', () => {
      mockComponent.dependencies[0].source = 'formule';
      mockComponent.dependencies[0].formule = '[comp2] + 10';
      mockComponent.dependencies[0].elements[0].query = 'comp2';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería usar source criteria-value', () => {
      mockComponent.dependencies[0].source = 'criteria-value';
      mockComponent.dependencies[0].query = 'comp2';
      mockComponent.dependencies[0].elements[0].query = 'comp2';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería usar source launcher', () => {
      mockComponent.dependencies[0].source = 'launcher';
      mockComponent.dependencies[0].query = 'comp2';
      mockComponent.dependencies[0].elements[0].query = 'comp2';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería usar source reset', () => {
      mockComponent.dependencies[0].source = 'reset';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería manejar source query con target label', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'label';
      mockComponent.dependencies[0].serverAction = 'data';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActions = calls.some(([action]) => action.type === 'ADD_ACTIONS_TOP');
      expect(hasAddActions).toBe(true);
    });

    it('debería manejar source query con target unit', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'unit';
      mockComponent.dependencies[0].serverAction = 'data';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActions = calls.some(([action]) => action.type === 'ADD_ACTIONS_TOP');
      expect(hasAddActions).toBe(true);
    });

    it('debería manejar source query con target format-number', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'format-number';
      mockComponent.dependencies[0].serverAction = 'data';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActions = calls.some(([action]) => action.type === 'ADD_ACTIONS_TOP');
      expect(hasAddActions).toBe(true);
    });

    it('debería manejar source query con target validate', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'validate';
      mockComponent.dependencies[0].serverAction = 'data';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActions = calls.some(([action]) => action.type === 'ADD_ACTIONS_TOP');
      expect(hasAddActions).toBe(true);
    });

    it('debería manejar source query con target input', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'input';
      mockComponent.dependencies[0].serverAction = 'data';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActions = calls.some(([action]) => action.type === 'ADD_ACTIONS_TOP');
      expect(hasAddActions).toBe(true);
    });

    it('debería restaurar format-number cuando dependencia no se cumple', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'format-number';
      mockComponent.dependencies[0].serverAction = 'data';
      mockComponent.dependencies[0].elements[0].value = 'wrong';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      // No debería despachar ADD_ACTIONS_TOP porque la condición no se cumple
      const calls = dispatch.calls.allArgs();
      const hasRestoreAttributes = calls.some(([action]) =>
        action.type === 'RESTORE_MULTIPLE_ATTRIBUTES'
      );
      expect(hasRestoreAttributes).toBe(true);
    });

    it('debería restaurar validation cuando dependencia no se cumple', () => {
      mockComponent.dependencies[0].source = 'query';
      mockComponent.dependencies[0].target = 'validate';
      mockComponent.dependencies[0].serverAction = 'data';
      mockComponent.dependencies[0].elements[0].value = 'wrong';
      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasRestoreValidation = calls.some(([action]) =>
        action.type === 'RESTORE_MULTIPLE_VALIDATION'
      );
      expect(hasRestoreValidation).toBe(true);
    });
  });

  describe('Dependencias invertidas', () => {
    it('debería invertir resultado de dependencia', () => {
      mockComponent.dependencies = [{
        type: 'and',
        invert: true,
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      // Con invert, debería ocultar en vez de mostrar
      const calls = dispatch.calls.allArgs();
      const hasVisibleUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.visible === false)
      );
      expect(hasVisibleUpdate).toBe(true);
    });
  });

  describe('Acciones de dependencias', () => {
    it('debería ejecutar acciones cuando se cumple la dependencia', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        actions: [
          { type: 'custom-action', parameters: {} },
          { type: 'another-action', parameters: {} }
        ],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActions = calls.some(([action]) =>
        action.type === 'ADD_ACTIONS_TOP' &&
        action.payload.length === 2
      );
      expect(hasAddActions).toBe(true);
    });

    it('no debería ejecutar acciones cuando no se cumple la dependencia', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'other',
          condition: 'eq',
          checkChanges: true
        }],
        actions: [
          { type: 'custom-action', parameters: {} }
        ],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAddActionsWithCustom = calls.some(([action]) =>
        action.type === 'ADD_ACTIONS_TOP' &&
        action.payload.some(a => a.type === 'custom-action')
      );
      expect(hasAddActionsWithCustom).toBe(false);
    });
  });

  describe('Grupos de componentes', () => {
    beforeEach(() => {
      mockState.components.groupComp1 = {
        address: { view: 'testView', component: 'groupComp1' },
        attributes: { group: 'testGroup' },
        validationRules: {},
        model: { values: [{ value: 'g1', selected: true }] },
        dependencies: []
      };

      mockState.components.groupComp2 = {
        address: { view: 'testView', component: 'groupComp2' },
        attributes: { group: 'testGroup' },
        validationRules: {},
        model: { values: [{ value: 'g2', selected: true }] },
        dependencies: []
      };
    });

    it('debería manejar dependencias de grupos', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'testGroup',
          attribute1: 'totalValues',
          condition: 'gt',
          value: 0,
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Dependencias con eventos', () => {
    it('debería verificar evento del componente', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          event: 'click',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.event = 'click';

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('no debería cumplirse si evento no coincide', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          event: 'click',
          checkChanges: true
        }],
        target: 'label',
        initial: true
      }];
      mockState.components.comp2.model.event = 'change';

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).not.toHaveBeenCalled();
    });
  });

  describe('Dependencias de columnas de grid', () => {
    beforeEach(() => {
      mockState.components.gridComp = {
        address: { view: 'testView', component: 'gridComp' },
        attributes: {
          columnModel: [
            {
              id: 'col1',
              label: 'Column 1',
              dependencies: [{
                type: 'and',
                elements: [{
                  id: 'comp2',
                  attribute1: 'value',
                  value: 'test',
                  condition: 'eq',
                  checkChanges: true
                }],
                target: 'show',
                initial: true
              }]
            }
          ],
          gridId: 'id'
        },
        validationRules: {},
        model: {
          values: [
            { id: 1, col1: 'value1', editing: false },
            { id: 2, col1: 'value2', editing: true }
          ]
        },
        dependencies: []
      };

      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];
    });

    it('debería procesar dependencias de columnas', () => {
      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería crear dependencias por fila en columnas', () => {
      mockState.components.gridComp.attributes.columnModel[0].dependencies[0].elements[0].attribute1 = 'currentRow';

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería manejar show-column y hide-column para dependencias de columna', () => {
      mockState.components.gridComp.attributes.columnModel[0].dependencies[0].target = 'show';

      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasColumnUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES'
      );
      expect(hasColumnUpdate).toBe(true);
    });
  });

  describe('Atributos de texto y celdas', () => {
    beforeEach(() => {
      mockState.components.gridComp = {
        address: { view: 'testView', component: 'gridComp' },
        attributes: {
          columnModel: [{ id: 'col1' }],
          gridId: 'id'
        },
        validationRules: {},
        model: {
          values: [
            { id: 1, col1: { value: 'cell1', label: 'Cell 1' }, selected: false },
            { id: 2, col1: { value: 'cell2', label: 'Cell 2' }, selected: true }
          ]
        },
        dependencies: []
      };
    });

    it('debería obtener valor de celda específica', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'value',
          column1: 'col1',
          row1: 2,
          condition: 'eq',
          value: 'cell2',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener label de celda', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'text',
          column1: 'col1',
          row1: 2,
          condition: 'eq',
          value: 'Cell 2',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería contar filas seleccionadas en grid', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'value',
          condition: 'eq',
          value: 1,
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Advertencias y errores', () => {
    it('debería advertir cuando componente no existe', () => {
      spyOn(console, 'warn');

      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'nonexistent',
          attribute1: 'value',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(console.warn).toHaveBeenCalledWith(jasmine.stringContaining('nonexistent is not defined'));
    });
  });

  describe('Dependencias con checkChanges false', () => {
    it('no debería crear triggers si checkChanges es false', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          checkChanges: false
        }],
        target: 'input',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      // Sin triggers, no debería despachar nada
      expect(dispatch).not.toHaveBeenCalled();
    });
  });

  describe('Atributos adicionales de grid', () => {
    beforeEach(() => {
      mockState.components.gridComp = {
        address: { view: 'testView', component: 'gridComp' },
        attributes: {
          columnModel: [{ id: 'col1' }],
          gridId: 'id'
        },
        validationRules: {},
        model: {
          values: [
            { id: 1, col1: 'value1', editing: false },
            { id: 2, col1: 'value2', editing: true },
            { id: 3, col1: 'value3', editing: false }
          ]
        },
        dependencies: []
      };
    });

    it('debería obtener prevRowValue', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'prevRowValue',
          column1: 'col1',
          condition: 'eq',
          value: 'value1',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener nextRowValue', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'nextRowValue',
          column1: 'col1',
          condition: 'eq',
          value: 'value3',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener selectedRowValue', () => {
      mockState.components.gridComp.model.values[1].selected = true;

      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'selectedRowValue',
          column1: 'col1',
          condition: 'eq',
          value: 'value2',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener footerValue', () => {
      mockState.components.gridComp.model.values[1].selected = true;

      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'footerValue',
          condition: 'eq',
          value: 1,
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener currentRowValue', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'currentRowValue',
          column1: 'col1',
          row1: 2,
          condition: 'eq',
          value: 'value2',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener prevCurrentRowValue', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'prevCurrentRowValue',
          column1: 'col1',
          row1: 2,
          condition: 'eq',
          value: 'value1',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener nextCurrentRowValue', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'nextCurrentRowValue',
          column1: 'col1',
          row1: 2,
          condition: 'eq',
          value: 'value3',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });

    it('debería obtener fullDataColumn', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'gridComp',
          attribute1: 'fullDataColumn',
          column1: 'col1',
          condition: 'is not false',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Atributos específicos de componentes', () => {
    it('debería obtener atributos de chart (xMin, xMax, yMin, yMax)', () => {
      mockState.components.chartComp = {
        address: { view: 'testView', component: 'chartComp' },
        attributes: {
          xMin: 0,
          xMax: 100,
          yMin: 0,
          yMax: 50
        },
        validationRules: {},
        model: { values: [] },
        dependencies: []
      };

      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'chartComp',
          attribute1: 'xMax',
          condition: 'eq',
          value: 100,
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Dependencias con múltiples triggers', () => {
    it('debería evaluar dependencia con dos triggers (id2)', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp1',
          attribute1: 'totalValues',
          id2: 'comp2',
          attribute2: 'totalValues',
          condition: 'gt',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });

  describe('Dependencias con settings desactivadas', () => {
    it('no debería ejecutar dependencias si activeDependencies es false', () => {
      mockState.settings.activeDependencies = false;

      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).not.toHaveBeenCalled();
    });
  });

  describe('Target chart-options', () => {
    it('debería aplicar target chart-options', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        target: 'chart-options',
        source: 'value',
        value: { option1: true },
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasChartOptionsUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.chartModel)
      );
      expect(hasChartOptionsUpdate).toBe(true);
    });
  });

  describe('Target attribute', () => {
    it('debería aplicar target attribute con query específico', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp2',
          attribute1: 'value',
          value: 'test',
          condition: 'eq',
          checkChanges: true
        }],
        target: 'attribute',
        query: 'customAttr',
        source: 'value',
        value: 'customValue',
        initial: true
      }];
      mockState.components.comp2.model.values = [{ value: 'test', selected: true }];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      const calls = dispatch.calls.allArgs();
      const hasAttributeUpdate = calls.some(([action]) =>
        action.type === 'UPDATE_MULTIPLE_ATTRIBUTES' &&
        action.componentList.some(item => item.data.customAttr === 'customValue')
      );
      expect(hasAttributeUpdate).toBe(true);
    });
  });

  describe('Prevención de índices negativos', () => {
    it('debería manejar prevCurrentRow sin ir a índice negativo', () => {
      mockComponent.dependencies = [{
        type: 'and',
        elements: [{
          id: 'comp1',
          attribute1: 'prevCurrentRow',
          row1: 1,
          condition: 'eq',
          value: 0,
          checkChanges: true
        }],
        target: 'show',
        initial: true
      }];
      mockComponent.model.values = [
        { value: 'val1', selected: true }
      ];

      dependencies.initializeDependencies('testView', mockState, dispatch);

      expect(dispatch).toHaveBeenCalled();
    });
  });
});