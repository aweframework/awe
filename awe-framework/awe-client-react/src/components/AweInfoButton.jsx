import React from "react";
import { Button } from "primereact/button";
import { classNames } from "../utilities/components";
import { getHelpTooltipNode, getIconCode, translateLabel } from "../utilities";
import PropTypes from "prop-types";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useTranslation } from "react-i18next";
import { updateModelWithDependencies } from "../redux/thunks/components";
import { addActionsTop } from "../redux/actions/actions";

function AweInfoButton(props) {

  const { id } = props;
  const { address = {}, attributes = {}, actions } = useComponentState(id);
  const globalDisabled = useSelector(state => state.actions.running);
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const { style, icon, disabled, label, size, visible, help, helpImage } = attributes;
  const dispatch = useDispatch();

  const onClick = () => {
    // Change click event
    dispatch(updateModelWithDependencies(address, { event: "click" }));

    // Send actions to action container
    dispatch(addActionsTop(actions.map(action => ({ ...action, address }))));
  };

  const classes = classNames(`help-info-button-${id}`, "p-button-rounded", "p-button-text", "p-button-secondary", { "hidden": !visible, [`p-button-${size}`]: size }, style);

  return (<span className="p-overlay-badge">
    {getHelpTooltipNode(help, helpImage, t, `.help-info-button-${id}`)}
    <Button
      id={id}
      type="button"
      className={classes}
      icon={getIconCode(icon, "p-button-icon p-c p-button-icon-left")}
      disabled={globalDisabled || disabled}
      label={translateLabel(label, t)}
      iconPos={"left"}
      onClick={onClick}
      data-pr-position={"bottom"}
      data-pr-showdelay={settings.helpTimeout}
    />
  </span>);
}

AweInfoButton.propTypes = {
  id: PropTypes.string
};

export default AweInfoButton;
