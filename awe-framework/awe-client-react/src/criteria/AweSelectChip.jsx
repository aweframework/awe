import React from "react";
import PropTypes from "prop-types";
import {localeOption} from "primereact/api";
import {getIconNode} from "../utilities";
import {TestIds, testHook} from "../utilities/testIds";
import {getSuggestionLabel} from "../utilities/suggest";
import AweSelectMultipleChips from "./AweSelectMultipleChips";
import "./AweSelect.less";

/**
 * Chip of a chosen value of a multiple select or a multiple suggest (criterion and grid editor), as the chip of the
 * design system: its label and, unless the field is disabled, a remove icon that removes that value. The chip removes
 * its own value instead of the chip of PrimeReact, which ignores the click on a chip it considers out of sight of the
 * field
 * @category Criteria
 */
function AweSelectChip({option, disabled, onRemove}) {
  const remove = (event) => {
    event.stopPropagation();
    event.preventDefault();
    onRemove(option.code);
  };
  const onKeyDown = (event) => {
    if (["Enter", "NumpadEnter", "Space"].includes(event.code) || [" ", "Enter"].includes(event.key)) {
      remove(event);
    }
  };
  return <div className="awe-select-chip" {...testHook(TestIds.selectChoice)}>
    <span className="awe-select-chip-label" title={option.name}>{option.name}</span>
    {!disabled && <span role="button" tabIndex={0} className="awe-select-chip-remove"
      aria-label={`${localeOption("removeTokenIcon")} ${option.name}`} onClick={remove} onKeyDown={onKeyDown}
      {...testHook(TestIds.selectChoiceClose)}>
      {getIconNode("lucide:x", {"aria-hidden": "true"})}
    </span>}
  </div>;
}

AweSelectChip.propTypes = {
  option: PropTypes.shape({code: PropTypes.any, name: PropTypes.string}).isRequired,
  disabled: PropTypes.bool,
  onRemove: PropTypes.func.isRequired,
};

/**
 * Template of the chosen values of a multiple select: a chip per value and, while no value is chosen, the placeholder or
 * an empty label (PrimeReact writes "empty" in the field when the template renders nothing)
 * @param {boolean} disabled The select is disabled (its chips cannot be removed)
 * @param {function} onRemove Removes a value from its code
 * @param {string} placeholder Placeholder of the field
 * @returns {function} Selected item template of a MultiSelect
 */
export const selectChipTemplate = (disabled, onRemove, placeholder) => (option) => option
  ? <AweSelectChip option={option} disabled={disabled} onRemove={onRemove}/>
  : placeholder || <></>;

/**
 * Chips of the chosen values of a multiple suggest (AutoComplete): the chip of a multiple select inside each token of
 * PrimeReact, which then draws no remove icon of its own
 * @param {object[]} value Chosen values ({value, label})
 * @param {boolean} disabled The suggest is disabled (its chips cannot be removed)
 * @param {function} onChange Change handler of the AutoComplete, called with the values left
 * @returns {object} selectedItemTemplate and removeTokenIcon of an AutoComplete
 */
export const suggestChipProps = (value, disabled, onChange) => {
  const onRemove = (code) => onChange({value: value.filter(item => String(item.value) !== String(code))});
  return {
    selectedItemTemplate: (item) =>
      <AweSelectChip option={{code: item.value, name: getSuggestionLabel(item)}} disabled={disabled} onRemove={onRemove}/>,
    removeTokenIcon: () => null
  };
};

/**
 * Chosen values of a multiple suggest as one line (display="summary", #845): the first token of PrimeReact draws the
 * chips that fit and a "+N" counter that lists them all; the rest of the tokens draw nothing and are hidden
 * @param {object[]} value Chosen values ({value, label})
 * @param {boolean} disabled The suggest is disabled (its values cannot be removed)
 * @param {function} onChange Change handler of the AutoComplete, called with the values left
 * @returns {object} selectedItemTemplate and removeTokenIcon of an AutoComplete
 */
export const suggestSummaryProps = (value, disabled, onChange) => {
  const onRemove = (code) => onChange({value: value.filter(item => String(item.value) !== String(code))});
  const values = value.map(item => ({code: item.value, name: getSuggestionLabel(item)}));
  return {
    selectedItemTemplate: (item) => String(item.value) === String(value[0]?.value)
      ? <AweSelectMultipleChips values={values} onRemove={disabled ? undefined : onRemove}/>
      : null,
    removeTokenIcon: () => null
  };
};

export default AweSelectChip;
