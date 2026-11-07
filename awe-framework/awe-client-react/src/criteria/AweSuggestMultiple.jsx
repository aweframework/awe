import React, {useEffect, useMemo, useRef, useState} from "react";
import {criterionPlaceholder, isCriterionRequired, requiredAria} from "../utilities/labels";
import {AutoComplete} from "primereact/autocomplete";
import {classNames} from "../utilities/components";
import {getIconNode} from "../utilities";
import {TestIds, testHook} from "../utilities/testIds";
import "./AweSuggest.less";
import {useTranslation} from "react-i18next";
import {useComponentState} from "../hooks/useComponentState";
import AweCriterion from "./AweCriterion";
import useSuggest from "../hooks/useSuggest";
import {getSuggestionLabel, withTextLabels} from "../utilities/suggest";
import useComponent from "../hooks/useComponent";
import PropTypes from "prop-types";
import {autoCompletePassThrough} from "../utilities/testPassThrough";
import {suggestChipProps} from "./AweSelectChip";

function AweSuggestMultiple(props) {
  const { id } = props;

  const { t } = useTranslation();
  const { address } = useComponent(id);
  const { model, attributes, validationRules } = useComponentState(id);
  const autocompleteRef = useRef(null);
  const [suggestions, setSuggestions] = useState([...model.values]);
  // PrimeReact needs the labels as text, but the server answers numbers for a suggest over a numeric column
  const textSuggestions = useMemo(() => withTextLabels(suggestions), [suggestions]);
  const [value, setValue] = useState(model.values.filter(item => item.selected) || []);

  const { onChange, onClear, onKeyPress, onSuggest, initialSuggest } = useSuggest(autocompleteRef, setSuggestions, value, setValue, { ...attributes, address });

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

  const {required, readonly, timeout, size, error } = attributes;
  const classes = classNames("", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });
  const chosen = value.map(item => ({ ...item, label: getSuggestionLabel(item) }));
  // A field that can be left empty (it is not required) offers one button that empties every chosen value
  const clearable = !readonly && !isCriterionRequired(attributes, validationRules) && chosen.length > 0;

  // A value can be chosen only once: the server answers a new object for the same option every time, so PrimeReact
  // compares two different elements and the same value would be stored twice (and sent twice to the server)
  const onChosen = (event) => {
    // The text being typed is not a list of values: it goes to the component as it comes
    if (!Array.isArray(event.value)) {
      onChange(event);
      return;
    }
    const seen = new Set();
    const unique = event.value.filter(item => {
      if (item === null || item === undefined) {
        return false;
      }
      const key = String(item?.value ?? item);
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
    onChange({...event, value: unique});
  };

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <AutoComplete multiple={true}
        ref={autocompleteRef}
        id={id}
        value={chosen}
        {...suggestChipProps(chosen, readonly, onChosen)}
        placeholder={criterionPlaceholder(attributes, validationRules, t)}
        required={required}
        {...requiredAria(attributes, validationRules)}
        disabled={readonly}
        onChange={onChosen}
        onKeyDown={onKeyPress}
        delay={timeout || 300}
        field="label"
        invalid={error}
        suggestions={textSuggestions}
        completeMethod={onSuggest}
        className={classes}
        appendTo={document.body}
        dropdown
        forceSelection
        autoHighlight
        pt={autoCompletePassThrough(id, true)}
      />
      {clearable && <button type="button" className="awe-suggest-clear" aria-label={t("BUTTON_CLEAR")}
        onClick={onClear} {...testHook(TestIds.selectClear)}>
        {getIconNode("lucide:x", {"aria-hidden": "true"})}
      </button>}
    </AweCriterion>
  );
}

AweSuggestMultiple.propTypes = {
  id: PropTypes.string,
};

export default AweSuggestMultiple;