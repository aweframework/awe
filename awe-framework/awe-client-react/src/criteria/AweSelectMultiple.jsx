import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweCriterionComponent from "./AweCriterionComponent";
import {bindMethods, translateLabel} from "../utilities";
import {MultiSelect} from "primereact/multiselect";
import {classNames} from "../utilities/components";

class AweSelectMultiple extends AweCriterionComponent {

  constructor(props) {
    super(props);
    this.state = {selected: []};
    bindMethods(this, ["getComponent", "getValue", "onChange"]);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    this.setState({selected: this.getValue()})
  }

  onChange(e) {
    const {address, model, updateModelWithDependencies} = this.props;
    const selectedItems = e.value.map(item => item.code);
    updateModelWithDependencies(address, {
      values: model.values.map(item => ({
        ...item,
        selected: selectedItems.includes(item.value)
      }))
    });
  }

  getValue() {
    const {t, model} = this.props;
    return model.values.filter(item => item.selected).map(item => ({code: item.value, name: translateLabel(item.label, t)}));
  }

  getComponent(style) {
    const {t, address, attributes, model} = this.props;
    const {placeholder, required, readonly, optional, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <MultiSelect
      ref={el => this.dropdown = el}
      id={address.component}
      inputid={address.component}
      value={this.getValue()}
      placeholder={translateLabel(placeholder, t)}
      onChange={this.onChange}
      required={required}
      disabled={readonly}
      className={classes}
      display="chip"
      resetFilterOnHide={true}
      filter
      optionLabel="name"
      options={model.values.map(item => ({code: item.value, name: translateLabel(item.label, t)}))}
      showClear={optional}
    />;
  }
}

export default connectComponent(AweSelectMultiple);
