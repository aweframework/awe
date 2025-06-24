import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {InputTextarea} from "primereact/inputtextarea";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";

class AweInputTextarea extends AweTextComponent {

  constructor(props) {
    super(props);
    this.groupClass = "";

    this.getComponent = this.getComponent.bind(this);
  }

  getIcon() {
    return null;
  }

  getUnit() {
    return null;
  }

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, size, areaRows} = attributes;
    const classes = classNames("w-full", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style);

    return <InputTextarea
      id={address.component}
      value={this.state.value}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      onChange={this.onChange}
      onBlur={this.onBlur}
      required={required}
      disabled={readonly}
      rows={areaRows}
    />;
  }
}

export default connectComponent(AweInputTextarea);
