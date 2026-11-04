import React from "react";
import {SelectButton} from "primereact/selectbutton";
import useCheckboxRadio from "../hooks/useCheckboxRadio";
import AweCriterion from "./AweCriterion";
import classNames from "classnames";
import PropTypes from "prop-types";
import { buttonGroupPassThrough } from "../utilities/testPassThrough";

function AweButtonRadio(props) {
  const { id } = props;
  const { address, model, attributes, validationRules, getValue, optionLabel, itemTemplate, onChangeButtonRadio } = useCheckboxRadio(id);

  const { size, error = false, initialLoad } = attributes;
  const classes = classNames({ [`text-${size}`]: size, "p-invalid": error });
  // A criterion with options (initial load enum or query, or more than one value) shows its label above its buttons, as
  // any other criterion; a criterion without options is one button whose text is its label
  const hasOptions = Boolean(initialLoad) || (model.values || []).length > 1;
  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel={hasOptions}>
      <SelectButton
        inputid={address?.component}
        value={getValue()[0] || null}
        options={model.values}
        optionLabel={optionLabel}
        onChange={onChangeButtonRadio}
        itemTemplate={itemTemplate}
        className={classes}
        pt={buttonGroupPassThrough}
      />
    </AweCriterion>
  );
}

AweButtonRadio.propTypes = {
  id: PropTypes.string,
};

export default AweButtonRadio;
