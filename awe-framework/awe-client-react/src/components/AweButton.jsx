import React, {useCallback} from "react";
import {Button} from "primereact/button";
import {ButtonTypes} from "../redux/actions/components";
import {getHelpTooltipNode, getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {addActionsTop} from "../redux/actions/actions";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import PropTypes from "prop-types";
import {Skeleton} from "primereact/skeleton";

const {BUTTON_RESET, BUTTON_SUBMIT, BUTTON_NORMAL} = ButtonTypes;

const getButtonType = (buttonType) => {
  switch (buttonType) {
    case BUTTON_SUBMIT:
      return "";
    case BUTTON_RESET:
      return "p-button-secondary";
    case BUTTON_NORMAL:
    default:
      return "p-button-outlined";
  }
};

function AweButton(props) {
  const { id, style: propsStyle } = props;
  const { address = {}, attributes = {}, actions = [], settings, globalDisabled } = useSelector(state => ({
    address: state.components[id]?.address,
    attributes: state.components[id]?.attributes,
    actions: state.components[id]?.actions,
    settings: state.settings,
    globalDisabled: state.actions.running
  }));
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onClick = useCallback(() => {
    // Change click event
    dispatch(updateThunk(address, {event: "click"}));

    // Send actions to action container
    if (actions.length > 0) {
      dispatch(addActionsTop(actions.map(action => ({...action, address}))));
    } else if (BUTTON_RESET === attributes.buttonType) {
      dispatch(addActionsTop([{type: "restore", address}]));
    }
  }, [actions, address, attributes?.buttonType, dispatch]);

  // If address is undefined, return skeleton
  if (!address) {
    return <Skeleton width="10rem" height="2rem" className={propsStyle}/>;
  }

  const {style, icon, disabled, label, size, visible, help, helpImage, buttonType} = attributes;
  const classes = classNames(`help-button-${id}`, "mr-2", "mt-2", getButtonType(buttonType), {"hidden": !visible, [`p-button-${size}`]: size, [`text-${size}`]: size}, style);
  return (<>
    { getHelpTooltipNode(help, helpImage, t, `.help-button-${id}`) }
    <Button
      id={id}
      type="button"
      className={classes}
      icon={getIconCode(icon, classNames("p-button-icon", "p-c",  "p-button-icon-left", {[`text-${size}`]: size}), )}
      disabled={globalDisabled || disabled}
      label={translateLabel(label, t)}
      iconPos={"left"}
      onClick={onClick}
      data-pr-position={"bottom"}
      data-pr-showdelay={settings.helpTimeout}
    />
  </>);
}

AweButton.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
};

export default AweButton;
