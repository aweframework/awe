import React, { useCallback, useMemo, useRef } from "react";
import { createPortal } from "react-dom";
import { Button } from "primereact/button";
import { Badge } from "primereact/badge";
import { OverlayPanel } from "primereact/overlaypanel";
import { classNames, clickDropdown } from "../utilities/components";

import "./AweInfoDropdown.less";
import { getIconCode, translateLabel } from "../utilities";
import { Components } from "../utilities/structure";
import { useDispatch } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { useTranslation } from "react-i18next";
import { addActionsTop } from "../redux/actions/actions";
import { updateModelWithDependencies } from "../redux/thunks/components";
import PropTypes from "prop-types";
import AweWindow from "./AweWindow";
import { TestIds, testHook } from "../utilities/testIds";

function AweInfoDropdown(props) {
  const { id, elementList = [] } = props;
  const { address, model = { values: [] }, attributes = {}, actions = [] } = useComponentState(id);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const opRef = useRef(null);
  // The children are mounted once, in a container of their own, and the container moves: it waits hidden in the page
  // while the panel is closed (so their autoload runs without opening it, as the AngularJS client keeps the content of
  // the dropdown in the page) and it moves into the panel while it is shown. They never remount, so opening or closing
  // the dropdown launches no query
  const container = useMemo(() => document.createElement("div"), []);
  const hostRef = useRef(null);
  const parkContainer = useCallback(host => {
    hostRef.current = host;
    if (host && !container.parentNode) {
      host.appendChild(container);
    }
  }, [container]);
  const showContainer = useCallback(placeholder => {
    // The panel shows the container while it is mounted and gives it back to the host when it unmounts
    (placeholder || hostRef.current)?.appendChild(container);
  }, [container]);

  const { values = [] } = model;
  const { style, dropdownStyle, icon, disabled, label, size, unit, visible } = attributes;
  const classes = classNames("p-button-rounded", "p-button-text", "p-button-secondary", { [`p-button-${size}`]: size }, style, { "hidden": !visible });

  const computedLabel = values.length ? values[0].label : label;
  const computedUnit = unit && <Badge value={unit} severity="info" />;

  const onClick = useCallback((e) => {
    if ((elementList || []).length > 0) {
      // The dropdown opens and the actions of the info run too (the notification bell filters its list on click)
      clickDropdown(e, { addActionsTop, dispatch, updateModelWithDependencies, address, actions }, opRef.current);
    } else if ((actions || []).length > 0) {
      dispatch(addActionsTop(actions.map(action => ({ ...action, address }))));
    }
  }, [actions, address, dispatch, elementList]);

  return <>
    <span className="p-overlay-badge">
      <Button
        id={address?.component}
        type="button"
        className={classes}
        icon={getIconCode(icon, "p-button-icon p-c p-button-icon-left")}
        disabled={disabled}
        label={translateLabel(computedLabel, t)}
        iconPos={"left"}
        onClick={onClick}
        {...testHook(TestIds.infoDropdown)}
      />
      {computedUnit}
    </span>
    {elementList.length > 0 && <div hidden aria-hidden="true" ref={parkContainer}>
      {createPortal(elementList.map((node, index) => Components(node, index)), container)}
    </div>}
    <OverlayPanel ref={opRef} dismissable className={classNames("info-dropdown", dropdownStyle)}
      pt={{ root: testHook(TestIds.infoDropdownMenu, { owner: address?.component }) }}>
      <div ref={showContainer} />
    </OverlayPanel>
  </>;
}

AweInfoDropdown.propTypes = {
  id: PropTypes.string,
  elementList: PropTypes.array
};

export default AweInfoDropdown;
