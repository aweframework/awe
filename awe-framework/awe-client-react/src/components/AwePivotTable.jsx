import React, {useEffect, useState} from "react";
import PivotTableUI from 'react-pivottable/PivotTableUI';
import 'react-pivottable/pivottable.css';
import {extractCellValue} from "../utilities/grid";
import {isEmpty} from "../utilities/general";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {updateAttributes} from "../redux/actions/components";

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
  const { address, model, attributes = {} } = useSelector(state => ({
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes
  }));
  const [rendered, setRendered] = useState(false);



  /**
   * On attributes change
   * @param e Changed attributes
   */
  const onChange = (e) => {
    const {cols, rows, vals} = e;
    dispatch(updateAttributes(address, {
      ...e,
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
    const {cols, rows, vals, style} = attributes;
    let fixedAttributes = rendered ? {
      ...attributes,
      vals: readValueList(vals),
      cols: readValueList(cols),
      rows: readValueList(rows)
    } : {};
    return (<div className={style}>
      <PivotTableUI
        data={model.values.map(row => Object.entries(row).reduce((prev, [key, value]) =>
          ({...prev, [key]: extractCellValue(value)}), {}))}
        onChange={onChange}
        unusedOrientationCutoff={Infinity}
        {...fixedAttributes}
      />
    </div>);
}

export default AwePivotTable;
