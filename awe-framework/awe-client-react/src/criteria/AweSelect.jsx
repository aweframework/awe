import React from "react";
import {connectComponent} from "../components/AweComponent";
import {Dropdown} from "primereact/dropdown";
import AweCriterionComponent from "./AweCriterionComponent";
import {bindMethods, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";

class AweSelect extends AweCriterionComponent {

  constructor(props) {
    super(props);

    bindMethods(this, ["getComponent", "getValue", "onChange"])
  }

  onChange(e) {
    const {address, model, updateModelWithDependencies} = this.props;
    updateModelWithDependencies(address, {
      values: model.values.map(d => ({
        ...d,
        selected: d.value === e.target.value
      }))
    });
  }

  getValue() {
    return this.props.model.values.filter(v => v.selected).map(v => v.value)[0] || null;
  }

  getComponent(style) {
    const {t, address, attributes, model} = this.props;
    const {placeholder, required, readonly, optional, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <Dropdown
      ref={el => this.dropdown = el}
      id={address.component}
      value={this.getValue()}
      placeholder={translateLabel(placeholder, t)}
      onChange={this.onChange}
      required={required}
      disabled={readonly}
      className={classes}
      options={model.values.map(value => ({...value, label: translateLabel(value.label, t)}))}
      showClear={optional}
      filter={model.values.length > 5}
      filterBy="label"
    />;
  }
}

export default connectComponent(AweSelect);
