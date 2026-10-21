import React from "react";
import { Button } from "primereact/button";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { selectActionsRunning } from "../redux/selectors/stateSelectors";

/**
 * Button disabled while actions run. It is the only part of a button that reads the running flag, so a running/idle
 * transition renders this button again but not the component that holds it
 * @category Components
 */
function ActionsRunningButton({ disabled, ...props }) {
  const running = useSelector(selectActionsRunning);
  return <Button {...props} disabled={running || disabled} />;
}

ActionsRunningButton.propTypes = {
  disabled: PropTypes.bool
};

export default ActionsRunningButton;
