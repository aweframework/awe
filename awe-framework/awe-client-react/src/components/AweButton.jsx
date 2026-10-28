import React, { useCallback } from "react";
import ActionsRunningButton from "./ActionsRunningButton";
import { ButtonTypes } from "../redux/actions/components";
import { getHelpTooltipNode, getIconCode, translateLabel } from "../utilities";
import { classNames } from "../utilities/components";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useTranslation } from "react-i18next";
import { addActionsTop } from "../redux/actions/actions";
import { updateModelWithDependencies as updateThunk } from "../redux/thunks/components";
import PropTypes from "prop-types";
import { ButtonSkeleton } from "./AweSkeleton";
import { TestIds, testHook } from "../utilities/testIds";
import { useContextMenu, useContextMenuAnchor } from "../hooks/useContextMenu";
import { useSigninButton } from "./SigninPage";

const { BUTTON_RESET, BUTTON_SUBMIT, BUTTON_NORMAL } = ButtonTypes;

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
  const state = useComponentState(id);
  const { address = {}, attributes = {}, actions = [] } = state;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { onContextMenu, contextMenuTemplate } = useContextMenu({ address, contextMenu: attributes.contextMenu });
  const buttonRef = useContextMenuAnchor(address);
  // On the sign-in page the login buttons are drawn as the prototype
  const signin = useSigninButton(actions, attributes, t, id);

  const onClick = useCallback(() => {
    // Change click event
    dispatch(updateThunk(address, { event: "click" }));

    // Send actions to action container
    if (actions.length > 0) {
      dispatch(addActionsTop(actions.map(action => ({ ...action, address }))));
    } else if (BUTTON_RESET === attributes.buttonType) {
      dispatch(addActionsTop([{ type: "restore", address }]));
    }
  }, [actions, address, attributes?.buttonType, dispatch]);

  // A button that has no state at all is waiting to register: its skeleton takes its place
  if (Object.keys(state).length === 0) {
    return <ButtonSkeleton attributes={attributes} className={propsStyle} />;
  }

  const { style, icon, disabled, label, size, visible, help, helpImage, buttonType, showLoading } = attributes;
  const classes = classNames(`help-button-${id}`, "mr-2", "mt-2", signin?.variant ?? getButtonType(buttonType), { "hidden": !visible, [`p-button-${size}`]: size, [`text-${size}`]: size }, style, signin?.className);
  return (<>
    {getHelpTooltipNode(help, helpImage, t, `.help-button-${id}`)}
    <ActionsRunningButton
      ref={buttonRef}
      id={id}
      type="button"
      className={classes}
      icon={signin?.icon ?? getIconCode(icon || signin?.defaultIcon, classNames("p-button-icon", "p-c", "p-button-icon-left", { [`text-${size}`]: size }),)}
      disabled={disabled || signin?.disabled}
      // Busy state unless show-loading="false"; the sign-in page draws its own
      showLoading={!signin && showLoading !== false}
      label={signin?.label ?? translateLabel(label, t)}
      iconPos={"left"}
      onClick={onClick}
      onContextMenu={onContextMenu}
      data-pr-position={"bottom"}
      data-pr-showdelay={settings.helpTimeout}
      {...testHook(TestIds.button)}
    />
    {signin?.after}
    {contextMenuTemplate()}
  </>);
}

AweButton.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
};

export default AweButton;
