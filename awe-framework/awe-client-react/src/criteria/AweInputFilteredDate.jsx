import React, {useCallback, useEffect, useState} from "react";
import {Calendar} from "primereact/calendar";
import {translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {fromDate, getAvailableDates, getDisabledDates, getMaxDate, getMinDate, toDate} from "../utilities/dates";
import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import PropTypes from "prop-types";

function AweInputFilteredDate(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { address, model, attributes = {}, validationRules = {}, settings } = useText(id);

  const [disabledDates, setDisabledDates] = useState([]);
  const [maxDate, setMaxDate] = useState(null);
  const [minDate, setMinDate] = useState(null);

  const onChange = useCallback((e) => {
    const newDate = e?.value ?? e?.target?.value ?? null;
    const value = fromDate(newDate) || "";
    dispatch(updateThunk(address, { values: [{ value, label: value, selected: true }] }));
  }, [dispatch, address]);

  const onClear = useCallback((e) => {
    dispatch(updateThunk(address, { values: model.values.map(item => ({...item, selected: false }))}));
  }, [dispatch, address]);

  useEffect(() => {
    const availableDates = getAvailableDates(model?.values || []);
    setDisabledDates(getDisabledDates(availableDates));
    setMaxDate(getMaxDate(availableDates));
    setMinDate(getMinDate(availableDates));
  }, [model?.values]);

  const { placeholder, required, readonly, size, error } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  const selected = (model.values || []).find(v => v.selected)?.value || null;

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <Calendar
        id={address?.component}
        value={toDate(selected)}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={onChange}
        onClearButtonClick={onClear}
        dateFormat="dd/mm/yy"
        disabledDates={disabledDates}
        maxDate={maxDate}
        minDate={minDate}
        required={required}
        disabled={readonly}
        locale={settings.language}
        showButtonBar
      />
    </AweCriterion>
  );
}

AweInputFilteredDate.propTypes = {
  id: PropTypes.string,
};

export default AweInputFilteredDate;
