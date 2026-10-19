import React from "react";
import {Badge} from 'primereact/badge';
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {getIconCode, getVisibleTextData, translateLabel} from "../utilities";
import "./ColumnTextView.less";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";
import {updateModelWithDependencies as updateThunk} from "../redux/thunks/components";
import {addActionsTop} from "../redux/actions/actions";
import {renderSafeHtml} from "../utilities/sanitizeHtml";
import {getFirstDefinedAndNotNullValue} from "../utilities/general";

/**
 * Text of the cell. An html label (formatted-text column) is sanitized and not translated, as in the AngularJS client
 * @param {object} data Cell data
 * @param {boolean} html Whether the label is html
 * @param {function} t Translate function
 * @returns {React.ReactNode} Text of the cell
 */
function getCellText({label, value}, html, t) {
  return html ? renderSafeHtml(getFirstDefinedAndNotNullValue(label, value, "")) : getVisibleTextData(label || value, t);
}

/**
 * Tooltip of the cell. The markup of an html label is not a valid tooltip, so it only uses the title
 * @param {object} data Cell data
 * @param {boolean} html Whether the label is html
 * @param {function} t Translate function
 * @returns {string|undefined} Tooltip of the cell
 */
function getCellTooltip({title, label}, html, t) {
  if (html) {
    return title ? translateLabel(title, t) : undefined;
  }
  return translateLabel(title || label, t);
}

function ColumnTextView(props) {
  const { data, align, icon: propIcon, unit: propUnit, address, actions = [], html = false } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));

  const {style} = data;
  const icon = data.icon || propIcon || undefined;
  const unit = data.unit || propUnit || null;

  const onAction = (event) => {
    // Change event
    updateModelWithDependencies(address, {event});
    if (actions.length > 0) {
      dispatch(addActionsTop(actions.map(action => ({...action, address: {...address}}))));
    }
  };

  const onClick = () => onAction("click");
  const onKeyDown = () => onAction("keyDown");

  return (
    <button className={classNames("text-view", style)} title={getCellTooltip(data, html, t)} onClick={onClick} onKeyDown={onKeyDown}>
      <span className={classNames("text-view-icon", style)}>{getIconCode(icon, "fa-fw")}</span>
      <span className={classNames("text-view-text", style)} style={{textAlign: align}}>{getCellText(data, html, t)}</span>
      {unit && <Badge value={translateLabel(unit, t)} severity="secondary" style={{justifyContent: "center"}}/>}
    </button>
  );
}

ColumnTextView.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  align: PropTypes.string,
  icon: PropTypes.string,
  actions: PropTypes.array,
  style: PropTypes.string,
  unit: PropTypes.string,
  html: PropTypes.bool,
};

export default ColumnTextView;
