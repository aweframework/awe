import React from "react";
import {Criteria} from "../utilities/structure";

import "./AweInfoCriterion.less";

function AweInfoCriterion(props) {
  return <div className="info-criterion">{Criteria(props, 0)}</div>;
}

export default AweInfoCriterion;