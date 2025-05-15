import React, {Component} from "react";
import {Panel} from 'primereact/panel';
import {withTranslation} from 'react-i18next';
import "./AweWindow.less";
import {bindMethods, getHelpTooltipNode, getIconCode} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import PropTypes from "prop-types";

class AweWindow extends Component {

  constructor(props) {
    super(props);
    bindMethods(this, ["getIcon"]);
  }

  getIcon() {
    const {icon} = this.props;
    if (icon) {
      return getIconCode(icon, "fa-fw")
    }
    return null;
  }

  getHelpIcon() {
    const {help, helpImage} = this.props;
    if (help || helpImage) {
      return <i role="note" className={`help-icon pi pi-question-circle`} />;
    } else {
      return null;
    }
  }

  getHeader() {
    const {label, t, id} = this.props;
    return <div className={`help-target-${id}`}>{t(label)} {this.getHelpIcon()}</div>
  }

  render() {
    const {style, expand, id, help, helpImage, t, elementList = []} = this.props;
    const classes = classNames("m-2", `expandible-${expand || "vertical"}`, style);

    return <>
      {getHelpTooltipNode(help, helpImage, t,`.help-target-${id}`)}
      <Panel
      id={id}
      className={classes}
      header={this.getHeader()}
      icons={this.getIcon()}>
        {elementList.map((node, index) => Components(node, index))}
      </Panel>
    </>;
  }
}

AweWindow.propTypes = {
  id: PropTypes.string,
  style: PropTypes.string,
  expand: PropTypes.string,
  label: PropTypes.string,
  icon: PropTypes.string,
  help: PropTypes.string,
  helpImage: PropTypes.string,
  t: PropTypes.func.isRequired,
  elementList: PropTypes.array
};

export default withTranslation()(AweWindow);
