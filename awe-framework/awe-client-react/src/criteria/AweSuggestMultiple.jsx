import React, {useRef, useState} from "react";
import {AutoComplete} from "primereact/autocomplete";
import {classNames} from "../utilities/components";
import "./AweSuggest.less";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";
import AweCriterion from "./AweCriterion";
import useSuggest from "../hooks/useSuggest";
import {translateLabel} from "../utilities";

function AweSuggestMultiple(props) {
  const {id} = props;

  const {t} = useTranslation();
  const {address, model, attributes, validationRules} = useSelector(state => ({
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes,
    validationRules: state.components[id]?.validationRules}));
  const autocompleteRef = useRef(null);
  const [suggestions, setSuggestions] = useState([...model.values]);
  const [value, setValue] = useState(model.values.filter(item => item.selected) || []);

  const {onChange, onKeyPress, onSuggest} = useSuggest(autocompleteRef, setSuggestions, value, setValue, {...attributes, address});

  const {placeholder, required, readonly, timeout, size, error} = attributes;
  const classes = classNames("", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error});

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <AutoComplete multiple={true}
        ref={autocompleteRef}
        id={address.component}
        value={value.map(item => ({...item, label: item.label || item.value}))}
        placeholder={translateLabel(placeholder, t)}
        required={required}
        disabled={readonly}
        onChange={onChange}
        onKeyDown={onKeyPress}
        delay={timeout || 300}
        field="label"
        invalid={error}
        suggestions={suggestions}
        completeMethod={onSuggest}
        className={classes}
        appendTo={document.body}
        forceSelection
      />
    </AweCriterion>
  );
}

export default AweSuggestMultiple;