import React from "react";
import {RadioButton} from "primereact/radiobutton";
import PropTypes from "prop-types";
import useRadioColumn from "../hooks/useRadioColumn";
import {formatMessage} from "../utilities";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";

/**
 * Radio column: a radio button with its label for every option of the list of the column, like a select column does.
 * The value of the chosen option is the value of the cell.
 */
function ColumnRadio(props) {
  const {style, data = {}} = props;
  const {options, disabled, visible, error, name, groupLabel, required, onSelect, onKeyDown, t} = useRadioColumn(props);

  return <div role="radiogroup" aria-label={groupLabel}
    className={classNames("column-radio", style, data?.style, {"hidden": !visible})}>
    {options.map(option => <span key={option.value} className="column-radio-option">
      <RadioButton
        inputId={option.inputId}
        name={name}
        value={option.value}
        checked={option.checked}
        required={required}
        disabled={disabled || option.disabled}
        invalid={!!error}
        aria-label={option.label}
        onChange={() => onSelect(option)}
        pt={{
          root: testHook(TestIds.criterionInput, {selected: option.checked, attributes: {"option-id": option.value}}),
          input: {onKeyDown}
        }}
        tooltip={formatMessage(error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
      <label htmlFor={option.inputId} className="column-radio-label">{option.label}</label>
    </span>)}
  </div>;
}

ColumnRadio.propTypes = {
  address: PropTypes.object.isRequired,
  attrs: PropTypes.object,
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
  label: PropTypes.string,
  model: PropTypes.object,
  readonly: PropTypes.bool,
  required: PropTypes.bool,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

export default ColumnRadio;
