import React from "react";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {MultiSelect} from "primereact/multiselect";
import {classNames} from "../utilities/components";
import AweCriterion from "./AweCriterion";
import {useSelect} from "../hooks/useSelect";
import PropTypes from "prop-types";
import {multiSelectPassThrough} from "../utilities/testPassThrough";

function AweSelectMultiple(props) {

  const {id} = props;
  const {t, ref, address, attributes, validationRules, options, selected, onChange} =
    useSelect({id, multiple: true});

    const {required, readonly, optional, size, error} = attributes;
    const classes = classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });
  return <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <MultiSelect
        ref={ref}
        id={id}
        inputid={id}
        value={selected}
        placeholder={criterionPlaceholder(attributes, validationRules, t, false)}
        onChange={onChange}
        required={required}
        disabled={readonly}
        className={classes}
        display="chip"
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
