import React from "react";
import {AweComponent} from "../components/AweComponent";
import "./AweCriterionComponent.less";
import {bindMethods, formatMessage, getHelpTooltipNode, getIconCode} from "../utilities";
import {classNames} from "../utilities/components";

export default class AweCriterionComponent extends AweComponent {

  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["getLabel", "getIcon", "getUnit", "getHelpIcon"]);
    this.groupClass = "p-inputgroup";
  }

  getHelpIcon() {
    const {attributes} = this.props;
    const {help, helpImage} = attributes;
    if (help || helpImage) {
      return <i role="note" className={`help-icon pi pi-question-circle`} />;
    }
    return null;
  }

  getLabel() {
    const {t, address, attributes, settings} = this.props;
    const {label, style, help, helpImage} = attributes;
    if (label) {
      return <>
        {getHelpTooltipNode(help, helpImage, t,`.help-target-${address.component}`)}
        <label htmlFor={address.component} className={`block help-target-${address.component}`}
               data-pr-position="bottom"
               data-pr-at="left+6 bottom"
               style={{...(help || helpImage ? {cursor: "pointer"} : {})}}
               data-pr-showdelay={settings.helpTimeout}>{this.getHelpIcon()}{t(label)}</label>
      </>;
    } else if ((style || "").includes("no-label")) {
      return null;
    } else {
      return <div className="block mb-2">&nbsp;</div>;
    }
  }

  getIcon() {
    const {icon} = this.props.attributes;
    return icon ? <span className="p-inputgroup-addon">{getIconCode(icon)}</span> : null;
  }

  getUnit() {
    const {t, attributes} = this.props;
    const {unit, size} = attributes;
    if (unit) {
      return <span className={classNames("p-inputgroup-addon", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size})}>{t(unit)}</span>;
    }

    return null;
  }

  getValidation() {
    const {t, attributes} = this.props;
    const {error} = attributes;

    if (error) {
      return <small className="p-invalid">{formatMessage(error, t)}</small>
    }

    return null;
  }

  render() {
    const {attributes, validationRules, address} = this.props;
    const {style, visible, error, invisible, readonly} = attributes || {};

    // Component classes
    const classes = classNames("field", {invisible, "hidden": !visible, "p-disabled": readonly,
      "required": validationRules?.required, "col-12": !style}, style);

    // Paint component
    return <div className={classes} criterion-id={address.component}>
      {this.getLabel()}
      <div className={classNames(this.groupClass)} style={{position: "relative"}}>
        {this.getIcon()}
        {this.getComponent(error ? "p-invalid" : "")}
        {this.getUnit()}
      </div>
      {this.getValidation()}
    </div>;
  }
}
