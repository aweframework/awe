import React, {Component} from "react";
import {Panel} from 'primereact/panel';
import {withTranslation} from 'react-i18next';
import "./AweWindow.less";
import {bindMethods, getIconCode} from "../utilities";
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";

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

  render() {
    const {style, expand, id, label, t, elementList = []} = this.props;
    const classes = classNames("m-2", `expandible-${expand || "vertical"}`, style);

    return <Panel
      id={id}
      className={classes}
      header={t(label)}
      icons={this.getIcon()}
      children={elementList.map((node, index) => Components(node, index))}/>;
  }
}

export default withTranslation()(AweWindow);
