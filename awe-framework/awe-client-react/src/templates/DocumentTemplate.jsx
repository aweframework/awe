import React from "react";
import {getSource, getSourceChildren, getSourceStyle} from "../utilities/structure";

import "./WindowTemplate.css";
import PropTypes from "prop-types";

function DocumentTemplate(props) {

  const { elementList = [] } = props;

  const sourceCenter = getSource("center", elementList);
  const sourceModal = getSource("modal", elementList);
  const sourceHidden = getSource("hidden", elementList);
  const sourceButtons = getSource("buttons", elementList);

  return (
    <div className={"expand expandible-vertical animate__animated animate__fadeIn"} style={{position: "relative"}}>
      <div className={`window-buttons  ${getSourceStyle(sourceButtons)}`}>{getSourceChildren(sourceButtons)}</div>
      <div className={`expand expandible-vertical maximize-target ${getSourceStyle(sourceCenter)}`}>{getSourceChildren(sourceCenter)}</div>
      <div style={{position: 'absolute'}} className={getSourceStyle(sourceModal)}>{getSourceChildren(sourceModal)}</div>
      <div style={{display: 'none'}}>{getSourceChildren(sourceHidden)}</div>
    </div>
  );
}

// Connect redux store updates
DocumentTemplate.propTypes = {
  elementList: PropTypes.any,
};

export default DocumentTemplate;
