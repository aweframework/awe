import SpecificDependencyRegistry from '../../../../src/redux/registry/SpecificDependencyRegistry';

describe('awe-react-client/test/js/redux/registry/SpecificDependencyRegistryTest.js', () => {
  afterEach(() => SpecificDependencyRegistry.clear());

  it('returns the handler registered for a component type', () => {
    const handler = jest.fn();
    SpecificDependencyRegistry.register('grid', handler);
    expect(SpecificDependencyRegistry.get('grid')).toBe(handler);
  });

  it('returns undefined when the component type has no handler', () => {
    expect(SpecificDependencyRegistry.get('text')).toBeUndefined();
  });

  it('unregisters a handler', () => {
    SpecificDependencyRegistry.register('grid', jest.fn());
    SpecificDependencyRegistry.unregister('grid');
    expect(SpecificDependencyRegistry.get('grid')).toBeUndefined();
  });
});
