import { getComponentDeltas, makeGetComponent, getAllComponents, makeGetMultipleComponents, getContextMenuAttributes } from '../../../../src/redux/selectors/componentSelectors';
import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';

describe('awe-react-client/test/js/redux/selectors/componentSelectorsTest.js', () => {
    let mockState;

    beforeEach(() => {
        // Limpiar registry
        ComponentRegistry.clearAll();

        // Estado mock
        mockState = {
            components: {
                'comp1': {
                    model: { values: [1, 2, 3] }
                },
                'comp2': {
                    attributes: { visible: false },
                    model: { values: [4, 5, 6] }
                }
            }
        };

        // Registrar componentes base
        ComponentRegistry.register('comp1', {
            address: { view: 'base', component: 'comp1' },
            attributes: { label: 'Component 1', visible: true },
            validationRules: { required: true },
            dependencies: [],
            actions: []
        });

        ComponentRegistry.register('comp2', {
            address: { view: 'base', component: 'comp2' },
            attributes: { label: 'Component 2', visible: true },
            validationRules: {},
            dependencies: [],
            actions: []
        });
    });

    afterEach(() => {
        ComponentRegistry.clearAll();
    });

    describe('state without settings', () => {
        it('merges base and deltas whatever the settings are', () => {
            expect(makeGetComponent()(mockState, 'comp2').attributes).toEqual({ label: 'Component 2', visible: false });
            expect(getAllComponents(mockState).comp2.attributes.visible).toBe(false);
            expect(makeGetMultipleComponents(['comp2'])(mockState).comp2.attributes.label).toBe('Component 2');
        });
    });

    describe('makeGetComponent', () => {
        it('debería mergear base + deltas correctamente', () => {
            const selector = makeGetComponent();
            const result = selector(mockState, 'comp1');

            expect(result.address).toEqual({ view: 'base', component: 'comp1' });
            expect(result.attributes.label).toBe('Component 1');
            expect(result.model).toEqual({ values: [1, 2, 3] });
        });

        it('debería sobrescribir attributes del base con deltas', () => {
            const selector = makeGetComponent();
            const result = selector(mockState, 'comp2');

            expect(result.attributes.label).toBe('Component 2');
            expect(result.attributes.visible).toBe(false); // Del delta
        });

        it('debería manejar componente solo en Registry', () => {
            ComponentRegistry.register('comp3', {
                address: { view: 'base', component: 'comp3' },
                attributes: { label: 'Component 3' },
                validationRules: {},
                dependencies: [],
                actions: []
            });

            const selector = makeGetComponent();
            const result = selector(mockState, 'comp3');

            expect(result.attributes.label).toBe('Component 3');
        });

        it('debería manejar componente solo en Redux', () => {
            mockState.components['comp4'] = {
                model: { values: [7, 8, 9] }
            };

            const selector = makeGetComponent();
            const result = selector(mockState, 'comp4');

            expect(result.model).toEqual({ values: [7, 8, 9] });
        });

        it('debería retornar objeto vacío para componente inexistente', () => {
            const selector = makeGetComponent();
            const result = selector(mockState, 'nonexistent');

            expect(result).toEqual({});
        });

        it('debería ser memoizado', () => {
            const selector = makeGetComponent();
            const result1 = selector(mockState, 'comp1');
            const result2 = selector(mockState, 'comp1');

            expect(result1).toBe(result2); // Misma referencia
        });

        it('debería recalcular si cambian los deltas', () => {
            const selector = makeGetComponent();
            const result1 = selector(mockState, 'comp1');

            // Cambiar deltas (inmutablemente)
            mockState = {
                ...mockState,
                components: {
                    ...mockState.components,
                    'comp1': {
                        model: { values: [10, 11, 12] }
                    }
                }
            };

            const result2 = selector(mockState, 'comp1');

            expect(result1).not.toBe(result2);
            expect(result2.model).toEqual({ values: [10, 11, 12] });
        });
    });

    describe('getAllComponents', () => {
        it('debería retornar todos los componentes mergeados', () => {
            const result = getAllComponents(mockState);

            expect(Object.keys(result)).toContain('comp1');
            expect(Object.keys(result)).toContain('comp2');
            expect(result['comp1'].attributes.label).toBe('Component 1');
            expect(result['comp2'].attributes.visible).toBe(false);
        });

        it('debería incluir componentes solo en Registry', () => {
            ComponentRegistry.register('comp3', {
                address: { view: 'base', component: 'comp3' },
                attributes: { label: 'Component 3' },
                validationRules: {},
                dependencies: [],
                actions: []
            });

            const result = getAllComponents(mockState);

            expect(Object.keys(result)).toContain('comp3');
            expect(result['comp3'].attributes.label).toBe('Component 3');
        });

        it('debería incluir componentes solo en Redux', () => {
            mockState.components['comp4'] = {
                model: { values: [7, 8, 9] }
            };

            const result = getAllComponents(mockState);

            expect(Object.keys(result)).toContain('comp4');
            expect(result['comp4'].model).toEqual({ values: [7, 8, 9] });
        });

        it('debería ser memoizado', () => {
            const result1 = getAllComponents(mockState);
            const result2 = getAllComponents(mockState);

            expect(result1).toBe(result2);
        });

        it('debería recalcular si cambian los deltas', () => {
            const result1 = getAllComponents(mockState);

            // Cambiar deltas (inmutablemente)
            mockState = {
                ...mockState,
                components: {
                    ...mockState.components,
                    'comp1': {
                        model: { values: [10, 11, 12] }
                    }
                }
            };

            const result2 = getAllComponents(mockState);

            expect(result1).not.toBe(result2);
        });

        it('debería manejar estado vacío', () => {
            mockState.components = {};
            ComponentRegistry.clearAll();

            const result = getAllComponents(mockState);

            expect(result).toEqual({});
        });
    });

    describe('makeGetMultipleComponents', () => {
        it('debería retornar solo los componentes solicitados', () => {
            const selector = makeGetMultipleComponents(['comp1', 'comp2']);
            const result = selector(mockState);

            expect(Object.keys(result)).toEqual(['comp1', 'comp2']);
            expect(result['comp1'].attributes.label).toBe('Component 1');
            expect(result['comp2'].attributes.visible).toBe(false);
        });

        it('debería manejar componentes inexistentes', () => {
            const selector = makeGetMultipleComponents(['comp1', 'nonexistent']);
            const result = selector(mockState);

            expect(Object.keys(result)).toContain('comp1');
            expect(Object.keys(result)).toContain('nonexistent');
            expect(result['nonexistent']).toEqual({});
        });

        it('debería ser memoizado', () => {
            const selector = makeGetMultipleComponents(['comp1', 'comp2']);
            const result1 = selector(mockState);
            const result2 = selector(mockState);

            expect(result1).toBe(result2);
        });

        it('debería manejar array vacío', () => {
            const selector = makeGetMultipleComponents([]);
            const result = selector(mockState);

            expect(result).toEqual({});
        });
    });

    describe('merged components are reused', () => {
        it('getAllComponents keeps the reference of a component whose base and delta did not change', () => {
            const before = getAllComponents(mockState);

            const after = getAllComponents({
                ...mockState,
                components: { ...mockState.components, comp2: { model: { values: [9] } } }
            });

            expect(after).not.toBe(before);
            expect(after.comp1).toBe(before.comp1);
            expect(after.comp2).not.toBe(before.comp2);
            expect(after.comp2.model.values).toEqual([9]);
        });

        it('getAllComponents builds a new object for a component registered again', () => {
            const before = getAllComponents(mockState);
            ComponentRegistry.register('comp1', {
                address: { view: 'base', component: 'comp1' },
                attributes: { label: 'Changed' }
            });

            const after = getAllComponents({ ...mockState, components: { ...mockState.components } });

            expect(after.comp1).not.toBe(before.comp1);
            expect(after.comp1.attributes.label).toBe('Changed');
            expect(after.comp2).toBe(before.comp2);
        });

        it('getAllComponents forgets the components that are neither registered nor in the state', () => {
            getAllComponents(mockState);
            ComponentRegistry.unregister('comp1');

            const after = getAllComponents({ components: { comp2: mockState.components.comp2 } });

            expect(Object.keys(after).sort()).toEqual(['comp2']);
        });

        it('makeGetComponent returns the same object while base and delta are unchanged, whatever the id asked before', () => {
            const selector = makeGetComponent();
            const first = selector(mockState, 'comp1');
            selector(mockState, 'comp2');

            expect(selector(mockState, 'comp1')).toBe(first);
            expect(selector({ ...mockState, components: { ...mockState.components } }, 'comp1')).toBe(first);
        });

        it('makeGetComponent returns the same object for a component that does not exist', () => {
            const selector = makeGetComponent();

            expect(selector(mockState, 'nonexistent')).toBe(selector({ components: {} }, 'nonexistent'));
        });

        it('does not create a new delta for a component without deltas', () => {
            const state = { components: {} };

            expect(getComponentDeltas(state, 'missing')).toBe(getComponentDeltas(state, 'other'));
            expect(Object.isFrozen(getComponentDeltas(state, 'missing'))).toBe(true);
        });
    });

    describe('stable references', () => {
        const withChangedComp3 = (state) => ({
            ...state,
            components: { ...state.components, comp3: { attributes: { label: 'Changed' } } }
        });

        it('keeps the reference of a requested component when another one changes', () => {
            const selector = makeGetMultipleComponents(['comp1', 'comp2']);
            const before = selector(mockState);

            const after = selector(withChangedComp3(mockState));

            expect(after.comp1).toBe(before.comp1);
            expect(after.comp2).toBe(before.comp2);
        });

        it('builds a new reference of a requested component when its delta changes', () => {
            const selector = makeGetMultipleComponents(['comp1']);
            const before = selector(mockState);

            const after = selector({
                ...mockState,
                components: { ...mockState.components, comp1: { attributes: { label: 'Changed' } } }
            });

            expect(after.comp1).not.toBe(before.comp1);
            expect(after.comp1.attributes.label).toBe('Changed');
        });
    });

    describe('getContextMenuAttributes', () => {
        const state = {
            components: {
                first: { attributes: { label: 'First', contextMenu: [{ id: 'child' }] } },
                child: { attributes: { label: 'Child' } },
                second: { attributes: { label: 'Second' } },
                unrelated: { attributes: { label: 'Unrelated' } }
            }
        };

        it('returns the attributes of the options and of their nested options', () => {
            const result = getContextMenuAttributes(state, [{ id: 'first' }, { id: 'second' }]);

            expect(Object.keys(result).sort()).toEqual(['child', 'first', 'second']);
            expect(result.child).toEqual(state.components.child.attributes);
        });

        it('skips the options that do not exist', () => {
            expect(getContextMenuAttributes(state, [{ id: 'missing' }])).toEqual({});
        });

        it('returns nothing without a context menu', () => {
            expect(getContextMenuAttributes(state, undefined)).toEqual({});
        });

        it('merges the base and the delta of the options with the component registry', () => {
            ComponentRegistry.register('first', {
                address: { view: 'base', component: 'first' },
                attributes: { label: 'Base', icon: 'check' }
            });

            const result = getContextMenuAttributes(state, [{ id: 'first' }]);

            expect(result.first).toMatchObject({ label: 'First', icon: 'check' });
        });

        it('keeps the references of the attributes when an unrelated component changes', () => {
            const before = getContextMenuAttributes(state, [{ id: 'first' }]);
            const after = getContextMenuAttributes(
                { ...state, components: { ...state.components, unrelated: { attributes: { label: 'New' } } } },
                [{ id: 'first' }]);

            expect(after.first).toBe(before.first);
            expect(after.child).toBe(before.child);
        });
    });

    describe('integration tests', () => {
        it('debería funcionar con componente completo', () => {
            ComponentRegistry.clearAll();
            ComponentRegistry.register('grid1', {
                address: { view: 'base', component: 'grid1' },
                attributes: {
                    label: 'My Grid',
                    visible: true,
                    readonly: false,
                    columnModel: [{ name: 'col1' }, { name: 'col2' }]
                },
                validationRules: { required: true },
                dependencies: [{ id: 'dep1' }],
                actions: [{ type: 'filter' }]
            });

            mockState.components['grid1'] = {
                attributes: { visible: false },
                model: {
                    values: [{ id: 1 }, { id: 2 }],
                    page: 1,
                    total: 1,
                    records: 2
                }
            };

            const selector = makeGetComponent();
            const result = selector(mockState, 'grid1');

            expect(result.address).toEqual({ view: 'base', component: 'grid1' });
            expect(result.attributes.label).toBe('My Grid');
            expect(result.attributes.visible).toBe(false);
            expect(result.attributes.columnModel.length).toBe(2);
            expect(result.model.values.length).toBe(2);
            expect(result.dependencies.length).toBe(1);
            expect(result.actions.length).toBe(1);
        });
    });
});
