import React, {useCallback, useMemo, useState} from "react";
import {Calendar} from "primereact/calendar";
import {Button} from "primereact/button";
import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import PropTypes from "prop-types";
import {translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {fromDate, parsePresets, resolvePreset, toDate} from "../utilities/dates";
import {TestIds, testHook} from "../utilities/testIds";
import {calendarPassThrough} from "../utilities/testPassThrough";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import useText from "../hooks/useText";
import AweCriterion from "./AweCriterion";
import "./AweInputDateRange.less";

const DATE_FORMAT = "dd/mm/yy";

/**
 * Build the model values of a range
 * @param {string} from Start date
 * @param {string} to End date
 * @returns {{values: object[]}} Model data
 */
const toModel = (from, to) => ({
  values: [{value: from, label: from, selected: true}, {value: to, label: to, selected: true}]
});

/**
 * Date range criterion: a start and an end date (sent to the server as two parameters) and quick presets
 * @param {object} props Properties
 * @returns {JSX.Element} Criterion
 */
function AweInputDateRange(props) {
  const {id} = props;
  const dispatch = useDispatch();
  const {t} = useTranslation();
  const {address, model, attributes = {}, validationRules = {}, settings} = useText(id);
  const [applied, setApplied] = useState(null);

  const {presets, placeholder, readonly, size, error} = attributes;
  const [from = "", to = ""] = (model?.values || []).filter(value => value.selected).map(value => value.value || "");
  const codes = useMemo(() => parsePresets(presets).filter(code => resolvePreset(code) !== null), [presets]);

  const update = useCallback((newFrom, newTo) => {
    dispatch(updateThunk(address, toModel(newFrom, newTo)));
  }, [dispatch, address]);

  const onChangeFrom = useCallback((e) => update(fromDate(e?.value ?? null) || "", to), [update, to]);
  const onChangeTo = useCallback((e) => update(from, fromDate(e?.value ?? null) || ""), [update, from]);

  const applyPreset = useCallback((code) => {
    const range = resolvePreset(code);
    setApplied({code, ...range});
    update(range.from, range.to);
  }, [update]);

  // A preset stays active only while the dates are the ones it set, so editing a date (or resetting) clears it
  const isActive = (code) => applied?.code === code && applied.from === from && applied.to === to;

  const classes = classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error});
  const passThrough = (testId) => ({
    ...calendarPassThrough(address?.component),
    input: {root: testHook(testId)}
  });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules} groupClass="date-range">
      <div className="p-inputgroup date-range-dates">
        <Calendar
          id={address?.component}
          value={toDate(from)}
          className={classes}
          placeholder={translateLabel(placeholder, t) || t("DATE_RANGE_FROM", {defaultValue: "From"})}
          onChange={onChangeFrom}
          dateFormat={DATE_FORMAT}
          maxDate={toDate(to) || undefined}
          disabled={readonly}
          locale={settings.language}
          pt={passThrough(TestIds.dateRangeFrom)}
          showButtonBar
        />
        <span className="p-inputgroup-addon date-range-separator"><i className="pi pi-arrow-right"/></span>
        <Calendar
          id={`${address?.component}-to`}
          value={toDate(to)}
          className={classes}
          placeholder={t("DATE_RANGE_TO", {defaultValue: "To"})}
          onChange={onChangeTo}
          dateFormat={DATE_FORMAT}
          minDate={toDate(from) || undefined}
          disabled={readonly}
          locale={settings.language}
          pt={passThrough(TestIds.dateRangeTo)}
          showButtonBar
        />
      </div>
      {codes.length > 0 && (
        <div className="date-range-presets">
          {codes.map(code => (
            <Button
              key={code}
              type="button"
              size="small"
              text={!isActive(code)}
              label={t(`DATE_RANGE_PRESET_${code}`, {defaultValue: code})}
              disabled={readonly}
              onClick={() => applyPreset(code)}
              pt={{root: testHook(TestIds.dateRangePreset, {active: isActive(code), attributes: {"option-id": code}})}}
            />
          ))}
        </div>
      )}
    </AweCriterion>
  );
}

AweInputDateRange.propTypes = {
  id: PropTypes.string,
};

export default AweInputDateRange;
