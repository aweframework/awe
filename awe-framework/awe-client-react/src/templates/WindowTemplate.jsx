import React, {Component} from "react";
import {getSource, getSourceChildren} from "../utilities/structure";
import {BreadCrumb} from "primereact/breadcrumb";

import "./WindowTemplate.css";
import {connect} from "react-redux";
import {withTranslation} from "react-i18next";

class WindowTemplate extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    const {breadcrumbs} = this.props;
    const home = {icon: 'pi pi-home'};
    const sourceCenter = getSource("center", this.props.elementList);
    const sourceModal = getSource("modal", this.props.elementList);
    const sourceHidden = getSource("hidden", this.props.elementList);
    const sourceButtons = getSource("buttons", this.props.elementList);

    return (
      <div className={"expand expandible-vertical animate__animated animate__fadeIn"} style={{position: "relative"}}>
        <div className="breadcrumb-buttons">
            <BreadCrumb model={breadcrumbs.items} home={home}/>
            <div className={`window-buttons pull-right ${sourceButtons.style}`}>{getSourceChildren(sourceButtons)}</div>
        </div>
        <div className={`expand expandible-vertical ${sourceCenter.style}`}>{getSourceChildren(sourceCenter)}</div>
        <div style={{position: 'absolute'}} className={sourceModal.style}>{getSourceChildren(sourceModal)}</div>
        <div style={{display: 'none'}}>{getSourceChildren(sourceHidden)}</div>
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
