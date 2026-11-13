import React from "react";
import PropTypes from "prop-types";

/**
 * AWE File Manager component
 * @category Widgets
 */
function AweFileManager(props) {
  const {id} = props;
  return <div className={"expand expandible-vertical"} id={id}>
    <iframe src={"/fm/home"} className={"expand"} style={{border: "none"}}/>
  </div>;
}

AweFileManager.propTypes = {
  id: PropTypes.string,
};

export default AweFileManager;
