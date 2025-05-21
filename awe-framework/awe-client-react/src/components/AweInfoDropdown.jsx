import React from "react";
import {Button} from "primereact/button";
import {AweComponent, connectComponent} from "./AweComponent";
import {OverlayPanel} from "primereact/overlaypanel";
import {classNames, clickDropdown} from "../utilities/components";

import "./AweInfoDropdown.less";
import {getIconCode, translateLabel} from "../utilities";
import {Components} from "../utilities/structure";

class AweInfoDropdown extends AweComponent {

  constructor(props) {
    super(props);

    this.onClick = this.onClick.bind(this);
  }

  onClick(e) {
    clickDropdown(e, this.props, this.op);
  }

  render() {
    const {t, attributes, model, elementList = []} = this.props;
    const {values} = model;
    const {style, icon, disabled, label, size, unit} = attributes;
    const classes = classNames("p-button-rounded", "p-button-text", "p-button-secondary", {[`p-button-${size}`]: size}, style);

    let computedLabel = values.length ? values[0].label : label;
    let computedUnit = unit && <span className="p-badge p-badge-info">{unit}</span>;
    return <>
      <span className="p-overlay-badge">
        <Button
          id={this.props.address.component}
          type="button"
          className={classes}
          icon={getIconCode(icon, "p-button-icon p-c p-button-icon-left")}
          disabled={this.props.disabled || disabled}
          label={translateLabel(computedLabel, t)}
          iconPos={"left"}
          onClick={this.onClick}
        />
        {computedUnit}
      </span>
      <OverlayPanel ref={(el) => this.op = el} dismissable className={"info-dropdown"}>
        {elementList.map((node, index) => Components(node, index))}
      </OverlayPanel>
    </>;
  }
}

export default connectComponent(AweInfoDropdown);
