import React, {useCallback} from "react";
import {Calendar} from "primereact/calendar";
import {translateLabel} from "../utilities";
import {fromTime, toTime} from "../utilities/dates";
import {classNames} from "../utilities/components";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";

function AweInputTime(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const settings = useSelector(state => state.settings);
  const { address, attributes = {}, validationRules = {}, value } = useText(id);

  const onChange = useCallback((e) => {
    const newDate = e?.value ?? e?.target?.value ?? null;
    const fixed = fromTime(newDate) || "";
    dispatch(updateThunk(address, { values: [{ value: fixed, label: fixed, selected: true }] }));
  }, [dispatch, address]);

  const { placeholder, required, readonly, size, error } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <Calendar
        id={address?.component}
        value={toTime(value)}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={onChange}
        required={required}
        disabled={readonly}
        locale={settings.language}
        timeOnly
        showSeconds
      />
    </AweCriterion>
  );
}

export default AweInputTime;
