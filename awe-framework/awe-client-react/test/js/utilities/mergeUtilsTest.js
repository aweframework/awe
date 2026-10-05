import { mergeComponentState, calculateDeltas } from '../../../src/utilities/mergeUtils';

describe('awe-react-client/test/js/utilities/mergeUtilsTest.js', () => {
    describe('mergeComponentState', () => {
        it('debería retornar deltas si no hay base', () => {
            const base = {};
            const deltas = { model: { values: [1, 2, 3] } };

            const result = mergeComponentState(base, deltas);

            expect(result).toEqual(deltas);
        });

        it('debería retornar base si no hay deltas', () => {
            const base = { attributes: { label: 'Test' } };
            const deltas = {};

            const result = mergeComponentState(base, deltas);

            expect(result).toEqual(base);
        });

        it('debería mergear attributes correctamente', () => {
            const base = {
                address: { view: 'base', component: 'comp1' },
                attributes: {
                    label: 'Original',
                    visible: true,
                    readonly: false
                }
            };

            const deltas = {
                attributes: {
                    visible: false
                }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.attributes).toEqual({
                label: 'Original',
                visible: false,
                readonly: false
            });
        });

        it('debería dar prioridad a deltas.model sobre base.model', () => {
            const base = {
                model: { values: [1, 2, 3] }
            };

            const deltas = {
                model: { values: [4, 5, 6] }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.model).toEqual({ values: [4, 5, 6] });
        });

        it('debería usar base.model si no hay deltas.model', () => {
            const base = {
                model: { values: [1, 2, 3] }
            };

            const deltas = {
                attributes: { visible: false }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.model).toEqual({ values: [1, 2, 3] });
        });

        it('debería mergear validationRules', () => {
            const base = {
                validationRules: {
                    required: true,
                    minLength: 5
                }
            };

            const deltas = {
                validationRules: {
                    required: false
                }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.validationRules).toEqual({
                required: false,
                minLength: 5
            });
        });

        it('debería mantener dependencies del base', () => {
            const base = {
                dependencies: [{ id: 'dep1' }, { id: 'dep2' }]
            };

            const deltas = {
                model: { values: [] }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.dependencies).toEqual(base.dependencies);
        });

        it('debería mantener actions del base', () => {
            const base = {
                actions: [{ type: 'action1' }]
            };

            const deltas = {
                model: { values: [] }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.actions).toEqual(base.actions);
        });

        it('debería usar defaultModel de base y de deltas', () => {
            const base = { defaultModel: { values: [1] } };

            expect(mergeComponentState(base, { model: { values: [2] } }).defaultModel).toEqual({ values: [1] });
            expect(mergeComponentState(base, { defaultModel: { values: [3] } }).defaultModel).toEqual({ values: [3] });
        });

        it('debería usar storedModel de deltas si existe', () => {
            const base = {
                storedModel: { values: [1, 2] }
            };

            const deltas = {
                storedModel: { values: [3, 4] }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.storedModel).toEqual({ values: [3, 4] });
        });

        it('debería usar storedModel de base si no hay en deltas', () => {
            const base = {
                storedModel: { values: [1, 2] }
            };

            const deltas = {
                model: { values: [3, 4] }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.storedModel).toEqual({ values: [1, 2] });
        });

        it('debería manejar specificAttributes de deltas', () => {
            const base = {
                attributes: { label: 'Test' }
            };

            const deltas = {
                specificAttributes: { customProp: 'value' }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.specificAttributes).toEqual({ customProp: 'value' });
        });

        it('debería mergear componente completo correctamente', () => {
            const base = {
                address: { view: 'base', component: 'comp1' },
                attributes: {
                    label: 'Original',
                    visible: true,
                    readonly: false,
                    columnModel: [{ name: 'col1' }]
                },
                validationRules: {
                    required: true
                },
                dependencies: [{ id: 'dep1' }],
                actions: [{ type: 'action1' }]
            };

            const deltas = {
                attributes: {
                    visible: false
                },
                model: {
                    values: [{ id: 1 }, { id: 2 }]
                }
            };

            const result = mergeComponentState(base, deltas);

            expect(result.address).toEqual(base.address);
            expect(result.attributes.label).toBe('Original');
            expect(result.attributes.visible).toBe(false);
            expect(result.attributes.columnModel).toEqual([{ name: 'col1' }]);
            expect(result.model).toEqual(deltas.model);
            expect(result.dependencies).toEqual(base.dependencies);
            expect(result.actions).toEqual(base.actions);
        });
    });

    describe('calculateDeltas', () => {
        it('debería retornar objeto vacío si no hay cambios', () => {
            const base = {
                attributes: { label: 'Test' }
            };

            const current = {
                attributes: { label: 'Test' }
            };

            const result = calculateDeltas(base, current);

            expect(result).toEqual({});
        });

        it('debería detectar cambios en attributes', () => {
            const base = {
                attributes: { label: 'Original', visible: true }
            };

            const current = {
                attributes: { label: 'Original', visible: false }
            };

            const result = calculateDeltas(base, current);

            expect(result.attributes).toBeDefined();
            expect(result.attributes.visible).toBe(false);
        });

        it('debería incluir solo propiedades cambiadas en attributes', () => {
            const base = {
                attributes: { label: 'Original', visible: true, readonly: false }
            };

            const current = {
                attributes: { label: 'Original', visible: false, readonly: false }
            };

            const result = calculateDeltas(base, current);

            expect(result.attributes).toEqual({ visible: false });
            expect(result.attributes.label).toBeUndefined();
            expect(result.attributes.readonly).toBeUndefined();
        });

        it('debería no incluir model si es idéntico al base', () => {
            const base = {
                model: { values: [1, 2, 3] }
            };

            const current = {
                model: { values: [1, 2, 3] }
            };

            const result = calculateDeltas(base, current);

            expect(result.model).toBeUndefined();
        });

        it('debería detectar cambios en validationRules', () => {
            const base = {
                validationRules: { required: true }
            };

            const current = {
                validationRules: { required: false }
            };

            const result = calculateDeltas(base, current);

            expect(result.validationRules).toEqual({ required: false });
        });

        it('debería incluir defaultModel si existe', () => {
            const result = calculateDeltas({}, {defaultModel: { values: [1] }});

            expect(result.defaultModel).toEqual({ values: [1] });
        });

        it('debería incluir storedModel si existe', () => {
            const base = {};

            const current = {
                storedModel: { values: [1, 2] }
            };

            const result = calculateDeltas(base, current);

            expect(result.storedModel).toEqual({ values: [1, 2] });
        });

        it('debería incluir storedAttributes si existe', () => {
            const base = {};

            const current = {
                storedAttributes: { label: 'Stored' }
            };

            const result = calculateDeltas(base, current);

            expect(result.storedAttributes).toEqual({ label: 'Stored' });
        });

        it('debería incluir specificAttributes si existe', () => {
            const base = {};

            const current = {
                specificAttributes: { custom: 'value' }
            };

            const result = calculateDeltas(base, current);

            expect(result.specificAttributes).toEqual({ custom: 'value' });
        });

        it('debería calcular deltas completos correctamente', () => {
            const base = {
                attributes: { label: 'Original', visible: true },
                validationRules: { required: true }
            };

            const current = {
                attributes: { label: 'Original', visible: false, disabled: true },
                model: { values: [1, 2, 3] },
                validationRules: { required: true },
                storedModel: { values: [] }
            };

            const result = calculateDeltas(base, current);

            expect(result.attributes).toEqual({ visible: false, disabled: true });
            expect(result.model).toEqual({ values: [1, 2, 3] });
            expect(result.validationRules).toBeUndefined(); // No cambió
            expect(result.storedModel).toEqual({ values: [] });
        });
    });
});
