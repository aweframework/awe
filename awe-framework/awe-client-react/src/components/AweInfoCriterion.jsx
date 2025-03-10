import React, {Component} from "react";
import {Criteria} from "../utilities/structure";

import "./AweInfoCriterion.less";

export default class AweInfoCriterion extends Component {

  render() {
    return <div className="info-criterion">{Criteria(this.props, 0)}</div>
  }
}
