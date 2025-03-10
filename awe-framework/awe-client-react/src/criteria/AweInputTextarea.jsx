import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {InputTextarea} from "primereact/inputtextarea";
import {classNames} from "../utilities/components";

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
    const {placeholder, required, readonly, size} = attributes;
    const classes = classNames("col-12", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style);

    return <InputTextarea
      id={address.component}
      value={this.state.value}
      className={classes}
      placeholder={t(placeholder)}
      onChange={this.onChange}
      onBlur={this.onBlur}
      required={required}
      disabled={readonly}
    />;
  }
}

export default connectComponent(AweInputTextarea);
