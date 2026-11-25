import React from "react";
import {SelectButton} from "primereact/selectbutton";
import useCheckboxRadio from "../hooks/useCheckboxRadio";
import AweCriterion from "./AweCriterion";
import classNames from "classnames";
import PropTypes from "prop-types";
import { buttonGroupPassThrough } from "../utilities/testPassThrough";
import { useTranslation } from "react-i18next";
import { translateLabel } from "../utilities";
import AweChoiceCards from "./AweChoiceCards";
import AweChips from "./AweChips";

function AweButtonRadio(props) {
  const { id } = props;
  const { t } = useTranslation();
  const { address, model, attributes, validationRules, getValue, optionLabel, itemTemplate, onChangeButtonRadio } = useCheckboxRadio(id);

  const { size, error = false, initialLoad, style = "", label, readonly } = attributes;
  const classes = classNames({ [`text-${size}`]: size, "p-invalid": error });
  // A criterion with options (initial load enum or query, or more than one value) shows its label above its buttons, as
  // any other criterion; a criterion without options is one button whose text is its label
  const hasOptions = Boolean(initialLoad) || (model.values || []).length > 1;
  const styles = String(style).split(/\s+/);
  // style="awe-chips": the options are pills that wrap, the chosen one with a tick (#993, T37)
  if (hasOptions && styles.includes("awe-chips")) {
    const chosen = getValue()[0];
    return (
      <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel>
        <AweChips
          id={address?.component}
          mode="radio"
          options={model.values || []}
          selected={chosen === undefined || chosen === null ? [] : [chosen]}
          onChange={values => onChangeButtonRadio({value: values[0]})}
          label={translateLabel(label, t)}
          readonly={Boolean(readonly)}
        />
      </AweCriterion>
    );
  }
  // style="awe-choice-cards": the options are cards with their icon, label and description (#993)
  if (hasOptions && styles.includes("awe-choice-cards")) {
    return (
      <AweCriterion address={address} attributes={attributes} validationRules={validationRules} generateLabel>
        <AweChoiceCards
          id={address?.component}
          options={model.values || []}
          value={getValue()[0] ?? null}
          onChange={onChangeButtonRadio}
          label={translateLabel(label, t)}
          readonly={Boolean(readonly)}
        />
      </AweCriterion>
    );
  }
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
