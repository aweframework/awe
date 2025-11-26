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

function ColumnTextView(props) {
  const { data, align, icon: propIcon, unit: propUnit, address, actions = [] } = props;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateModelWithDependencies = (addr, payload) => dispatch(updateThunk(addr, payload));

  const {style, title, label, value} = data;
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
    <button className={classNames("text-view", style)} title={translateLabel(title || label, t)} onClick={onClick} onKeyDown={onKeyDown}>
      <span className={classNames("text-view-icon", style)}>{getIconCode(icon, "fa-fw")}</span>
      <span className={classNames("text-view-text", style)} style={{textAlign: align}}>{getVisibleTextData(label || value, t)}</span>
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
};

export default ColumnTextView;
