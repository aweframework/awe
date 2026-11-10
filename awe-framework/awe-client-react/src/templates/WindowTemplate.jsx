import React from "react";
import {useSelector} from "react-redux";
import {getSource, getSourceChildren, getSourceStyle} from "../utilities/structure";
import WindowBreadcrumb from "../components/WindowBreadcrumb";

import "./WindowTemplate.css";
import PropTypes from "prop-types";

const WINDOW_HEADERS = ["breadcrumb", "none"];
const normalizeHeader = value => String(value ?? "").trim().toLowerCase();

/**
 * Resolve the window header as the server does (WindowHeaderType.parse): the screen value overrides the application
 * setting, both trimmed and case insensitive; an unknown screen value falls back to the application setting and an
 * unknown application setting to the breadcrumb
 */
function resolveWindowHeader(screenHeader, applicationHeader) {
  const screenValue = normalizeHeader(screenHeader);
  const applicationValue = normalizeHeader(applicationHeader);
  return [screenValue, applicationValue].find(value => WINDOW_HEADERS.includes(value)) ?? "breadcrumb";
}

function WindowTemplate(props) {

  const { elementList = [], windowHeader } = props;
  const applicationHeader = useSelector(state => state.settings?.windowHeader);
  const noneHeader = resolveWindowHeader(windowHeader, applicationHeader) === "none";
  const sourceCenter = getSource("center", elementList);
  const sourceModal = getSource("modal", elementList);
  const sourceHidden = getSource("hidden", elementList);
  const sourceButtons = getSource("buttons", elementList);
  const buttons =<div className={`window-buttons ${getSourceStyle(sourceButtons)}`}>{getSourceChildren(sourceButtons)}</div>;

  const getHeader = () => {
    if (noneHeader) {
      return <div className="breadcrumb-buttons window-header-none p-breadcrumb-container">
        {buttons}
      </div>;
    }
    return <div className="breadcrumb-buttons p-breadcrumb-container">
      <WindowBreadcrumb/>
      {buttons}
    </div>;
  };

  return (
    <div className={"expand expandible-vertical animate__animated animate__fadeIn"} style={{position: "relative"}}>
      {getHeader()}
      <div className={`expand expandible-vertical maximize-target ${getSourceStyle(sourceCenter)}`}>{getSourceChildren(sourceCenter)}</div>
      <div style={{position: 'absolute'}} className={getSourceStyle(sourceModal)}>{getSourceChildren(sourceModal)}</div>
      <div style={{display: 'none'}}>{getSourceChildren(sourceHidden)}</div>
    </div>
  );
}

// Connect redux store updates
WindowTemplate.propTypes = {
  elementList: PropTypes.any,
  windowHeader: PropTypes.string,
};

export default WindowTemplate;
