import React from "react";
import {connectComponent} from "../components/AweComponent";
import {Checkbox} from "primereact/checkbox";
import AweCheckboxRadioComponent from "./AweCheckboxRadioComponent";
import {classNames} from "../utilities/components";
import {InputSwitch} from "primereact/inputswitch";
import {translateLabel} from "../utilities";

class AweInputCheckbox extends AweCheckboxRadioComponent {

  getComponent(errorStyle = "") {
    const {t, address, attributes} = this.props;
    const {style = ""} = attributes;
    const {placeholder, required, readonly, label, size} = attributes;
    const isSwitch = style.includes("switch");
    const classes = classNames(errorStyle, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    if (isSwitch) {
      return <div className="field-checkbox">
        <InputSwitch
          inputid={address.component}
          checked={this.getChecked()}
          placeholder={translateLabel(placeholder, t)}
          onChange={(e) => this.onChange({target: {...e.target, checked: e.target.value}})}
          required={required}
          disabled={readonly}
          className={classes}
        />
        <label htmlFor={address.component}>{translateLabel(label, t)}</label>
      </div>;
    } else {
      return <div className="field-checkbox">
        <Checkbox
          inputid={address.component}
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
}

export default connectComponent(AweInputCheckbox);
