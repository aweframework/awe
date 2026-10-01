import React, { useCallback, useMemo, useState } from "react";
import { InputNumber } from "primereact/inputnumber";
import { translateLabel } from "../utilities";
import { formatNumber, translateNumberFormat } from "../utilities/numbers";
import { classNames } from "../utilities/components";
import { isEmpty } from "../utilities/general";

import "./AweInputNumeric.less";
import { Slider } from "primereact/slider";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import { useDispatch } from "react-redux";
import { updateModelWithDependencies as updateThunk } from "../redux/thunks/components";
import { useTranslation } from "react-i18next";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";

/**
 * Normalize the text-model value into a number the numeric input can compare against.
 * Must go through isEmpty() instead of a raw `Number(value)` call: Number('') is 0,
 * so an empty model value would otherwise be indistinguishable from an actual 0,
 * silently dropping the first '0' a user types into a blank required field.
 * @param {*} value Raw value coming from the text model (string, number, null or undefined)
 * @return {number|null} Parsed number, or null when the model has no value
 */
function getModelNumberValue(value) {
  return isEmpty(value) ? null : Number(value);
}

function AweInputNumeric(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { address, attributes = {}, validationRules = {}, value: valueFromModel } = useText(id);
  const { t } = useTranslation();

  const [sliding, setSliding] = useState(false);
  const [number, setNumber] = useState(Number(valueFromModel));

  const { placeholder, required, readonly, numberFormat, size, align, icon, unit, error = false, showSlider = false } = attributes;
  const classes = classNames({ "with-icon": icon, "with-unit": unit, "with-slider": showSlider, [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  const nf = useMemo(() => translateNumberFormat(numberFormat), [numberFormat]);

  const currentValue = sliding ? number : getModelNumberValue(valueFromModel);

  const commit = useCallback((val) => {
    const v = val == null || val === "" ? null : Number(val);
    dispatch(updateThunk(address, {
      values: [{ value: v, label: formatNumber(v, numberFormat), selected: true }]
    }));
  }, [dispatch, address, numberFormat]);

  const onValueChange = useCallback((e) => {
    const v = sliding ? number : e.value;
    if (v !== getModelNumberValue(valueFromModel)) {
      commit(v);
      setSliding(false);
    }
  }, [sliding, number, valueFromModel, commit]);

  const onSlide = useCallback((e) => {
    if (!readonly) {
      setNumber(Number(e.value));
      setSliding(true);
      if (e.originalEvent?.type === "click") onValueChange(e);
    }
  }, [readonly, onValueChange]);

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} groupClass={showSlider ? "" : "p-inputgroup"}
      generateIcon={!showSlider} generateUnit={!showSlider}>
      <InputNumber
        id={address?.component}
        value={currentValue}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        required={required}
        disabled={readonly}
        mode="decimal"
        locale={nf.locale}
        useGrouping={nf.useGrouping}
        maxFractionDigits={nf.maxFractionDigits}
        minFractionDigits={nf.minFractionDigits}
        min={nf.min}
        max={nf.max}
        suffix={nf.suffix}
        onValueChange={onValueChange}
        inputStyle={{ textAlign: align || "right" }}
        pt={{ input: { root: testHook(TestIds.criterionInput) } }}
        onKeyDown={e => e.key === "Enter" && commit(currentValue)}
      />
      {showSlider ? (
        <Slider
          value={Number(currentValue || 0)}
          disabled={readonly}
          min={nf.min}
          max={nf.max}
          step={nf.step}
          onChange={onSlide}
          onSlideEnd={onValueChange}
        />
      ) : null}
    </AweCriterion>
  );
}

AweInputNumeric.propTypes = {
  id: PropTypes.string,
};

export default AweInputNumeric;
