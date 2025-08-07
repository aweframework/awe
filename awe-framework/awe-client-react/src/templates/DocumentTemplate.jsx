import React, {Component} from "react";
import {getSource, getSourceChildren} from "../utilities/structure";

import "./WindowTemplate.css";
import {connect} from "react-redux";
import {withTranslation} from "react-i18next";

class DocumentTemplate extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    const sourceCenter = getSource("center", this.props.elementList);
    const sourceModal = getSource("modal", this.props.elementList);
    const sourceHidden = getSource("hidden", this.props.elementList);
    const sourceButtons = getSource("buttons", this.props.elementList);

    return (
      <div className={"expand expandible-vertical animate__animated animate__fadeIn"} style={{position: "relative"}}>
        <div className={`window-buttons  ${sourceButtons.style}`}>{getSourceChildren(sourceButtons)}</div>
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
export default connect(mapStateToProps, null)(withTranslation()(DocumentTemplate));
