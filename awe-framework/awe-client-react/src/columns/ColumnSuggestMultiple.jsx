import React, {useEffect, useMemo, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import useSuggest from "../hooks/useSuggest";
import ColumnSuggestInput from "./ColumnSuggestInput";
import PropTypes from "prop-types";

function ColumnSuggestMultiple(props) {

  const { placeholder, label, style, required, readonly, model, data, attrs, timeout } = props;
  const cellStyle = Array.isArray(data) ? null : data?.style;
  const {readonly: cellReadonly, validationRules = {}, visible = true, error = null} = attrs;
  const {required: cellRequired} = validationRules;
  const classes = classNames(style, cellStyle, "column-editor", {"p-invalid": error}, {"hidden": !visible});
  const {t} = useTranslation();
  const autocompleteRef = useRef(null);
  const selectedItems = useMemo(() => {
    if (Array.isArray(data)) {
      const selected = data.filter(item => item?.selected);
      return selected.length > 0 ? selected : data;
    }
    if (data?.value !== undefined && data?.value !== null) {
      return [data];
    }
    return [];
  }, [data]);
  const columnModel = useMemo(() => {
    const merged = [...(model?.values ?? []), ...selectedItems]
      .map(item => ({...item, label: item.label || item.value}));
    const seen = new Set();
    return merged.filter(item => {
      const key = String(item.value);
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  }, [model?.values, selectedItems]);
  const [suggestions, setSuggestions] = useState(columnModel);
  const [value, setValue] = useState([]);

  const {onChange, onClear, onKeyPress, onSuggest} =
    useSuggest(autocompleteRef, setSuggestions, value, setValue, props);

  useEffect(() => {
    setSuggestions(columnModel);
  }, [columnModel]);

  useEffect(() => {
    setValue(selectedItems.map(item => ({...item, label: item.label || item.value})));
  }, [selectedItems]);

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
      multiple={true}
      owner={props.address?.component}
      t={t}
    />
  );
}

ColumnSuggestMultiple.propTypes = {
  attrs: PropTypes.object,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  label: PropTypes.string,
  model: PropTypes.object,
  placeholder: PropTypes.string,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  timeout: PropTypes.number,
};

export default ColumnSuggestMultiple;
