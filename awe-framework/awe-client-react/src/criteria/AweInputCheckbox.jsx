import React from "react";
import {connectComponent} from "../components/AweComponent";
import {Checkbox} from "primereact/checkbox";
import AweCheckboxRadioComponent from "./AweCheckboxRadioComponent";
import {classNames} from "../utilities/components";

class AweInputCheckbox extends AweCheckboxRadioComponent {

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, label, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <div className="field-checkbox">
      <Checkbox
        inputid={address.component}
        checked={this.getChecked()}
        placeholder={t(placeholder)}
        onChange={this.onChange}
        required={required}
        disabled={readonly}
        className={classes}
      />
      <label htmlFor={address.component}>{t(label)}</label>
    </div>;
  }
}

export default connectComponent(AweInputCheckbox);
