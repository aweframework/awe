import {isEmpty} from "../../utilities/general";
import {fetchAction, fetchHtml, getContextPath, getCookie, getRestUrl} from "../../utilities";
import {getFormValues} from "../selectors/form";
import type {AppThunk} from "../types";
import type {ClientAction} from "./types";
import {runRefusal} from "./refusal";
import {addActionsTop} from "../actions/actions";
import type {ActionItem} from "../actions/actions";
import type {ComponentAddress} from "../../contract";

/** Setter of a state that holds a text: it receives a function from the previous text to the next one */
type SetText = (update: (previous: string) => string) => void;

/** Setter of a state that holds a number */
type SetOffset = (update: (previous: number) => number) => void;

/**
 * Update log data from actions
 * @param {object[]} actions Action list
 * @param setLogText Update log text
 * @param setOffset Define offset
 */
function updateLog (actions: ClientAction[] = [], setLogText: SetText, setOffset: SetOffset): void {
  const newLines = actions
    .filter(a => a.type === "log-delta")
    .map(a => (a.parameters as { log: string | string[] }).log)
    .flat();

  if (newLines.length !== 0) {
    // A blank text (nothing read yet) is not a line: the first lines start the log, with nothing before them
    setLogText(prevLogText => (isEmpty((prevLogText || "").trim()) ? "" : prevLogText + "\n") + newLines.join("\n"));
    setOffset(prev => prev + newLines.length);
  }
}

/**
 * Call for log delta. The lines of the answer go to the viewer; the other actions of the answer run on the viewer, as
 * AngularJS runs every action of an answer (the server turns the autorefresh of the viewer off when the execution of
 * the log has ended)
 * @param {string} serverAction Server action
 * @param {string} targetAction Target action
 * @param {number} offset Line to read from
 * @param setLogText Update log text
 * @param setOffset Define offset
 * @param {object} [address] Address of the viewer, where the other actions of the answer run
 */
export function fetchLogAction (
  serverAction: string,
  targetAction: string,
  offset: number = 0,
  setLogText: SetText,
  setOffset: SetOffset,
  address?: ComponentAddress
): AppThunk<Promise<void>> {
  // The read ends when its answer is applied: the viewer starts no other read meanwhile
  return (dispatch, getState) => {
    const {settings} = getState();
    return fetchAction(serverAction, targetAction, {...getFormValues(getState()), offset}, settings.token)
      .then((a) => {
        // The refusal of the server (an expired session, denied access) answers with its own actions (the panel on
        // a 401) instead of the log (#935, S6); any other answer that failed keeps failing here, as it always did
        if (runRefusal(a, dispatch)) {
          return;
        }
        const actions = a as ClientAction[];
        updateLog(actions, setLogText, setOffset);
        const others = actions.filter(action => action?.type !== "log-delta");
        if (others.length > 0) {
          dispatch(addActionsTop(others.map(action => ({...(address ? {address} : {}), ...action})) as ActionItem[]));
        }
      })
      .catch((reason) => console.error("Error reading log file:", reason));
  };
}

export function fetchPdfAction (targetAction: string, setPdf: (url: string) => void): AppThunk {
  return (dispatch, getState) => {
    const {settings} = getState();
    fetch(`${getContextPath()}${getRestUrl("file", "stream", "maintain", targetAction)}`, {
      method: 'POST',
      body: JSON.stringify(getFormValues(getState())),
      headers: {
        "Accept": "*/*",
        "Authorization": settings.token,
        "Content-Type": "application/json",
        "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
      } as Record<string, string> // A missing token or cookie is sent as it is
    }).then(response => response.blob())
      .then(blob => setPdf(window.URL.createObjectURL(blob)))
      .catch((reason) => console.error("Error reading pdf file:", reason));
  };
}

export function fetchHelpAction(setHelp: (help: string) => void, option: string | null = null): AppThunk {
  return (dispatch, getState) => {
    const { settings } = getState();
    fetchHtml(getRestUrl("template", "help", option === "application-help" ? null : option), settings.token)
      .then(setHelp)
      .catch((reason) => console.error("Error reading help file:", reason));
  };
}

/**
 * Read the content of a file through a generic file endpoint (the answer is plain text with status 201)
 * @param {string} serverAction Server action (endpoint)
 * @param {string} targetAction Target action
 * @param {function} setContent Receives the content when the server answers one
 * @returns {function} Thunk that returns the request promise (always resolved)
 */
export function fetchFileContentAction(
  serverAction: string,
  targetAction: string,
  setContent: (content: string) => void
): AppThunk<Promise<false | void>> {
  return (dispatch, getState) => {
    const {settings} = getState();
    return fetch(`${getContextPath()}${getRestUrl(serverAction, targetAction)}`, {
      method: 'POST',
      body: JSON.stringify(getFormValues(getState())),
      headers: {
        "Accept": "*/*",
        "Authorization": settings.token,
        "Content-Type": "application/json;charset=UTF-8",
        "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
      } as Record<string, string> // A missing token or cookie is sent as it is
    }).then(response => response.status === 201 ? response.text() : null)
      .then(content => !isEmpty(content) && setContent(content as string))
      // eslint-disable-next-line no-console
      .catch((reason) => console.error("Error reading file:", reason));
  };
}
