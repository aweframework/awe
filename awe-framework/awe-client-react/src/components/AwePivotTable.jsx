import React, {useEffect, useState} from "react";
import PivotTableUI from 'react-pivottable/PivotTableUI';
import 'react-pivottable/pivottable.css';
import {extractCellValue} from "../utilities/grid";
import {isEmpty} from "../utilities/general";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {buildPivotProps, XML_ONLY_ATTRIBUTES} from "./pivot/pivotOptions";
import {useComponentState} from "../hooks/useComponentState";
import {updateAttributes} from "../redux/actions/components";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

/**
 * Read value list
 * @param value
 * @returns {*}
 */
function readValueList(value) {
  return isEmpty(value) ? [] : value.split(",");
}

/**
 * Write value list
 * @param value
 * @returns {*}
 */
function writeValueList(value) {
  return value.join(",");
}

/**
 * AWE Pivot Table component
 * @category Components
 */
function AwePivotTable(props) {

  const { id, } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { address, model, attributes = {} } = useComponentState(id);
  const settings = useSelector(selectSettings);
  const [rendered, setRendered] = useState(false);


  /**
   * On attributes change
   * @param e Changed attributes
   */
  const onChange = (e) => {
    // Sorters are derived from sort-method and set-pivot-sorters: keep the stored orders
    const { cols, rows, vals, sorters, ...changed } = e;
    dispatch(updateAttributes(address, {
      ...changed,
      vals: writeValueList(vals),
      cols: writeValueList(cols),
      rows: writeValueList(rows)
    }));
  };

  // Initialize on mount
  useEffect(() => {
    setRendered(true);
  }, []);

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  const { cols, rows, vals, style, visible = true } = attributes;
  let fixedAttributes = {};
  if (rendered) {
    // XML values are initial values only: later user choices (kept in attributes) win
    const { aggregators, menuLimit, sorters, ...initial } = buildPivotProps(attributes, settings);
    const userAttributes = Object.fromEntries(Object.entries(attributes)
      .filter(([name]) => !XML_ONLY_ATTRIBUTES.includes(name)));
    fixedAttributes = {
      ...initial,
      ...userAttributes,
      ...(vals !== undefined && { vals: readValueList(vals) }),
      cols: readValueList(cols),
      rows: readValueList(rows),
      aggregators,
      menuLimit,
      sorters
    };
  }
  const classes = classNames(style, { "hidden": !visible });
  return (<div className={classes}>
    <PivotTableUI
      data={model.values.map(row => Object.entries(row).reduce((prev, [key, value]) =>
        ({ ...prev, [key]: extractCellValue(value) }), {}))}
      onChange={onChange}
      unusedOrientationCutoff={Infinity}
      {...fixedAttributes}
    />
  </div>);
}

AwePivotTable.propTypes = {
  id: PropTypes.string,
};

export default AwePivotTable;
