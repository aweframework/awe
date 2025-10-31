import React, {useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {AutoComplete} from "primereact/autocomplete";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import useSuggest from "../hooks/useSuggest";
import {getCellSuggestData} from "../utilities/grid";

function ColumnSuggest(props) {

  const { placeholder, label, required, readonly, model, data, timeout } = props;
  const {validationRules = props.validationRules || {}} = data;
  const classes = classNames("column-editor", {"p-invalid": data?.error});
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

  return <AutoComplete
    ref={autocompleteRef}
    value={value}
    placeholder={translateLabel(placeholder || label, t) + (required ? " *" : "")}
    required={validationRules.required || required}
    disabled={data?.readonly || readonly}
    onChange={onChange}
    onClear={onClear}
    dropdown
    onKeyDown={onKeyPress}
    delay={timeout || 300}
    field="label"
    invalid={data?.error}
    suggestions={suggestions}
    completeMethod={onSuggest}
    className={classes}
    inputClassName={classes}
    appendTo={document.body}
    tooltip={formatMessage(data?.error, t)}
    tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
    forceSelection={true}
  />;
}

export default ColumnSuggest;
