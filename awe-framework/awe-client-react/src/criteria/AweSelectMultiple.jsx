import React from "react";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {MultiSelect} from "primereact/multiselect";
import {classNames} from "../utilities/components";
import AweCriterion from "./AweCriterion";
import AweSelectMultipleChips from "./AweSelectMultipleChips";
import {useSelect} from "../hooks/useSelect";
import PropTypes from "prop-types";
import {multiSelectPassThrough} from "../utilities/testPassThrough";

function AweSelectMultiple(props) {

  const {id} = props;
  const {t, ref, address, attributes, validationRules, options, selected, onChange} =
    useSelect({id, multiple: true});

  const {required, readonly, optional, size, error, display} = attributes;
  // display="chip" keeps the chips of PrimeReact, one remove icon each, which grow with the values. By default the field
  // stays one control high: the chips that fit, then a counter that lists them all (#922)
  const oneLine = display !== "chip";
  const classes = classNames({
    [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error,
    "awe-select-multiple": oneLine, "awe-select-multiple-clearable": oneLine && optional && selected?.length
  });
  const chipsTemplate = value => value && value.code === selected[0].code
    ? <AweSelectMultipleChips values={selected} onRemove={code => onChange({value: selected.filter(item => item.code !== code)})}/>
    : null;
  return <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <MultiSelect
        ref={ref}
        inputId={id}
        value={selected}
        placeholder={criterionPlaceholder(attributes, validationRules, t, false)}
        onChange={onChange}
        required={required}
        disabled={readonly}
        className={classes}
        display={oneLine ? "comma" : "chip"}
        selectedItemTemplate={oneLine ? chipsTemplate : undefined}
        resetFilterOnHide={true}
        filter
        invalid={error}
        optionLabel="name"
        options={options}
        showClear={optional}
        pt={{...multiSelectPassThrough(id), input: requiredAria(attributes, validationRules)}}
      />
    </AweCriterion>;
}

AweSelectMultiple.propTypes = {
  id: PropTypes.string,
};

export default AweSelectMultiple;
