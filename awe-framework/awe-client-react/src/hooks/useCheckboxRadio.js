import React, {useCallback, useMemo} from "react";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import {classNames} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";
import useComponent from "./useComponent";

/**
 * useCheckboxRadio
 * Shared logic for checkbox/radio criteria components.
 * Provides helpers to read selection and to update selection depending on the control type.
 *
 * Usage:
 *   const { address, model, attributes, validationRules, getChecked, getValue, itemTemplate,
 *           onChangeCheckbox, onChangeButtonRadio, onChangeButtonCheckbox } = useCheckboxRadio(id);
 */
export default function useCheckboxRadio(id) {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { address } = useComponent(id);
  const { model = { values: [] }, attributes = {}, validationRules = {} } = useSelector(state => ({
    model: state.components[id]?.model || { values: [] },
    attributes: state.components[id]?.attributes || {},
    validationRules: state.components[id]?.validationRules || {}
  }));

  const values = model.values || [];

  const getChecked = useCallback(() => values.filter(v => v.selected).length > 0, [values]);

  const getValue = useCallback(() => values.filter(item => item.selected).map(item => item.value), [values]);

  const onChangeCheckbox = useCallback((e) => {
    const checked = !!e.target?.checked;
    dispatch(updateThunk(address, {
      values: values.map(d => ({ ...d, selected: checked }))
    }));
  }, [dispatch, address, values]);

  const onChangeButtonRadio = useCallback((e) => {
    const selectedValue = e.value;
    dispatch(updateThunk(address, {
      values: values.map(item => ({ ...item, selected: selectedValue === item.value }))
    }));
  }, [dispatch, address, values]);

  const onChangeButtonCheckbox = useCallback((e) => {
    const selectedValues = Array.isArray(e.value) ? e.value : [];
    dispatch(updateThunk(address, {
      values: values.map(item => ({ ...item, selected: selectedValues.includes(item.value) }))
    }));
  }, [dispatch, address, values]);

  const itemTemplate = useCallback((option) => {
    const { icon, label, style } = option || {};
    const { size } = attributes || {};
    const iconTemplate = getIconCode(icon);
    const labelTemplate = label ? <span>{translateLabel(label, t)}</span> : null;
    return <span className={classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style)}>
      {iconTemplate}
      {labelTemplate}
    </span>;
  }, [attributes, t]);

  return useMemo(() => ({
    address,
    model,
    attributes,
    validationRules,
    getChecked,
    getValue,
    itemTemplate,
    onChangeCheckbox,
    onChangeButtonRadio,
    onChangeButtonCheckbox
  }), [
    address,
    model,
    attributes,
    validationRules,
    getChecked,
    getValue,
    itemTemplate,
    onChangeCheckbox,
    onChangeButtonRadio,
    onChangeButtonCheckbox]);
}
