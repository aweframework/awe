import React from "react";
import { TabMenu } from "primereact/tabmenu";
import { Components } from "../utilities/structure";
import { getIconCode, getIconNode, translateLabel } from "../utilities";
import { classNames } from "../utilities/components";
import "./AweTabs.less";
import { Badge } from "primereact/badge";
import { useTranslation } from "react-i18next";
import { useInvalidValues, usePanelable } from "../hooks/usePanelable";
import { useSelector } from "react-redux";
import { selectActionsRunning } from "../redux/selectors/stateSelectors";
import { useComponentState } from "../hooks/useComponentState";
import PropTypes from "prop-types";
import { TestIds, testHook } from "../utilities/testIds";
import { tabMenuPassThrough } from "../utilities/testPassThrough";
import AweWindow from "./AweWindow";
import { useContextMenu } from "../hooks/useContextMenu";
import useMaximize from "../hooks/useMaximize";

/**
 * Tab menu disabled while actions run. It is the only part of the tabs that reads the running flag, so a running/idle
 * transition does not render the tab content again
 */
function AweTabMenu({ id, values, activeIndex, disabled, invalid, onChange, itemRenderer }) {
  const running = useSelector(selectActionsRunning);
  const menuDisabled = running || disabled;
  return <TabMenu id={id} model={values.map((item, index) => ({
    ...item,
    disabled: menuDisabled,
    template: (item) => itemRenderer(item, index)
  }))}
    activeIndex={activeIndex}
    pt={tabMenuPassThrough(values, activeIndex, menuDisabled, invalid)}
    onTabChange={onChange} />;
}

AweTabMenu.propTypes = {
  id: PropTypes.string,
  values: PropTypes.array,
  activeIndex: PropTypes.number,
  disabled: PropTypes.bool,
  invalid: PropTypes.array,
  onChange: PropTypes.func,
  itemRenderer: PropTypes.func
};

function AweTabs(props) {
  const { id, maximize, elementList = [] } = props;
  const { model = { values: [] }, address, attributes = {} } = useComponentState(id);
  const { t } = useTranslation();
  const { values, activeIndex, selectIndex } = usePanelable(model, address);
  const invalid = useInvalidValues(id);
  const { onContextMenu, contextMenuTemplate } = useContextMenu({ address, contextMenu: attributes.contextMenu });
  // The whole tab component (list and content) grows over the maximize target like a window does
  const { ref: tabsRef, iconMaximized, style: maximizeStyle, className: maximizeClassName, toggle: toggleMaximized } = useMaximize();
  const maximizable = maximize === true || maximize === "true";

  const titleRenderer = (title) => title ? <span className={`p-tab-supertitle`}>{translateLabel(title, t)}</span> : null;
  const labelRenderer = (label) => label ? <span className={`p-tab-title`} {...testHook(TestIds.tabLabel)}>{translateLabel(label, t)}</span> : null;
  const iconRenderer = (icon) => icon ? <span className="p-tab-icon">{getIconCode(icon)}</span> : null;
  const unitRenderer = (unit) => unit ? <Badge className="p-tab-unit" value={translateLabel(unit, t)}></Badge> : null;

  const onChange = (e) => {
    const { index } = e;
    selectIndex(index);
  };

  const itemRenderer = (item, itemIndex) => (
    <button className={`p-menuitem-link`} onClick={() => onChange({ index: itemIndex })} tabIndex={-1} {...testHook(TestIds.tabLink)}>
      {iconRenderer(item.icon)}
      <div className={`p-tab-text`}>
        {titleRenderer(item.title)}
        {labelRenderer(item.label)}
      </div>
      {unitRenderer(item.unit)}
    </button>
  );

  const { disabled, orientation = "horizontal" } = attributes;
  const expandible = orientation === "horizontal" ? "vertical" : "horizontal";
  const className = classNames("p-tabmenu-container", "expand", "expandible-" + expandible, "orientation-" + orientation,
    {"p-tabmenu-maximizable": maximizable}, maximizeClassName);
  const maximizeTitle = t(iconMaximized ? "SCREEN_TEXT_RESTORE" : "SCREEN_TEXT_MAXIMIZE");
  return <div className={className} criterion-id={id} ref={tabsRef} style={maximizeStyle} onContextMenu={onContextMenu}
    {...testHook(TestIds.tabs, { owner: id, maximized: iconMaximized })}>
    {maximizable && <button type="button" className="maximize-button p-link" title={maximizeTitle} aria-label={maximizeTitle}
      onClick={toggleMaximized} {...testHook(TestIds.tabMaximizeButton)}>
      {getIconNode(iconMaximized ? "pi-window-minimize" : "pi-window-maximize")}
    </button>}
    <AweTabMenu id={id} values={values} activeIndex={activeIndex} disabled={disabled} invalid={invalid}
      onChange={onChange} itemRenderer={itemRenderer} />
    {/* As in the AngularJS client, the page shown is the container whose id is the value of the selected option */}
    {elementList
      .filter(item => item.elementType === "TabContainer" && item.id === values[activeIndex]?.value)
      .map((node, index) => Components(node, index))}
    {contextMenuTemplate()}
  </div>;
}

AweTabs.propTypes = {
  id: PropTypes.string,
  maximize: PropTypes.oneOfType([PropTypes.bool, PropTypes.string]),
  elementList: PropTypes.array
};

export default AweTabs;
