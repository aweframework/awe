import React from "react";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {Dialog} from "primereact/dialog";
import {Button} from "primereact/button";
import {updateSettings} from "../redux/actions/settings";
import {getIconCode, getContextPath, getIconNode, translateLabel} from "../utilities";

const BUTTON_ICON = "p-button-icon p-c p-button-icon-right";

/**
 * Root of the deployment the application restarts from: the client addresses every request of its own with the
 * context path, so a WAR at /awe restarts at /awe/ and lands on the start screen (#935, S5)
 * @returns {string} URL of the application root
 */
const startScreenUrl = () => `${getContextPath()}/`;

/**
 * Session expired container (#935, S5): a blocking panel, with no other control than the button that restarts the
 * application and lands on its start screen. The session is gone, so there is no close and no cancel: neither a
 * click on the backdrop nor the Escape key dismisses it
 * @param {function} props.navigate Where the application goes (injected by tests; the browser navigates by default)
 * @category Containers
 */
function SessionExpiredContainer({navigate = (url) => window.location.assign(url)}) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const sessionExpired = useSelector((state) => state.settings.sessionExpired);

  // Restarting reloads the application and opens its start screen
  const onRestart = () => {
    dispatch(updateSettings({sessionExpired: false}));
    navigate(startScreenUrl());
  };

  return <Dialog
    visible={!!sessionExpired}
    header={translateLabel("ERROR_TITLE_SESSION_EXPIRED", t)}
    modal={true}
    closable={false}
    closeOnEscape={false}
    focusOnShow={false}
    blockScroll={true}
    className="awe-confirm-dialog awe-session-expired-dialog"
    aria-label={translateLabel("ERROR_TITLE_SESSION_EXPIRED", t)}
    footer={
      <Button
        id="session-expired-restart"
        label={translateLabel("BUTTON_RESTART_APPLICATION", t)}
        icon={getIconCode("pi-refresh", BUTTON_ICON)}
        onClick={onRestart}
        severity="warning"
      />
    }>
    {getIconNode("pi-exclamation-triangle", {className: "confirm-icon"})}
    <span>{translateLabel("ERROR_MESSAGE_SESSION_EXPIRED", t)}</span>
  </Dialog>;
}

export default SessionExpiredContainer;
