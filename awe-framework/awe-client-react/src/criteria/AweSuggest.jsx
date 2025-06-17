import React from "react";
import {connectComponent} from "../components/AweComponent";
import {AutoComplete} from "primereact/autocomplete";

import "./AweSuggest.less";
import AweSuggestComponent from "./AweSuggestComponent";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";

class AweSuggest extends AweSuggestComponent {

  getComponent(style) {
    const {t, address, placeholder, required, readonly, timeout, size} = this.props;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});
    return <AutoComplete
      ref={el => this.autocomplete = el}
      id={address.component}
      value={Array.isArray(this.state.suggest) ? this.state.suggest[0] || null : this.state.suggest}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      onChange={this.onChange}
      onSelect={this.onSelect}
      onClear={this.onClear}
      dropdown
      delay={timeout || 300}
      field="label"
      suggestions={this.state.suggestions}
      completeMethod={this.suggest}
      className={classes}
      appendTo={document.body}
      forceSelection={true}
    />;
  }
}

export default connectComponent(AweSuggest);
