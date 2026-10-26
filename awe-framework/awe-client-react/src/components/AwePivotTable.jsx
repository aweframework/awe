import React, {useEffect, useState} from "react";
import PivotTableUI from 'react-pivottable/PivotTableUI';
import 'react-pivottable/pivottable.css';
import TableRenderers from 'react-pivottable/TableRenderers';
import TableBarchartRenderer from "./pivot/TableBarchartRenderer";
import {extractCellValue} from "../utilities/grid";
import {isEmpty} from "../utilities/general";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {buildPivotProps, XML_ONLY_ATTRIBUTES} from "./pivot/pivotOptions";
import {useComponentState} from "../hooks/useComponentState";
import {updateAttributes} from "../redux/actions/components";
import {classNames} from "../utilities/components";
import {useContextMenu} from "../hooks/useContextMenu";
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

/** Props that react-pivottable gives back and that are not stored as attributes (data and derived props) */
const NOT_STORED = ["data", "sorters", "aggregators", "renderers", "menuLimit", "onChange"];

/** react-pivottable renderers plus the AngularJS "Table Barchart" */
const RENDERERS = {...TableRenderers, "Table Barchart": TableBarchartRenderer};

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
  const { onContextMenu, contextMenuTemplate } = useContextMenu({ address, contextMenu: attributes.contextMenu });


  /**
   * On attributes change
   * @param e Changed attributes
   */
  const onChange = (e) => {
    // Keep only the pivot state chosen by the user: the data comes from the model (a stored copy would hide reloads),
    // and the sorters, aggregators, renderers and callbacks are derived on every render
    const { cols, rows, vals } = e;
    const changed = Object.fromEntries(Object.entries(e).filter(([name]) => !NOT_STORED.includes(name)));
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
  return (<div className={classes} onContextMenu={onContextMenu}>
    <PivotTableUI
      data={model.values.map(row => Object.entries(row).reduce((prev, [key, value]) =>
        ({ ...prev, [key]: extractCellValue(value) }), {}))}
      onChange={onChange}
      unusedOrientationCutoff={Infinity}
      renderers={RENDERERS}
      {...fixedAttributes}
    />
    {contextMenuTemplate()}
  </div>);
}

AwePivotTable.propTypes = {
  id: PropTypes.string,
};

export default AwePivotTable;
