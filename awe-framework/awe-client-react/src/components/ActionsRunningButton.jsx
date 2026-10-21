import React from "react";
import { Button } from "primereact/button";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { selectActionsRunning } from "../redux/selectors/stateSelectors";

/**
 * Button disabled while actions run. It is the only part of a button that reads the running flag, so a running/idle
 * transition renders this button again but not the component that holds it. The ref reaches the PrimeReact button
 * @category Components
 */
const ActionsRunningButton = React.forwardRef(function ActionsRunningButton({ disabled, ...props }, ref) {
  const running = useSelector(selectActionsRunning);
  return <Button {...props} ref={ref} disabled={running || disabled} />;
});

ActionsRunningButton.propTypes = {
  disabled: PropTypes.bool
};

export default ActionsRunningButton;
