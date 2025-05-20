import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {AutoComplete} from "primereact/autocomplete";
import {bindMethods, formatMessage} from "../utilities";
import {classNames, suggest} from "../utilities/components";

class ColumnSuggest extends Component {

  constructor(props) {
    super(props);
    this.state = {suggest: props.data.label || "", suggestions: [...props.model.values, props.data]};

    // Bind events
    this.suggesting = false;
    bindMethods(this, ["onChange", "onSelect", "onKeyPress", "onClear", "suggest"]);
    this.abortController = new AbortController();
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    const {suggestions} = this.state;
    const {data} = this.props;
    let found = (suggestions.find(suggestion => String(suggestion.value) === String(data.value)) || {});
    this.setState({suggest: found.label});
  }

  onSelect(e) {
    const {address, updateModelWithDependencies, data} = this.props;
    if (data.value !== e.value.value) {
      updateModelWithDependencies(address, {values: e.value});
    }
  }

  onChange(e) {
    if (this.state.suggest !== e.value) {
      this.setState({suggest: e.value});
    }
  }

  onClear() {
    const {address, updateModelWithDependencies} = this.props;

    // Clear suggest
    updateModelWithDependencies(address, {values: []});
  }

  onKeyPress(e) {
    if (e.code === "Space" && e.target.value === "") {
      this.autocomplete.onDropdownClick();
    }
  }

  suggest(event) {
    suggest(this, event, event.query);
  }

  render() {
    const {t, placeholder, label, required, readonly, timeout, data} = this.props;
    const {validationRules = {}} = data;
    const classes = classNames("column-editor", {"p-invalid": data?.error});
    return <AutoComplete
      ref={el => this.autocomplete = el}
      value={this.state.suggest}
      placeholder={t(placeholder || label) + (required ? " *" : "")}
      required={validationRules.required || required}
      disabled={data?.readonly || readonly}
      onChange={this.onChange}
      onSelect={this.onSelect}
      onClear={this.onClear}
      onKeyPress={this.onKeyPress}
      delay={timeout || 300}
      field="label"
      suggestions={this.state.suggestions}
      completeMethod={this.suggest}
      className={classes}
      inputClassName={classes}
      appendTo={document.body}
      tooltip={formatMessage(data?.error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      forceSelection={true}
    />;
  }
}

export default withTranslation()(ColumnSuggest);
