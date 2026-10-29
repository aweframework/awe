import React, { useCallback, useRef } from "react";
import { OverlayPanel } from "primereact/overlaypanel";
import { classNames, clickDropdown } from "../utilities/components";
import {getContextPath, getIconCode, getIconNode, translateLabel} from "../utilities";

import "./AweAvatar.less";
import { Avatar } from "primereact/avatar";
import { Badge } from "primereact/badge";
import { Components } from "../utilities/structure";
import { getFirstDefinedValue } from "../utilities/general";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { addActionsTop } from "../redux/actions/actions";
import { updateModelWithDependencies } from "../redux/thunks/components";
import PropTypes from "prop-types";
import useComponent from "../hooks/useComponent";
import { TestIds, testHook } from "../utilities/testIds";
import { getInitials } from "../utilities/initials";

// The initials live in the utilities, shared with the avatar of the grid cells (#793)
export { getInitials };

function AweAvatar(props) {
  const { id, elementList = [] } = props;
  const { address } = useComponent(id);
  const { model = {}, attributes = {}, actions = [] } = useComponentState(id);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const opRef = useRef(null);

  const { values = [] } = model;
  const { style, icon, disabled, label, size, unit, image, visible = true, showLabel = true } = attributes;
  const classes = classNames("p-button-rounded", "p-button-text", "p-button-secondary", { [`p-button-${size}`]: size }, { "hidden": !visible });

  const computedLabel = getFirstDefinedValue(values?.[0]?.label, label);
  // The name of the value (the name of the user, for example) is the text of the avatar; the label stays as its hint
  const shownName = translateLabel(values?.[0]?.name || computedLabel, t);
  const description = values?.[0]?.description;
  const opensDropdown = actions.length === 0 && elementList.length > 0;
  const imagePath = getFirstDefinedValue(values?.[0]?.image, image);
  // Without an image the avatar shows its icon or the initial of its label, never a request to a "null" image
  const computedImage = imagePath ? `${getContextPath()}${imagePath}` : null;
  const computedIcon = getFirstDefinedValue(values?.[0]?.icon, icon);
  const computedUnit = getFirstDefinedValue(values?.[0]?.unit, unit);
  const unitBadge = computedUnit && <Badge value={computedUnit} />;
  // The label, and the description of the value as a second line (the profile of the user, for example)
  const labelSpan = showLabel && (
    <span className="avatar-text">
      <span className="avatar-name" {...testHook(TestIds.avatarName)}>{shownName}</span>
      {description && <span className="avatar-description">{translateLabel(description, t)}</span>}
    </span>
  );
  // A chevron tells that the avatar opens a dropdown, as the user menu of the prototype
  const chevron = showLabel && opensDropdown &&
    getIconNode("lucide:chevron-down", { className: "avatar-chevron", "aria-hidden": "true" });

  const onClick = useCallback((e) => {
    if ((actions || []).length > 0) {
      dispatch(addActionsTop(actions.map(action => ({ ...action, address }))));
    } else if ((elementList || []).length > 0) {
      clickDropdown(e, { addActionsTop, dispatch, updateModelWithDependencies, address, actions }, opRef.current);
    }
  }, [actions, address, dispatch, elementList, props]);

  const dropdown = opensDropdown && (
    <OverlayPanel ref={opRef} dismissable className={"info-dropdown avatar-dropdown"} pt={{ root: testHook(TestIds.infoDropdownMenu, { owner: id }) }}>
      {elementList.map((node, index) => Components(node, index))}
    </OverlayPanel>
  );

  return (<>
    <div className={`avatar-component p-overlay-badge ${style}`} onClick={onClick} role="button" onKeyDown={onClick}>
      <Avatar
        id={id}
        type="button"
        className={classes}
        shape={"circle"}
        image={computedImage}
        icon={!computedImage && getIconCode(computedIcon, "p-button-icon p-c p-button-icon-left")}
        disabled={disabled}
        label={!computedImage && !computedIcon && getInitials(shownName)}
        title={translateLabel(computedLabel, t)}
        pt={{ root: testHook(TestIds.avatar) }}>
        {unitBadge}
      </Avatar>
      {labelSpan}
      {chevron}
    </div>
    {dropdown}
  </>);
}

AweAvatar.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweAvatar;
