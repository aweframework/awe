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
import { Components } from "../utilities/structure";
import { IGNORE_COMPONENT_LIST } from "./AweComponents";
import { getButtonVariant, hasButtonStyle, isIconOnly } from "../utilities/buttons";
import "./AweButton.less";

const CONTEXT_MENU_ENTRIES = ["ContextButton", "ContextSeparator"];

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

/**
 * Classes of a PrimeReact button: its look comes from the btn-* styles of the screen, as in the AngularJS engine, and
 * without them from its type
 */
const getButtonClasses = ({ id, className, variant, signin, style, buttonType, visible, size }) => {
  const buttonVariant = signin?.variant ?? variant ?? getButtonVariant(style, getButtonType(buttonType));
  // A normal button without a btn-* style is the default button of the application: its look comes from --awe-button-default-*
  const defaultLook = !signin && !variant && buttonType !== BUTTON_SUBMIT && buttonType !== BUTTON_RESET && !hasButtonStyle(style);
  return classNames(`help-button-${id}`, className ?? "awe-button-spaced", buttonVariant,
    { "hidden": !visible, "awe-button-default": defaultLook, [`p-button-${size}`]: size }, style, signin?.className);
};

/**
 * Tooltip of a button: its title, or the label of an icon-only button without a help
 */
const getTooltip = ({ title, iconOnly, help, helpImage, text, t }) => {
  if (title) {
    return translateLabel(title, t);
  }
  return iconOnly && !help && !helpImage ? text : "";
};

/**
 * Child elements a button shows: its actions, dependencies and context menu entries are not content
 * @param {object[]} elementList Child elements of the button
 * @returns {object[]} Elements to draw inside the button
 */
const getButtonContent = (elementList) => (elementList || []).filter(node =>
  !IGNORE_COMPONENT_LIST.includes(node.elementType) && !CONTEXT_MENU_ENTRIES.includes(node.elementType));

/**
 * A button with child elements (a notification row: title, description, time...) is a clickable element of its type
 * that holds them, as in the AngularJS engine, not a PrimeReact button that would drop them
 */
function ButtonContent({ id, type, style, visible, disabled, onClick, content }) {
  const onKeyDown = event => {
    if (disabled || (event.key !== "Enter" && event.key !== " ")) {
      return;
    }
    // Space would scroll the page
    event.preventDefault();
    onClick(event);
  };
  return React.createElement(type && type !== "button" ? type : "div", {
    id, className: classNames(`help-button-${id}`, "awe-button-content", { "hidden": !visible, "disabled": disabled }, style),
    role: "button", tabIndex: disabled ? -1 : 0, "aria-disabled": disabled ? "true" : undefined,
    onClick: disabled ? undefined : onClick, onKeyDown, ...testHook(TestIds.button)
  }, content.map((node, index) => Components(node, index)));
}

ButtonContent.propTypes = {
  id: PropTypes.string,
  type: PropTypes.string,
  style: PropTypes.string,
  visible: PropTypes.bool,
  disabled: PropTypes.bool,
  onClick: PropTypes.func,
  content: PropTypes.array
};

function AweButton(props) {
  // className and variant replace the margins and the look of the button where it is placed (the selection bar of a grid)
  const { id, style: propsStyle, className, variant, type, elementList } = props;
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

  const { style, icon, disabled, label, size, visible, help, helpImage, buttonType, showLoading, loadingLabel, busyThreshold, title } = attributes;
  const content = getButtonContent(elementList);
  if (content.length > 0) {
    return <ButtonContent id={id} type={type} style={style} visible={visible} disabled={disabled} onClick={onClick}
                          content={content} />;
  }
  const classes = getButtonClasses({ id, className, variant, signin, style, buttonType, visible, size });
  // An icon-only button (style no-label) shows its label as its tooltip (unless it has a help) and accessible name
  const iconOnly = !signin && isIconOnly(style);
  const text = translateLabel(label, t);
  const tooltip = getTooltip({ title, iconOnly, help, helpImage, text, t });
  return (<>
    {getHelpTooltipNode(help, helpImage, t, `.help-button-${id}`)}
    <ActionsRunningButton
      ref={buttonRef}
      id={id}
      type="button"
      className={classes}
      icon={signin?.icon ?? getIconCode(icon || signin?.defaultIcon, classNames("p-button-icon", "p-c", "p-button-icon-left"))}
      disabled={disabled || signin?.disabled}
      // Busy state unless show-loading="false"; the sign-in page draws its own
      showLoading={!signin && showLoading !== false}
      busyThreshold={showLoading === false ? undefined : busyThreshold}
      busyLabel={loadingLabel ? translateLabel(loadingLabel, t) : null}
      label={signin?.label ?? (iconOnly ? null : text)}
      {...(iconOnly && text ? { "aria-label": text } : {})}
      {...(tooltip ? { title: tooltip } : {})}
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
  type: PropTypes.string,
  elementList: PropTypes.array,
  className: PropTypes.string,
  variant: PropTypes.string,
};

export default AweButton;
