import React, {useCallback, useRef} from "react";
import {Button} from "primereact/button";
import {OverlayPanel} from "primereact/overlaypanel";
import {classNames, clickDropdown} from "../utilities/components";

import "./AweInfoDropdown.less";
import {getIconCode, translateLabel} from "../utilities";
import {Components} from "../utilities/structure";
import {useDispatch, useSelector} from "react-redux";
import {useTranslation} from "react-i18next";
import {addActionsTop} from "../redux/actions/actions";
import {updateModelWithDependencies} from "../redux/thunks/components";
import PropTypes from "prop-types";
import AweWindow from "./AweWindow";

function AweInfoDropdown(props) {
  const { id, elementList = [] } = props;
  const { address, model = { values: [] }, attributes = {}, actions = [], globalDisabled } = useSelector(state => ({
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes,
    actions: state.components[id]?.actions || [],
    globalDisabled: state.actions.running
  }));
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const opRef = useRef(null);

  const { values = [] } = model;
  const { style, icon, disabled, label, size, unit, visible } = attributes;
  const classes = classNames("p-button-rounded", "p-button-text", "p-button-secondary", { [`p-button-${size}`]: size }, style, {"hidden": !visible});

  const computedLabel = values.length ? values[0].label : label;
  const computedUnit = unit && <span className="p-badge p-badge-info">{unit}</span>;

  const onClick = useCallback((e) => {
    if ((actions || []).length > 0) {
      dispatch(addActionsTop(actions.map(action => ({ ...action, address }))));
    } else if ((elementList || []).length > 0) {
      clickDropdown(e, { addActionsTop, dispatch, updateModelWithDependencies, address, actions }, opRef.current);
    }
  }, [actions, address, dispatch, elementList]);

  return <>
    <span className="p-overlay-badge">
      <Button
        id={address?.component}
        type="button"
        className={classes}
        icon={getIconCode(icon, "p-button-icon p-c p-button-icon-left")}
        disabled={globalDisabled || disabled}
        label={translateLabel(computedLabel, t)}
        iconPos={"left"}
        onClick={onClick}
      />
      {computedUnit}
    </span>
    <OverlayPanel ref={opRef} dismissable className={"info-dropdown"}>
      {elementList.map((node, index) => Components(node, index))}
    </OverlayPanel>
  </>;
}

AweInfoDropdown.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweInfoDropdown;
