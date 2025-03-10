import React, {Component} from "react";
import {Components} from "../utilities/structure";

export default class FullTemplate extends Component {

  constructor(props) {
    super(props);

    this.getSource = this.getSource.bind(this);
  }

  getSource(source) {
    let node = this.props.elementList.find(n => n.source === source);
    return node ? node.elementList.map((child, index) => Components(child, index)) : null;
  }

  render() {
    return (
      <div className={"expand expandible-vertical animate__animated animate__fadeIn"}>
        <div className={"expand expandible-vertical"}>{this.getSource("center")}</div>
        <div style={{position:'absolute'}}>{this.getSource("modal")}</div>
        <div style={{display:'none'}}>{this.getSource("hidden")}</div>
      </div>
    );
  }
}
