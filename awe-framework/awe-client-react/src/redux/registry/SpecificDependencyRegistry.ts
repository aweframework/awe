import type {Dependency} from "../../contract";

/** Handler of the `specific` dependencies of a component type: returns the action to dispatch, if any */
export type SpecificDependencyHandler = (
  dependency: Dependency, value: unknown, launch: boolean
) => Record<string, unknown> | null | undefined;

/**
 * SpecificDependencyRegistry - hook for the `specific` dependency target.
 *
 * AngularJS launches the `applyDependency` api method of the target component for the targets it does not know
 * (`specific`). A React component type (the `component` attribute of the screen contract) can register a handler
 * here to receive those dependencies. A handler is called as `handler(dependency, value, launch)` and may return an
 * action to dispatch (or nothing). A type without handler ignores the dependency, as AngularJS does.
 */
class SpecificDependencyRegistry {
  handlers: Record<string, SpecificDependencyHandler> = {};

  /**
   * Register the handler of a component type
   * @param {string} componentType Component type
   * @param {function} handler Handler (dependency, value, launch) => action|null
   */
  register(componentType: string, handler: SpecificDependencyHandler): void {
    this.handlers[componentType] = handler;
  }

  unregister(componentType: string): void {
    delete this.handlers[componentType];
  }

  get(componentType: string): SpecificDependencyHandler | undefined {
    return this.handlers[componentType];
  }

  clear(): void {
    this.handlers = {};
  }
}

export default new SpecificDependencyRegistry();
