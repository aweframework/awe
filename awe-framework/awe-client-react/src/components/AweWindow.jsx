import React, {useEffect, useRef, useState} from "react";
import {Panel} from 'primereact/panel';
import {useTranslation} from 'react-i18next';
import "./AweWindow.less";
import {getHelpTooltipNode, getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import PropTypes from "prop-types";

function AweWindow(props) {
  const {id, style, help, helpImage, label, icon, expand, maximize, elementList = []} = props;
  const [maximized, setMaximized] = useState(false);
  const [bounds, setBounds] = useState(null);
  const buttonRef = useRef(null);

  // The window is fixed over the maximize target of the template. It cannot be absolute, because a resizable
  // that contains the window is a positioning context and the window would only fill that box.
  const measureTarget = () => {
    const target = buttonRef.current?.closest(".maximize-target");
    if (!target) {
      return {top: 0, left: 0, right: 0, bottom: 0};
    }
    const {top, left, width, height} = target.getBoundingClientRect();
    return {top, left, width, height};
  };

  useEffect(() => {
    if (!maximized) {
      return undefined;
    }
    const update = () => setBounds(measureTarget());
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, [maximized]);

  const toggleMaximized = () => {
    setBounds(maximized ? null : measureTarget());
    setMaximized(!maximized);
  };
  const { t } = useTranslation();

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
    const title = t(maximized ? "SCREEN_TEXT_RESTORE" : "SCREEN_TEXT_MAXIMIZE");
    return <button type="button" className="maximize-button p-link" title={title} aria-label={title}
      ref={buttonRef} onClick={toggleMaximized}>
      <i className={`pi ${maximized ? "pi-window-minimize" : "pi-window-maximize"}`}/>
    </button>;
  };

  const getIcons = () => <>{getIcon()}{getMaximizeButton()}</>;

  const getHelpIcon = () => {
    if (help || helpImage) {
      return <i role="note" className={`help-icon pi pi-question-circle`} />;
    } else {
      return null;
    }
  };

  const getHeader = () => {
    return <div className={`help-target-${id}`}>{translateLabel(label, t)} {getHelpIcon()}</div>;
  };

  const classes = classNames("m-2", `expandible-${expand || "vertical"}`, {maximized}, style);

  return <>
    {getHelpTooltipNode(help, helpImage, t,`.help-target-${id}`)}
    <Panel
      id={id}
      window-id={id}
      className={classes}
      style={maximized && bounds ? {position: "fixed", ...bounds} : undefined}
      header={getHeader()}
      icons={getIcons()}>
      {elementList.map((node, index) => Components(node, index))}
    </Panel>
  </>;
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
  elementList: PropTypes.array
};

export default AweWindow;
