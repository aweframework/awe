import React from "react";
import {Button} from "primereact/button";
import {AweComponent, connectComponent} from "./AweComponent";
import {ButtonTypes} from "../redux/actions/components";
import {bindMethods, getHelpTooltipNode, getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";

const {BUTTON_RESET, BUTTON_SUBMIT, BUTTON_NORMAL} = ButtonTypes;

/**
 * Awe Button Component
 * @extends AweComponent
 * @category Components
 * @subcategory Button
 * @component
 */
class AweButton extends AweComponent {

  /**
   * Create a button
   * @param {object} props Button properties
   */
  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["onClick", "getButtonType"]);
  }

  /**
   * Manage on click event
   */
  onClick() {
    const {actions, addActionsTop, updateModelWithDependencies, updateModel, attributes, address} = this.props;

    // Change click event
    updateModelWithDependencies(address, {event: "click"});

    // Send actions to action container
    if (actions.length > 0) {
      addActionsTop(actions.map(action => ({...action, address})));
    } else if (BUTTON_RESET === attributes.buttonType) {
      addActionsTop([{type: "restore", address}]);
    }
  }

  /**
   * Retrieve button type
   * @returns {string} Button type
   */
  getButtonType() {
    const {buttonType} = this.props.attributes;
    switch (buttonType) {
      case BUTTON_SUBMIT:
        return "";
      case BUTTON_RESET:
        return "p-button-secondary";
      case BUTTON_NORMAL:
      default:
        return "p-button-outlined";
    }
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {t, attributes, address, settings} = this.props;
    const {style, icon, disabled, label, size, visible, help, helpImage} = attributes;
    const classes = classNames(`help-button-${address.component}`, "mr-2", "mt-2", this.getButtonType(), {"hidden": !visible, [`p-button-${size}`]: size, [`text-${size}`]: size}, style);

    return <>
        { getHelpTooltipNode(help, helpImage, t, `.help-button-${address.component}`) }
        <Button
        id={address.component}
        type="button"
        className={classes}
        icon={getIconCode(icon, classNames("p-button-icon", "p-c",  "p-button-icon-left", {[`text-${size}`]: size}), )}
        disabled={this.props.disabled || disabled}
        label={translateLabel(label, t)}
        iconPos={"left"}
        onClick={this.onClick}
        data-pr-position={"bottom"}
        data-pr-showdelay={settings.helpTimeout}
      />
    </>;
  }
}

export default connectComponent(AweButton);
