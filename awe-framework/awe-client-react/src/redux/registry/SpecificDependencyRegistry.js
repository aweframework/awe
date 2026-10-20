/**
 * SpecificDependencyRegistry - hook for the `specific` dependency target.
 *
 * AngularJS launches the `applyDependency` api method of the target component for the targets it does not know
 * (`specific`). A React component type (the `component` attribute of the screen contract) can register a handler
 * here to receive those dependencies. A handler is called as `handler(dependency, value, launch)` and may return an
 * action to dispatch (or nothing). A type without handler ignores the dependency, as AngularJS does.
 */
class SpecificDependencyRegistry {
  constructor() {
    this.handlers = {};
  }

  /**
   * Register the handler of a component type
   * @param {string} componentType Component type
   * @param {function} handler Handler (dependency, value, launch) => action|null
   */
  register(componentType, handler) {
    this.handlers[componentType] = handler;
  }

  unregister(componentType) {
    delete this.handlers[componentType];
  }

  get(componentType) {
    return this.handlers[componentType];
  }

  clear() {
    this.handlers = {};
  }
}

export default new SpecificDependencyRegistry();
