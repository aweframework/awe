import type { ComponentAddress } from "../../contract";
import type { ActionItem } from "../actions/actions";
import { addActionsTop } from "../actions/actions";
import type { AppDispatch } from "../types";
import { isFetchError, refusalActions } from "../../utilities";

/** What a non-ok answer of a client request ran (#935) */
export type RefusalOutcome = "actions" | "refusal" | null;

/**
 * Run the client actions a refused or failed answer of a client request demands, from the single rule the status
 * decides (#935, S3, S5, S6, S7): a 401 opens the session-expired panel, a 403 whose JSON body carries client actions
 * is processed exactly like a 200 answer, any other 403 shows its access-denied message and a 404 its not-found
 * message. Every caller of a client request shares this rule.
 * @param error Answer of a request that failed (a `FetchError` when the answer was not ok)
 * @param dispatch Dispatch of the store
 * @param target Component that was loading while the request ran: the refusal ends its loading state with an
 *   `end-load` (optional; the caller that ends it itself passes nothing)
 * @param withCallerContext Wraps every action with the context of the caller (its address, its origin), when the
 *   caller applies one (optional)
 * @returns What ran: `"actions"` when a 403 carried client actions (the caller accepts its action and keeps the
 *   success result, as it does with a 200 answer), `"refusal"` for the other refused statuses, null for any other
 *   answer (the caller keeps its own failure path)
 * @memberOf Thunks
 */
export const runRefusal = (
  error: unknown,
  dispatch: AppDispatch,
  target?: ComponentAddress | null,
  withCallerContext?: (action: ActionItem) => ActionItem
): RefusalOutcome => {
  if (!isFetchError(error)) {
    return null;
  }
  const run = (action: ActionItem): ActionItem => (withCallerContext ? withCallerContext(action) : action);

  // A 403 whose JSON body is a list of client actions is processed exactly like a 200 answer (#935, S3)
  if (error.status === 403 && Array.isArray(error.body)) {
    dispatch(addActionsTop((error.body as ActionItem[]).map(run)));
    return "actions";
  }
  const refusal = refusalActions(error, target);
  if (refusal) {
    dispatch(addActionsTop(refusal.map(run)));
    return "refusal";
  }
  return null;
};
