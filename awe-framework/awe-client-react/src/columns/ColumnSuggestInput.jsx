import React from "react";
import {AutoComplete} from "primereact/autocomplete";
import {formatMessage, translateLabel} from "../utilities";
import {getFirstDefinedValue} from "../utilities/general";
import PropTypes from "prop-types";
import {autoCompletePassThrough} from "../utilities/testPassThrough";

function ColumnSuggestInput(props) {
  const {
    autocompleteRef,
    value,
    placeholder,
    label,
    required,
    cellRequired,
    readonly,
    cellReadonly,
    onChange,
    onClear,
    onKeyPress,
    timeout,
    error,
    suggestions,
    onSuggest,
    classes,
    multiple,
    owner,
    t
  } = props;

  return (
    <AutoComplete
      multiple={multiple}
      ref={autocompleteRef}
      value={value}
      placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
      required={getFirstDefinedValue(cellRequired, required, false)}
      disabled={getFirstDefinedValue(cellReadonly, readonly, false)}
      onChange={onChange}
      onClear={onClear}
      dropdown
      onKeyDown={onKeyPress}
      delay={timeout || 300}
      field="label"
      invalid={error}
      suggestions={suggestions}
      completeMethod={onSuggest}
      className={classes}
      inputClassName={classes}
      appendTo={document.body}
      tooltip={formatMessage(error, t)}
      tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      forceSelection={true}
      pt={autoCompletePassThrough(owner, multiple)}
    />
  );
}

ColumnSuggestInput.propTypes = {
  autocompleteRef: PropTypes.object,
  value: PropTypes.any,
  placeholder: PropTypes.string,
  label: PropTypes.string,
  required: PropTypes.bool,
  cellRequired: PropTypes.bool,
  readonly: PropTypes.bool,
  cellReadonly: PropTypes.bool,
  onChange: PropTypes.func,
  onClear: PropTypes.func,
  onKeyPress: PropTypes.func,
  timeout: PropTypes.number,
  error: PropTypes.any,
  suggestions: PropTypes.array,
  onSuggest: PropTypes.func,
  classes: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  multiple: PropTypes.bool,
  t: PropTypes.func
};

export default ColumnSuggestInput;
