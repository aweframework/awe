import React from "react";
import {SelectButton} from "primereact/selectbutton";
import useCheckboxRadio from "../hooks/useCheckboxRadio";
import AweCriterion from "./AweCriterion";
import classNames from "classnames";
import PropTypes from "prop-types";
import { buttonGroupPassThrough } from "../utilities/testPassThrough";

function AweButtonCheckbox(props) {
  const { id } = props;
  const { address, model, attributes, validationRules, getValue, itemTemplate, onChangeButtonCheckbox } = useCheckboxRadio(id);

  const { size, error = false } = attributes;
  const classes = classNames("", { [`text-${size}`]: size, "p-invalid": error });
  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel={false}>
      <SelectButton
        multiple
        inputid={address?.component}
        value={getValue()}
        options={model.values}
        onChange={onChangeButtonCheckbox}
        itemTemplate={itemTemplate}
        className={classes}
        pt={buttonGroupPassThrough}
      />
    </AweCriterion>
  );
}

AweButtonCheckbox.propTypes = {
  id: PropTypes.string,
};

export default AweButtonCheckbox;
