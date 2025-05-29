import React from "react";
import {InputText} from 'primereact/inputtext';
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";

class AweInputText extends AweTextComponent {

  constructor(props) {
    super(props);

    this.getComponent = this.getComponent.bind(this);
  }

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <InputText
      id={address.component}
      value={this.state.value}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      onChange={this.onChange}
      onBlur={this.onBlur}
      onKeyDown={e => e.key === "Enter" && this.onSubmit()}
      required={required}
      disabled={readonly}
    />;
  }
}

export default connectComponent(AweInputText);
