import React from "react";
import {getSource, getSourceChildren} from "../utilities/structure";

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
      <div className={`window-buttons  ${sourceButtons.style}`}>{getSourceChildren(sourceButtons)}</div>
      <div className={`expand expandible-vertical ${sourceCenter.style}`}>{getSourceChildren(sourceCenter)}</div>
      <div style={{position: 'absolute'}} className={sourceModal.style}>{getSourceChildren(sourceModal)}</div>
      <div style={{display: 'none'}}>{getSourceChildren(sourceHidden)}</div>
    </div>
  );
}

// Connect redux store updates
DocumentTemplate.propTypes = {
  elementList: PropTypes.any,
};

export default DocumentTemplate;
