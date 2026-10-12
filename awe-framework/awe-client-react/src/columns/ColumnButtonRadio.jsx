import React from "react";
import PropTypes from "prop-types";
import useRadioColumn from "../hooks/useRadioColumn";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";

/**
 * Button radio column: the options of the list of the column drawn as one joined bar of toggle buttons, like the
 * button-radio criterion. The value of the chosen option is the value of the cell.
 */
function ColumnButtonRadio(props) {
  const {style, data = {}} = props;
  const {options, disabled, visible, error, name, groupLabel, required, onSelect, onKeyDown, t} = useRadioColumn(props);

  return <div role="radiogroup" aria-label={groupLabel}
    className={classNames("column-button-radio", "p-selectbutton", "p-buttonset", style, data?.style, {"hidden": !visible, "p-invalid": !!error})}>
    {options.map(option => <label key={option.value} htmlFor={option.inputId}
      className={classNames("p-button", "p-component", {"p-highlight": option.checked, "p-disabled": disabled || option.disabled})}
      title={error ? t(error.message || error) : undefined}
      {...testHook(TestIds.criterionInput, {selected: option.checked, attributes: {"option-id": option.value}})}>
      <input type="radio" id={option.inputId} name={name} value={option.value} checked={option.checked} required={required}
        disabled={disabled || option.disabled} aria-label={option.label} className="p-hidden-accessible"
        onChange={() => onSelect(option)} onKeyDown={onKeyDown}/>
      <span className="p-button-label p-c">{option.label}</span>
    </label>)}
  </div>;
}

ColumnButtonRadio.propTypes = {
  address: PropTypes.object.isRequired,
  attrs: PropTypes.object,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  label: PropTypes.string,
  model: PropTypes.object,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

export default ColumnButtonRadio;
