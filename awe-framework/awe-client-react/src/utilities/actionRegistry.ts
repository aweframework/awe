import type {ClientAction} from "../contract";

/**
 * Handler of a client action: it receives the action (and the properties the actions container runs it with, which
 * only some actions read) and what it returns is up to the action
 */
export type ClientActionHandler = (action: ClientAction, props?: unknown) => unknown;

/**
 * Service that owns client actions
 */
export interface ClientActionService {
  getActions: () => Record<string, ClientActionHandler>;
}

/**
 * Build the client action table from the services that own actions.
 * Every service is registered under an explicit name and an action name can only have one owner: registering a
 * duplicate fails, so a service can never silently override the action of another one.
 * @param {Object<string, {getActions: function(): Object}>} services Services by owner name
 * @returns {Object<string, function>} Actions by action name
 * @throws {Error} When two services register the same action name
 * @memberOf Utilities
 */
export function buildActionRegistry(services: Record<string, ClientActionService>): Record<string, ClientActionHandler> {
  const owners: Record<string, string> = {};
  const registry: Record<string, ClientActionHandler> = {};
  Object.entries(services).forEach(([owner, service]) => {
    Object.entries(service.getActions()).forEach(([name, action]) => {
      if (name in owners) {
        throw new Error(`Client action "${name}" is registered by "${owners[name]}" and "${owner}"`);
      }
      owners[name] = owner;
      registry[name] = action;
    });
  });
  return registry;
}
