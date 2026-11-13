import React, {useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import useSuggest from "../hooks/useSuggest";
import {getCellSuggestData} from "../utilities/grid";
import PropTypes from "prop-types";
import ColumnSuggestInput from "./ColumnSuggestInput";

function ColumnSuggest(props) {

  const { placeholder, label, style, required, readonly, model, data, attrs, timeout } = props;
  const {style: cellStyle} = data;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;
  const classes = classNames(style, cellStyle, "column-editor", {"p-invalid": error}, {"hidden": !visible});
  const {t} = useTranslation();
  const autocompleteRef = useRef(null);
  const columnModel = useMemo(() => getCellSuggestData(model, data), [model, data]);
  const [suggestions, setSuggestions] = useState(columnModel);
  const [value, setValue] = useState({});

  const {onChange, onClear, onKeyPress, onSuggest, initialSuggest} = useSuggest(autocompleteRef, setSuggestions, value, setValue, props);

  // Change model values if updated
  useEffect(() => {
    const fixedValues = columnModel
      .map(item => ({...item, label: item.label || item.value, needsInit: !("label" in item)}))
      .find(item => item.selected) || {};
    setValue(fixedValues);
  }, [columnModel]);

  // Initial suggest
  useEffect(() => {
    const {checkTarget, targetAction} = props;
    if ((checkTarget || targetAction) && value?.needsInit) {
      initialSuggest(value.value)
        .then(() => setValue(prev => ({...prev, needsInit: false})));
    }
  }, [value]);

  return (
    <ColumnSuggestInput
      autocompleteRef={autocompleteRef}
      value={value}
      placeholder={placeholder}
      label={label}
      required={required}
      cellRequired={cellRequired}
      readonly={readonly}
      cellReadonly={cellReadonly}
      onChange={onChange}
      onClear={onClear}
      onKeyPress={onKeyPress}
      timeout={timeout}
      error={error}
      suggestions={suggestions}
      onSuggest={onSuggest}
      classes={classes}
      multiple={false}
      owner={props.address?.component}
      t={t}
    />
  );
}

ColumnSuggest.propTypes = {
  attrs: PropTypes.object,
  checkTarget: PropTypes.any,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  label: PropTypes.string,
  model: PropTypes.object,
  placeholder: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  targetAction: PropTypes.any,
  timeout: PropTypes.number,
};

export default ColumnSuggest;
