import React, {useEffect, useRef, useState} from "react";
import {AutoComplete} from "primereact/autocomplete";
import {classNames} from "../utilities/components";
import "./AweSuggest.less";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";
import AweCriterion from "./AweCriterion";
import useSuggest from "../hooks/useSuggest";
import {translateLabel} from "../utilities";
import {Skeleton} from "primereact/skeleton";
import PropTypes from "prop-types";

function AweSuggest(props) {
  const {id, style: propsStyle} = props;
  const {t} = useTranslation();
  const {address, model, attributes, validationRules} = useSelector(state => ({
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes,
    validationRules: state.components[id]?.validationRules}));
  const autocompleteRef = useRef(null);
  const [suggestions, setSuggestions] = useState([...model?.values || []]);
  const [value, setValue] = useState({});
  const {onChange, onClear, onKeyPress, onSuggest, initialSuggest} = useSuggest(autocompleteRef, setSuggestions, value, setValue, {...attributes, address});

  // Change model values if updated
  useEffect(() => {
    const fixedValues = (model?.values || [])
      .map(item => ({...item, label: item.label || item.value, needsInit: !("label" in item)}))
      .find(item => item.selected) || {};
    setValue(fixedValues);
  }, [model?.values]);

  // Initial suggest
  useEffect(() => {
    const {checkTarget, targetAction} = attributes || {}
    if ((checkTarget || targetAction) && value?.needsInit) {
      const query = value?.value;
      initialSuggest(query);
    }
  }, [value, attributes]);

  // If address is undefined, return skeleton
  if (!address) {
    return <Skeleton width="10rem" height="2rem" style={propsStyle}/>;
  }

  const {placeholder, required, readonly, timeout, size, error} = attributes;
  const classes = classNames("", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error});
  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <AutoComplete
        field="label"
        ref={autocompleteRef}
        id={address.component}
        value={value}
        placeholder={translateLabel(placeholder, t)}
        required={required}
        disabled={readonly}
        onChange={onChange}
        onClear={onClear}
        dropdown
        invalid={error}
        onKeyDown={onKeyPress}
        delay={timeout || 300}
        suggestions={suggestions}
        completeMethod={onSuggest}
        className={classes}
        appendTo={document.body}
        forceSelection={true}
      />
    </AweCriterion>
  );
}

AweSuggest.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string
};

export default AweSuggest;
