import React from "react";
import {SelectButton} from "primereact/selectbutton";
import useCheckboxRadio from "../hooks/useCheckboxRadio";
import AweCriterion from "./AweCriterion";
import classNames from "classnames";
import PropTypes from "prop-types";

function AweButtonRadio(props) {
  const { id } = props;
  const { address, model, attributes, validationRules, getValue, itemTemplate, onChangeButtonRadio } = useCheckboxRadio(id);

  const { size, error = false } = attributes;
  const classes = classNames({ [`text-${size}`]: size, "p-invalid": error });
  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel={false}>
      <SelectButton
        inputid={address?.component}
        value={getValue()[0] || null}
        options={model.values}
        onChange={onChangeButtonRadio}
        itemTemplate={itemTemplate}
        className={classes}
      />
    </AweCriterion>
  );
}

AweButtonRadio.propTypes = {
  id: PropTypes.string,
};

export default AweButtonRadio;
