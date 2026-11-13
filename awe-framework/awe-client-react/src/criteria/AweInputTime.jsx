import React, {useCallback} from "react";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {Calendar} from "primereact/calendar";
import {fromTime, getTimeOptions, toTime} from "../utilities/dates";
import {classNames} from "../utilities/components";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import PropTypes from "prop-types";
import useEnterKey from "../hooks/useEnterKey";
import {calendarPassThrough} from "../utilities/testPassThrough";

function AweInputTime(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  // Only the setting the component reads: any other change of the settings does not render it again
  const language = useSelector(state => state.settings?.language);
  const { address, attributes, validationRules, value, onSubmit } = useText(id);

  const timeFormat = attributes.timeFormat;
  const { hourFormat, showSeconds } = getTimeOptions(timeFormat);

  const onChange = useCallback((e) => {
    const newDate = e?.value ?? e?.target?.value ?? null;
    const fixed = fromTime(newDate, timeFormat) || "";
    dispatch(updateThunk(address, { values: [{ value: fixed, label: fixed, selected: true }] }));
  }, [dispatch, address, timeFormat]);

  const onKeyDown = useEnterKey(onSubmit);

  const {required, readonly, size, error } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <Calendar
        id={address?.component}
        value={toTime(value)}
        className={classes}
        placeholder={criterionPlaceholder(attributes, validationRules, t)}
        onChange={onChange}
        required={required}
        disabled={readonly}
        locale={language}
        pt={calendarPassThrough(address?.component, onKeyDown, undefined, requiredAria(attributes, validationRules))}
        timeOnly
        hourFormat={hourFormat}
        showSeconds={showSeconds}
      />
    </AweCriterion>
  );
}

AweInputTime.propTypes = {
  id: PropTypes.string,
};

export default AweInputTime;
