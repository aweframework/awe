import ComponentRegistry from '../../../../src/redux/registry/ComponentRegistry';

describe('awe-react-client/test/js/redux/registry/ComponentRegistryTest.js', () => {
    let mockComponent1;
    let mockComponent2;

    beforeEach(() => {
        // Limpiar registry antes de cada test
        ComponentRegistry.clearAll();

        mockComponent1 = {
            address: { view: 'base', component: 'comp1' },
            attributes: {
                label: 'Component 1',
                visible: true,
                readonly: false
            },
            validationRules: {
                required: true
            },
            dependencies: [{ id: 'dep1' }],
            actions: [{ type: 'action1' }],
            storedAttributes: {},
            storedValidationRules: {}
        };

        mockComponent2 = {
            address: { view: 'screen1', component: 'comp2' },
            attributes: {
                label: 'Component 2',
                visible: false
            },
            validationRules: {},
            dependencies: [],
            actions: []
        };
    });

    afterEach(() => {
        ComponentRegistry.clearAll();
    });

    describe('register', () => {
        it('debería registrar un componente correctamente', () => {
            ComponentRegistry.register('comp1', mockComponent1);

            const retrieved = ComponentRegistry.get('comp1');
            expect(retrieved).toBeDefined();
            expect(retrieved.address).toEqual(mockComponent1.address);
            expect(retrieved.attributes).toEqual(mockComponent1.attributes);
        });

        it('debería congelar el objeto registrado', () => {
            ComponentRegistry.register('comp1', mockComponent1);

            const retrieved = ComponentRegistry.get('comp1');
            expect(Object.isFrozen(retrieved)).toBe(true);
        });

        it('debería indexar por vista', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            const stats = ComponentRegistry.getStats();
            expect(stats.componentsByView['base']).toBe(1);
            expect(stats.componentsByView['screen1']).toBe(1);
        });

        it('debería permitir múltiples componentes en la misma vista', () => {
            const comp3 = { ...mockComponent1, address: { view: 'base', component: 'comp3' } };

            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp3', comp3);

            const stats = ComponentRegistry.getStats();
            expect(stats.componentsByView['base']).toBe(2);
        });

        it('debería avisar cuando se registra un componente sin address', () => {
            const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});

            ComponentRegistry.register('broken', {
                uid: 'broken-uid',
                attributes: { id: 'broken' },
                context: { view: 'screen1' }
            });

            expect(warnSpy).toHaveBeenCalledWith(
                expect.stringContaining('[AWE] Malformed component detected in registry:registerComponent'),
                expect.objectContaining({
                    componentKey: 'broken',
                    componentUid: 'broken-uid',
                    attributesId: 'broken',
                    view: 'screen1',
                    registryComponentId: 'broken'
                })
            );
        });
    });

    describe('get', () => {
        it('debería retornar el componente registrado', () => {
            ComponentRegistry.register('comp1', mockComponent1);

            const retrieved = ComponentRegistry.get('comp1');
            expect(retrieved).toBeDefined();
            expect(retrieved.attributes.label).toBe('Component 1');
        });

        it('debería retornar null para componente no registrado', () => {
            const retrieved = ComponentRegistry.get('nonexistent');
            expect(retrieved).toBeNull();
        });

        it('debería retornar null después de limpiar', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.clearAll();

            const retrieved = ComponentRegistry.get('comp1');
            expect(retrieved).toBeNull();
        });
    });

    describe('getAllIds', () => {
        it('debería retornar array vacío si no hay componentes', () => {
            const ids = ComponentRegistry.getAllIds();
            expect(ids).toEqual([]);
        });

        it('debería retornar todos los IDs registrados', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            const ids = ComponentRegistry.getAllIds();
            expect(ids).toContain('comp1');
            expect(ids).toContain('comp2');
            expect(ids.length).toBe(2);
        });

        it('debería actualizar después de registrar nuevos componentes', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            expect(ComponentRegistry.getAllIds().length).toBe(1);

            ComponentRegistry.register('comp2', mockComponent2);
            expect(ComponentRegistry.getAllIds().length).toBe(2);
        });
    });

    describe('clear', () => {
        it('debería limpiar componentes de una vista específica', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            ComponentRegistry.clear('base');

            expect(ComponentRegistry.get('comp1')).toBeNull();
            expect(ComponentRegistry.get('comp2')).toBeDefined();
        });

        it('debería actualizar el índice de vistas', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            ComponentRegistry.clear('base');

            const stats = ComponentRegistry.getStats();
            expect(stats.componentsByView['base']).toBeUndefined();
            expect(stats.componentsByView['screen1']).toBe(1);
        });

        it('no debería afectar otras vistas', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            ComponentRegistry.clear('screen1');

            expect(ComponentRegistry.get('comp1')).toBeDefined();
            expect(ComponentRegistry.get('comp2')).toBeNull();
        });

        it('debería limpiar huérfanos sin address usando context.view como fallback', () => {
            ComponentRegistry.register('orphan', {
                uid: 'orphan-uid',
                attributes: { id: 'orphan' },
                context: { view: 'base' }
            });
            ComponentRegistry.register('comp2', mockComponent2);

            ComponentRegistry.clear('base');

            expect(ComponentRegistry.get('orphan')).toBeNull();
            expect(ComponentRegistry.get('comp2')).toBeDefined();
        });

        it('debería manejar vista inexistente sin errores', () => {
            ComponentRegistry.register('comp1', mockComponent1);

            expect(() => ComponentRegistry.clear('nonexistent')).not.toThrow();
            expect(ComponentRegistry.get('comp1')).toBeDefined();
        });
    });

    describe('clearAll', () => {
        it('debería limpiar todos los componentes', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            ComponentRegistry.clearAll();

            expect(ComponentRegistry.get('comp1')).toBeNull();
            expect(ComponentRegistry.get('comp2')).toBeNull();
            expect(ComponentRegistry.getAllIds()).toEqual([]);
        });

        it('debería limpiar el índice de vistas', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            ComponentRegistry.clearAll();

            const stats = ComponentRegistry.getStats();
            expect(stats.totalComponents).toBe(0);
            expect(stats.views).toBe(0);
        });
    });

    describe('getStats', () => {
        it('debería retornar estadísticas correctas', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            ComponentRegistry.register('comp2', mockComponent2);

            const stats = ComponentRegistry.getStats();

            expect(stats.totalComponents).toBe(2);
            expect(stats.views).toBe(2);
            expect(stats.componentsByView).toEqual({
                'base': 1,
                'screen1': 1
            });
        });

        it('debería retornar estadísticas vacías si no hay componentes', () => {
            const stats = ComponentRegistry.getStats();

            expect(stats.totalComponents).toBe(0);
            expect(stats.views).toBe(0);
            expect(stats.componentsByView).toEqual({});
        });

        it('debería actualizar después de cambios', () => {
            ComponentRegistry.register('comp1', mockComponent1);
            let stats = ComponentRegistry.getStats();
            expect(stats.totalComponents).toBe(1);

            ComponentRegistry.register('comp2', mockComponent2);
            stats = ComponentRegistry.getStats();
            expect(stats.totalComponents).toBe(2);

            ComponentRegistry.clear('base');
            stats = ComponentRegistry.getStats();
            expect(stats.totalComponents).toBe(1);
        });
    });

    describe('singleton behavior', () => {
        it('debería mantener el estado entre importaciones', () => {
            ComponentRegistry.register('comp1', mockComponent1);

            // Simular otra importación
            const ComponentRegistry2 = require('../../../../src/redux/registry/ComponentRegistry').default;

            expect(ComponentRegistry2.get('comp1')).toBeDefined();
            expect(ComponentRegistry2.get('comp1').attributes.label).toBe('Component 1');
        });
    });
});
