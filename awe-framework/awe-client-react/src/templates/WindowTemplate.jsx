import React, {Component} from "react";
import {Components} from "../utilities/structure";
import {BreadCrumb} from "primereact/breadcrumb";

import "./WindowTemplate.css";
import {connect} from "react-redux";
import {withTranslation} from "react-i18next";

class WindowTemplate extends Component {

  constructor(props) {
    super(props);

    this.getSource = this.getSource.bind(this);
  }

  getSource(source) {
    let node = this.props.elementList.find(n => n.source === source);
    return node ? node.elementList.map((child, index) => Components(child, index)) : null;
  }

  render() {
    const {breadcrumbs} = this.props;
    const home = {icon: 'pi pi-home'}

    return (
      <div className={"expand expandible-vertical animate__animated animate__fadeIn"} style={{position: "relative"}}>
        <div className={"window-buttons"}>{this.getSource("buttons")}</div>
        <BreadCrumb model={breadcrumbs.items} home={home}/>
        <div className={"expand expandible-vertical"}>{this.getSource("center")}</div>
        <div style={{position: 'absolute'}}>{this.getSource("modal")}</div>
        <div style={{display: 'none'}}>{this.getSource("hidden")}</div>
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    breadcrumbs: state.screen.breadcrumbs
  };
}

// Connect redux store updates
export default connect(mapStateToProps, null)(withTranslation()(WindowTemplate));
