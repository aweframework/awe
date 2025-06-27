import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {AutoComplete} from "primereact/autocomplete";
import {bindMethods, formatMessage, translateLabel} from "../utilities";
import {classNames, suggest} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnSuggest extends Component {

  constructor(props) {
    super(props);
    this.state = {suggest: {...props.data, label: props.data?.label || props.data?.value || ""}, suggestions: [...(props.data?.value !== null ? [{...props.data, label: props.data?.label || props.data?.value || ""}] : [])]};

    // Bind events
    this.suggesting = false;
    bindMethods(this, ["onChange", "onSelect", "onClear", "suggest"]);
    this.abortController = new AbortController();
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    const {suggestions} = this.state;
    const {data} = this.props;
    let found = (suggestions.find(suggestion => String(suggestion.value) === String(data.value)) || {});
    this.setState({suggest: {...found, label: found.label || found.value || ""}});
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

  suggest(event) {
    suggest(this, event, event.query);
  }

  render() {
    const {t, placeholder, label, required, readonly, timeout, data, style} = this.props;
    const {validationRules = {}} = data;
    const classes = classNames("column-editor", {"p-invalid": data?.error}, style, data?.style);
    return <AutoComplete
      ref={el => this.autocomplete = el}
      value={this.state.suggest}
      placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
      required={validationRules.required || required}
      disabled={data?.readonly || readonly}
      onChange={this.onChange}
      onSelect={this.onSelect}
      onClear={this.onClear}
      dropdown
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

ColumnSuggest.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  placeholder: PropTypes.string,
  label: PropTypes.string,
  required: PropTypes.string,
  readonly: PropTypes.string,
  timeout: PropTypes.string,
  t: PropTypes.func.isRequired,
  updateModelWithDependencies: PropTypes.func.isRequired,
};

export default withTranslation()(ColumnSuggest);
