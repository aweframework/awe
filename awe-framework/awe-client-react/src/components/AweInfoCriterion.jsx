import React from "react";
import {Criteria} from "../utilities/structure";
import PropTypes from "prop-types";

import "./AweInfoCriterion.less";

function AweInfoCriterion(props) {
  return <div className="info-criterion">{Criteria(props, 0)}</div>;
}

AweInfoCriterion.propTypes = {
  address: PropTypes.object,
  attributes: PropTypes.object,
  component: PropTypes.string,
  model: PropTypes.object
};

export default AweInfoCriterion;
