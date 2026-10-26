import {isEmpty} from "../../utilities/general";
import {fetchAction, fetchHtml, getContextPath, getCookie, getRestUrl} from "../../utilities";
import {getFormValues} from "../selectors/form";
import type {AppThunk} from "../types";
import type {ClientAction} from "./types";

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
    setLogText(prevLogText => (isEmpty((prevLogText || "").trim()) ? " " : prevLogText + "\n") + newLines.join("\n"));
    setOffset(prev => prev + newLines.length);
  }
}

/**
 * Call for log delta
 */
export function fetchLogAction (
  serverAction: string,
  targetAction: string,
  offset: number = 0,
  setLogText: SetText,
  setOffset: SetOffset
): AppThunk {
  return (dispatch, getState) => {
    const {settings} = getState();
    fetchAction(serverAction, targetAction, {...getFormValues(getState()), offset}, settings.token)
      .then((a) => updateLog(a as ClientAction[], setLogText, setOffset)) // An error answer has no actions: it fails and is logged below
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
