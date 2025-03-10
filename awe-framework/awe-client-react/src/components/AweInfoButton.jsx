import React from "react";
import {Button} from "primereact/button";
import {AweComponent, connectComponent} from "./AweComponent";
import {classNames} from "../utilities/components";
import {getHelpTooltipNode, getIconCode} from "../utilities";

class AweInfoButton extends AweComponent {

  constructor(props) {
    super(props);

    // Bind events
    this.onClick = this.onClick.bind(this);
  }

  onClick() {
    const {addActionsTop, updateModelWithDependencies, updateModel, actions, address} = this.props;

    // Change click event
    updateModelWithDependencies(address, {event: "click"});

    // Send actions to action container
    addActionsTop(actions.map(action => ({...action, address})));
  }

  render() {
    const {t, attributes, settings, address} = this.props;
    const {style, icon, disabled, label, size, visible, help, helpImage} = attributes;
    const classes = classNames(`help-info-button-${address.component}`, "p-button-rounded", "p-button-text", "p-button-secondary", {"hidden": !visible, [`p-button-${size}`]: size}, style);

    return <span className="p-overlay-badge">
        { getHelpTooltipNode(help, helpImage, t, `.help-info-button-${address.component}`) }
        <Button
        id={address.component}
        type="button"
        className={classes}
        icon={getIconCode(icon, "p-button-icon p-c p-button-icon-left")}
        disabled={this.props.disabled || disabled}
        label={t(label)}
        iconPos={"left"}
        onClick={this.onClick}
        data-pr-position={"bottom"}
        data-pr-showdelay={settings.helpTimeout}
        />
      </span>;
      }
      }

      export default connectComponent(AweInfoButton);
