import React, {Component} from "react";
import {getSource, getSourceChildren} from "../utilities/structure";

export default class FullTemplate extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    const sourceCenter = getSource("center", this.props.elementList);
    const sourceModal = getSource("modal", this.props.elementList);
    const sourceHidden = getSource("hidden", this.props.elementList);

    return (
      <div className={"expand expandible-vertical animate__animated animate__fadeIn"}>
        <div className={`expand expandible-vertical ${sourceCenter.style}`}>{getSourceChildren(sourceCenter)}</div>
        <div style={{position:'absolute'}} className={sourceModal.style}>{getSourceChildren(sourceModal)}</div>
        <div style={{display:'none'}}>{getSourceChildren(sourceHidden)}</div>
      </div>
    );
  }
}
