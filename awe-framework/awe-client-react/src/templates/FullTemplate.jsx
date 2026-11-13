import React from "react";
import {getSource, getSourceChildren, getSourceStyle} from "../utilities/structure";
import PropTypes from "prop-types";

function FullTemplate(props) {

  const {elementList = []} = props;
  const sourceCenter = getSource("center", elementList);
  const sourceModal = getSource("modal", elementList);
  const sourceHidden = getSource("hidden", elementList);

  return (
    <div className={"expand expandible-vertical animate__animated animate__fadeIn"}>
      <div className={`expand expandible-vertical maximize-target ${getSourceStyle(sourceCenter)}`}>{getSourceChildren(sourceCenter)}</div>
      <div style={{position:'absolute'}} className={getSourceStyle(sourceModal)}>{getSourceChildren(sourceModal)}</div>
      <div style={{display:'none'}}>{getSourceChildren(sourceHidden)}</div>
    </div>
  );
}

FullTemplate.propTypes = {
  elementList: PropTypes.any,
};

export default FullTemplate;