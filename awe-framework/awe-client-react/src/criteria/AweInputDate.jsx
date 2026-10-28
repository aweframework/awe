import React, {useCallback} from "react";
import {criterionPlaceholder, requiredAria} from "../utilities/labels";
import {Calendar} from "primereact/calendar";
import {fromDate, toDate} from "../utilities/dates";
import useDateOptions from "../hooks/useDateOptions";
import {classNames} from "../utilities/components";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import AweCriterion from "./AweCriterion";
import useText from "../hooks/useText";
import PropTypes from "prop-types";
import useEnterKey from "../hooks/useEnterKey";
import {calendarPassThrough} from "../utilities/testPassThrough";

function AweInputDate(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const settings = useSelector(state => state.settings);
  const { address, attributes, validationRules, value, onSubmit } = useText(id);

  const { format, language, calendarProps } = useDateOptions(attributes, value, settings.language);

  const onChange = useCallback((e) => {
    const newDate = e?.value ?? e?.target?.value ?? null;
    const fixed = fromDate(newDate, format, language) || "";
    dispatch(updateThunk(address, { values: [{ value: fixed, label: fixed, selected: true }] }));
  }, [dispatch, address, format, language]);

  const onKeyDown = useEnterKey(onSubmit);

  const {required, readonly, size, error } = attributes;
  const classes = classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, "p-invalid": error });

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <Calendar
        id={address?.component}
        value={toDate(value, format, language)}
        className={classes}
        placeholder={criterionPlaceholder(attributes, validationRules, t)}
        onChange={onChange}
        required={required}
        disabled={readonly}
        locale={settings.language}
        pt={calendarPassThrough(address?.component, onKeyDown, undefined, requiredAria(attributes, validationRules))}
        showButtonBar
        {...calendarProps}
      />
    </AweCriterion>
  );
}

AweInputDate.propTypes = {
  id: PropTypes.string,
};

export default AweInputDate;
