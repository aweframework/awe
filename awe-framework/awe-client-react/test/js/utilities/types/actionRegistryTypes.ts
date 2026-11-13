// Type-level assertions for utilities/actionRegistry, checked by `npm run typecheck` (not run by Jest)
import type {ClientAction} from "../../../../src/contract";
import {buildActionRegistry} from "../../../../src/utilities/actionRegistry";

const reload = (action: ClientAction): string => action.type ?? "";
const services = {screen: {getActions: () => ({reload})}};

// The registry is a table of handlers by action name
const registry: Record<string, (action: ClientAction) => unknown> = buildActionRegistry(services);
registry.reload({type: "reload"});

// @ts-expect-error a service must expose getActions
buildActionRegistry({screen: {actions: {}}});
// @ts-expect-error the services are an object by owner name, not an array
buildActionRegistry([{getActions: () => ({})}]);
// @ts-expect-error an action handler must be a function
buildActionRegistry({screen: {getActions: () => ({reload: "reload"})}});
