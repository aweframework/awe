import React, {useCallback, useRef} from "react";
import {OverlayPanel} from "primereact/overlaypanel";
import {classNames, clickDropdown} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";

import "./AweAvatar.less";
import {Avatar} from "primereact/avatar";
import {Badge} from "primereact/badge";
import {Components} from "../utilities/structure";
import {getFirstDefinedValue} from "../utilities/general";
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import {updateModelWithDependencies} from "../redux/thunks/components";
import PropTypes from "prop-types";

function AweAvatar(props) {
  const { id, elementList = [] } = props;
  const { address, model, attributes = {}, actions = [], globalDisabled } = useSelector(state => ({
    address: state.components[id]?.address,
    model: state.components[id]?.model,
    attributes: state.components[id]?.attributes,
    actions: state.components[id]?.actions,
    globalDisabled: state.actions.running
  }));
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const opRef = useRef(null);

  const { values = [] } = model;
  const { style, icon, disabled, label, size, unit, image, visible = true, showLabel = true } = attributes;
  const classes = classNames("p-button-rounded", "p-button-text", "p-button-secondary", { [`p-button-${size}`]: size }, {"hidden": !visible});

  const computedLabel = getFirstDefinedValue(values?.[0]?.label, label);
  const computedImage = getFirstDefinedValue(values?.[0]?.image, image);
  const computedIcon = getFirstDefinedValue(values?.[0]?.icon, icon);
  const computedUnit = getFirstDefinedValue(values?.[0]?.unit, unit);
  const unitBadge = computedUnit && <Badge value={computedUnit}/>;
  const labelSpan = showLabel && <span className={"avatar-name"} >{translateLabel(computedLabel, t)}</span>;

  const onClick = useCallback((e) => {
    if ((actions || []).length > 0) {
      dispatch(addActionsTop(actions.map(action => ({ ...action, address }))));
    } else if ((elementList || []).length > 0) {
      clickDropdown(e, {addActionsTop, dispatch, updateModelWithDependencies, address, actions}, opRef.current);
    }
  }, [actions, address, dispatch, elementList, props]);

  const dropdown = actions.length === 0 && elementList.length > 0 && (
    <OverlayPanel ref={opRef} dismissable className={"info-dropdown"}>
      {elementList.map((node, index) => Components(node, index))}
    </OverlayPanel>
  );

  return (<>
    <div className={`avatar-component p-overlay-badge ${style}`} onClick={onClick} role="button" onKeyDown={onClick}>
      <Avatar
        id={address.component}
        type="button"
        className={classes}
        shape={"circle"}
        image={computedImage}
        icon={!computedImage && getIconCode(computedIcon, "p-button-icon p-c p-button-icon-left")}
        disabled={globalDisabled || disabled}
        label={!computedImage && !computedIcon && (computedLabel || "").charAt(0).toUpperCase()}
        title={translateLabel(computedLabel, t)}>
        {unitBadge}
      </Avatar>
      {labelSpan}
    </div>
    {dropdown}
  </>);
}

AweAvatar.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweAvatar;
