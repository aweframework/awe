import React, {useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {AutoComplete} from "primereact/autocomplete";
import {formatMessage, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import useSuggest from "../hooks/useSuggest";
import {getCellSuggestData} from "../utilities/grid";
import {getFirstDefinedValue} from "../utilities/general";

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

  return <AutoComplete
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
  />;
}

export default ColumnSuggest;
