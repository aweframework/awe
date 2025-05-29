import React from "react";
import {connectComponent} from "../components/AweComponent";
import {RadioButton} from "primereact/radiobutton";
import AweCheckboxRadioComponent from "./AweCheckboxRadioComponent";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";

class AweInputRadio extends AweCheckboxRadioComponent {

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, label, group, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <div className="field-radiobutton">
      <RadioButton
        inputid={address.component}
        name={group}
        checked={this.getChecked()}
        placeholder={translateLabel(placeholder, t)}
        onChange={this.onChange}
        required={required}
        disabled={readonly}
        className={classes}
      />
      <label htmlFor={address.component}>{translateLabel(label, t)}</label>
    </div>;
  }
}

export default connectComponent(AweInputRadio);
