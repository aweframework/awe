import AweCriterionComponent from "./AweCriterionComponent";
import {bindMethods, getIconCode, translateLabel} from "../utilities";
import React from "react";
import {classNames} from "../utilities/components";

export default class AweCheckboxRadioComponent extends AweCriterionComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["getComponent", "getValue", "onChange", "itemTemplate"])
  }

  onChange(e) {
    const {address, model, updateModelWithDependencies} = this.props;
    updateModelWithDependencies(address, {
      values: model.values.map(d => ({
        ...d,
        selected: e.target.checked
      }))
    });
  }

  getLabel() {
    return null;
  }

  getChecked() {
    return this.props.model.values.filter(v => v.selected).length > 0;
  }

  getValue() {
    return this.props.model.values.filter(item => item.selected).map(item => item.value);
  }

  selectedItemsTemplate(option) {
    const {placeholder} = this.props.attributes;
    if (option) {
      return <span>{option.name || option.code}</span>;
    }
    return placeholder || <>&nbsp;</>;
  }

  itemTemplate(option, attributes) {
    const {t} = this.props;
    const {icon, label, style} = option;
    const {size} = attributes;
    const iconTemplate = getIconCode(icon);
    const labelTemplate = label ? <span>{translateLabel(label, t)}</span> : null;
    return <span className={classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style)}>
      {iconTemplate}
      {labelTemplate}
    </span>;
  }
}
