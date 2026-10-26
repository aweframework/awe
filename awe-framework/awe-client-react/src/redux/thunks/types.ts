import type {ComponentAddress} from "../../contract";
import type {ComponentState} from "../../utilities/mergeUtils";
import type {ActionItem} from "../actions/actions";

/**
 * An action of the client as the thunks receive it: the type, the component it works on and its parameters. The
 * parameters depend on the type of the action, so each thunk narrows them to what it reads
 */
export interface ClientAction extends ActionItem {
  type: string;
  address?: ComponentAddress;
  target?: string;
  view?: string;
  silent?: boolean;
  parameters?: Record<string, unknown>;
}

/**
 * The components of the state as the thunks that work on a context read them: every component of the screen has the
 * structure path of its context (reading it fails otherwise, as it always did)
 */
export type ContextComponents = Record<string, ComponentState & {context: {view?: string; source: string[]}}>;

/** A function that translates a label */
export type Translate = (label: string) => string;
