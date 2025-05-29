import React from "react";
import {connectComponent} from "../components/AweComponent";
import {bindMethods, getIconCode, translateLabel} from "../utilities";
import AweCriterionComponent from "./AweCriterionComponent";
import {classNames, getVisibleTextData} from "../utilities/components";

class AweInputTextView extends AweCriterionComponent {

  constructor(props) {
    super(props);
    this.groupClass = "flex p-2";
    bindMethods(this, ["getComponent", "onClick", "onKeyDown", "onAction"]);
  }

  getIcon() {
    const {icon, size} = this.props.attributes;
    if (icon) {
      return getIconCode(icon, classNames("fa-fw", {[`text-${size}`]: size}));
    }

    return null;
  }

  getUnit() {
    const {t, attributes} = this.props;
    const {unit} = attributes;
    if (unit) {
      return <span className="p-tag ml-auto">{translateLabel(unit, t)}</span>;
    }

    return null;
  }

  onClick() {
    this.onAction("click");
  }

  onKeyDown() {
    this.onAction("keyDown")
  }

  onAction(event) {
    const {address, updateModelWithDependencies, actions, addActionsTop} = this.props;
    // Change event
    updateModelWithDependencies(address, {event});

    if (actions.length > 0) {
      addActionsTop(actions.map(action => ({...action, address: {...address}})));
    }
  }

  getComponent(style) {
    const {t, model, attributes} = this.props;
    const {placeholder, size} = attributes;
    const {values} = model;
    const textToShow = (values[0] || {}).label || (values[0] || {}).value || placeholder;
    const classes = classNames("text-view", {[`text-${size}`]: size}, style);

    return <button className={classes} tabIndex={0} onClick={this.onClick} onKeyDown={this.onKeyDown}>{getVisibleTextData(textToShow, t)}</button>;
  }
}

export default connectComponent(AweInputTextView);
