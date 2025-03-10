import React from "react";
import {connectComponent} from "../components/AweComponent";
import {SelectButton} from "primereact/selectbutton";
import AweCheckboxRadioComponent from "./AweCheckboxRadioComponent";

class AweButtonCheckbox extends AweCheckboxRadioComponent {

  constructor(props) {
    super(props);
  }

  onChange(e) {
    const {address, model, updateModelWithDependencies} = this.props;
    updateModelWithDependencies(address, {
      values: model.values.map(item => ({
        ...item,
        selected: e.value.includes(item.value)
      }))
    });
  }

  getComponent(style) {
    const {address, model, attributes} = this.props;
    return <SelectButton
      multiple
      inputid={address.component}
      value={this.getValue()}
      options={model.values}
      onChange={this.onChange}
      itemTemplate={o => this.itemTemplate(o, attributes)}
      className={style}
    />;
  }
}

export default connectComponent(AweButtonCheckbox);
