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
    setLogText(prevLogText => (isEmpty(prevLogText.trim()) ? " " : prevLogText + "\n") + newLines.join("\n"));
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