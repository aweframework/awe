// Type-level assertions for services/ClientActionsService, checked by `npm run typecheck` (not run by Jest)
import type {ClientAction} from "../../../../src/contract";
import useClientActions from "../../../../src/services/ClientActionsService";

export function useActionsTypes() {
  // The hook returns the table of client actions: handlers by action name
  const actions: Record<string, (action: ClientAction) => unknown> = useClientActions();
  const action: ClientAction = {type: "reload"};
  const result: unknown = actions.reload(action);

  // @ts-expect-error a handler needs the action
  actions.reload();
  // @ts-expect-error a handler receives a client action, not text
  actions.reload("reload");
  // @ts-expect-error the actions are handlers, not text
  const names: Record<string, string> = useClientActions();

  return {result, names};
}
