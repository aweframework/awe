import React from "react";
import {Panel} from 'primereact/panel';
import {useTranslation} from 'react-i18next';
import "./AweWindow.less";
import {getHelpTooltipNode, getIconCode, getIconNode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import {TestIds, testHook} from "../utilities/testIds";
import useMaximize from "../hooks/useMaximize";
import {AdvancedSearchContext, useAdvancedSearch} from "./AweWindowAdvanced";
import {useComponentState} from "../hooks/useComponentState";
import PropTypes from "prop-types";

function AweWindow(props) {
  const {id, style, help, helpImage, label, icon, expand, maximize, elementList = [],
    advancedMode, advancedCriteria, searchCriteria, advancedOpen} = props;
  // The window grows over the maximize target like a grid does, and its siblings fade out
  const {ref: windowRef, iconMaximized, style: maximizeStyle, className: maximizeClassName, toggle: toggleMaximized} = useMaximize();
  const { t } = useTranslation();
  // Advanced search (#790): a window that holds an advanced block shares its state with it, and the block draws the
  // disclosure row, the filters in effect and the advanced criteria where it is
  // The state the user left is stored in the controller of the window (the component in the store), which the screen
  // structure the window is rendered from does not carry, as whether it keeps saved searches
  const {attributes: {advancedOpen: storedOpen, savedSearches} = {}} = useComponentState(id) || {};
  const advanced = useAdvancedSearch({id, label, advancedMode, advancedCriteria, searchCriteria,
    advancedOpen: storedOpen ?? advancedOpen, savedSearches});

  const getIcon = () => {
    if (icon) {
      return getIconCode(icon, "fa-fw");
    }
    return null;
  };

  const isMaximizable = maximize === true || maximize === "true";

  const getMaximizeButton = () => {
    if (!isMaximizable) {
      return null;
    }
    const title = t(iconMaximized ? "SCREEN_TEXT_RESTORE" : "SCREEN_TEXT_MAXIMIZE");
    return <button type="button" className="maximize-button p-link" title={title} aria-label={title}
      onClick={toggleMaximized} {...testHook(TestIds.windowMaximizeButton)}>
      {getIconNode(iconMaximized ? "pi-window-minimize" : "pi-window-maximize")}
    </button>;
  };

  const getIcons = () => <>{getIcon()}{getMaximizeButton()}</>;

  const getHelpIcon = () => {
    if (help || helpImage) {
      return getIconNode("pi-question-circle", {role: "note", className: "help-icon"});
    } else {
      return null;
    }
  };

  const getHeader = () => {
    return <div className={`help-target-${id}`}>{translateLabel(label, t)} {getHelpIcon()}</div>;
  };

  const classes = classNames("m-2", `expandible-${expand || "vertical"}`, style, maximizeClassName);

  const panel = <>
    {getHelpTooltipNode(help, helpImage, t,`.help-target-${id}`)}
    <Panel
      id={id}
      window-id={id}
      className={classes}
      style={maximizeStyle}
      pt={{root: {ref: windowRef, ...testHook(TestIds.window, {owner: id, maximized: iconMaximized})}}}
      header={getHeader()}
      icons={getIcons()}>
      {elementList.map((node, index) => Components(node, index))}
    </Panel>
  </>;

  if (!advanced) {
    return panel;
  }
  return <AdvancedSearchContext.Provider value={advanced}>{panel}</AdvancedSearchContext.Provider>;
}

AweWindow.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  expand: PropTypes.string,
  maximize: PropTypes.oneOfType([PropTypes.bool, PropTypes.string]),
  label: PropTypes.string,
  icon: PropTypes.string,
  help: PropTypes.string,
  helpImage: PropTypes.string,
  elementList: PropTypes.array,
  advancedMode: PropTypes.string,
  advancedCriteria: PropTypes.array,
  searchCriteria: PropTypes.array,
  advancedOpen: PropTypes.bool
};

export default AweWindow;
