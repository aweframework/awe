import {isEmpty} from "../../utilities/general";
import {fetchAction, fetchHtml, getContextPath, getCookie, getRestUrl} from "../../utilities";
import {getFormValues} from "../selectors/form";

/**
 * Update log data from actions
 * @param {object[]} actions Action list
 * @param setLogText Update log text
 * @param setOffset Define offset
 */
function updateLog (actions = [], setLogText, setOffset) {
  const newLines = actions
    .filter(a => a.type === "log-delta")
    .map(a => a.parameters.log)
    .flat();

  if (newLines.length !== 0) {
    setLogText(prevLogText => (isEmpty((prevLogText || "").trim()) ? " " : prevLogText + "\n") + newLines.join("\n"));
    setOffset(prev => prev + newLines.length);
  }
}

/**
 * Call for log delta
 */
export function fetchLogAction (serverAction, targetAction, offset = 0, setLogText, setOffset) {
  return (dispatch, getState) => {
    const {settings} = getState();
    fetchAction(serverAction, targetAction, {...getFormValues(getState()), offset}, settings.token)
      .then((a) => updateLog(a, setLogText, setOffset))
      .catch((reason) => console.error("Error reading log file:", reason));
  };
}

export function fetchPdfAction (targetAction, setPdf) {
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
      }
    }).then(response => response.blob())
      .then(blob => setPdf(window.URL.createObjectURL(blob)))
      .catch((reason) => console.error("Error reading pdf file:", reason));
  };
}

export function fetchHelpAction(setHelp, option = null) {
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
export function fetchFileContentAction(serverAction, targetAction, setContent) {
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
      }
    }).then(response => response.status === 201 ? response.text() : null)
      .then(content => !isEmpty(content) && setContent(content))
      // eslint-disable-next-line no-console
      .catch((reason) => console.error("Error reading file:", reason));
  };
}
