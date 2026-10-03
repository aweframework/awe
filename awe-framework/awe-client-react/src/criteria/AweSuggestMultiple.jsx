import React, {useEffect, useRef, useState} from "react";
import {AutoComplete} from "primereact/autocomplete";
import {classNames} from "../utilities/components";
import "./AweSuggest.less";
import {useTranslation} from "react-i18next";
import {useComponentState} from "../hooks/useComponentState";
import AweCriterion from "./AweCriterion";
import useSuggest from "../hooks/useSuggest";
import {translateLabel} from "../utilities";
import useComponent from "../hooks/useComponent";
import PropTypes from "prop-types";
import {autoCompletePassThrough} from "../utilities/testPassThrough";

function AweSuggestMultiple(props) {
  const { id } = props;

  const { t } = useTranslation();
  const { address } = useComponent(id);
  const { model, attributes, validationRules } = useComponentState(id);
  const autocompleteRef = useRef(null);
  const [suggestions, setSuggestions] = useState([...model.values]);
  const [value, setValue] = useState(model.values.filter(item => item.selected) || []);

  const { onChange, onKeyPress, onSuggest, initialSuggest } = useSuggest(autocompleteRef, setSuggestions, value, setValue, { ...attributes, address });

  // Show the selected values again when the model changes (a dependency or an action selects other values)
  const isFirstRender = useRef(true);
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    setValue((model.values || []).filter(item => item.selected));
  }, [model.values]);

  // Retrieve the labels of the values that were selected without one (a server action selects only their values)
  useEffect(() => {
    if (attributes?.checkTarget && value.some(item => !item.label)) {
      initialSuggest(value.map(item => item.value));
    }
  }, [value]);

  const { placeholder, required, readonly, timeout, size, error } = attributes;
  const classes = classNames("", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <AutoComplete multiple={true}
        ref={autocompleteRef}
        id={id}
        value={value.map(item => ({ ...item, label: item.label || item.value }))}
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
        pt={autoCompletePassThrough(id, true)}
      />
    </AweCriterion>
  );
}

AweSuggestMultiple.propTypes = {
  id: PropTypes.string,
};

export default AweSuggestMultiple;