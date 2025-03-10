import React from "react";
import {connectComponent} from "../components/AweComponent";
import {AutoComplete} from "primereact/autocomplete";
import AweSuggestComponent from "./AweSuggestComponent";
import {classNames} from "../utilities/components";

class AweSuggestMultiple extends AweSuggestComponent {

  onChange(e) {
    const {address, updateModelWithDependencies} = this.props;
    const uniqueSuggest = _.uniqBy(this.state.suggest, "value");
    const uniqueValue = _.uniqBy(e.value, "value");
    if (uniqueSuggest.length !== uniqueValue.length) {
      updateModelWithDependencies(address, {values: uniqueValue.map(item => ({...item, selected: true}))});
    }
  }

  onSelect(e) {
    const {address, updateModelWithDependencies} = this.props;
    const uniqueSuggest = _.uniqBy(this.state.suggest, "value").map(t => t.value);
    if (!uniqueSuggest.includes(e.value?.value || null)) {
      updateModelWithDependencies(address, {values: [...this.state.suggest, e.value].flat().map(item => ({...item, selected: true}))});
    }
  }

  getComponent(style) {
    const {t, address, placeholder, required, readonly, timeout, size} = this.props;
    const {suggest, suggestions} = this.state;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <AutoComplete multiple
      ref={el => this.autocomplete = el}
      id={address.component}
      value={suggest.map(item => ({...item, label: item.label || item.value}))}
      placeholder={t(placeholder)}
      required={required}
      disabled={readonly}
      onChange={this.onChange}
      onSelect={this.onSelect}
      onKeyPress={this.onKeyPress}
      delay={timeout || 300}
      field="label"
      suggestions={suggestions}
      completeMethod={this.suggest}
      className={classes}
      appendTo={document.body}
      forceSelection={true}
    />;
  }
}

export default connectComponent(AweSuggestMultiple);
