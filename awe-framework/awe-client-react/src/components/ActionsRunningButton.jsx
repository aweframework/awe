import React, { useCallback } from "react";
import { Button } from "primereact/button";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { selectActionsRunning } from "../redux/selectors/stateSelectors";
import { getBusyIcon, useLaunchBusy } from "../hooks/useBusy";
import { classNames } from "../utilities/components";

/**
 * Button disabled while actions run. It is the only part of a button that reads the running flag, so a running/idle
 * transition renders this button again but not the component that holds it. The ref reaches the PrimeReact button.
 * With showLoading, the button whose click launched the actions shows the busy state (the spinner instead of its icon)
 * when they still run the threshold after the click (#771)
 * @category Components
 */
const ActionsRunningButton = React.forwardRef(function ActionsRunningButton(
  { disabled, showLoading, onClick, className, icon, ...props }, ref) {
  const running = useSelector(selectActionsRunning);
  const [launched, launch] = useLaunchBusy(running);
  const busy = showLoading && launched;
  const click = useCallback(event => {
    if (showLoading) {
      launch();
    }
    onClick?.(event);
  }, [showLoading, launch, onClick]);
  return <Button {...props} ref={ref} onClick={click} disabled={running || disabled}
    className={classNames(className, { "is-loading": busy })} icon={busy ? getBusyIcon() : icon}
    aria-busy={busy ? "true" : undefined} />;
});

ActionsRunningButton.propTypes = {
  disabled: PropTypes.bool,
  showLoading: PropTypes.bool,
  onClick: PropTypes.func,
  className: PropTypes.string,
  icon: PropTypes.node
};

export default ActionsRunningButton;
