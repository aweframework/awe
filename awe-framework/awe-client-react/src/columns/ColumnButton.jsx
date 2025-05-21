import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {Button} from "primereact/button";
import {ButtonTypes} from "../redux/actions/components";
import {bindMethods, getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";

const {BUTTON_RESET} = ButtonTypes;

class ColumnButton extends Component {

  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["onClick"]);
  }

  onClick() {
    const {actions, addActionsTop, updateModelWithDependencies, updateModel, buttonType, address} = this.props;
    // Change click event
    updateModelWithDependencies(address, {event: "click"});

    // Send actions to action container
    if (actions.length > 0) {
      addActionsTop(actions.map(action => ({...action, address: {...address}})));
    } else if (BUTTON_RESET === buttonType) {
      addActionsTop([{type: "restore", address: {...address}}]);
    }
  }

  render() {
    const {t, data, address, disabled, icon} = this.props;
    const {style, label} = data;
    const classes = classNames('p-button-sm', 'p-1', style);

    return <Button
      id={address.component}
      type="button"
      className={classes}
      icon={getIconCode(data.icon || icon, "p-button-icon p-c")}
      disabled={disabled}
      label={label ? translateLabel(label, t) : null}
      iconPos={"left"}
      onClick={this.onClick}
    />;
  }
}

export default withTranslation()(ColumnButton);
