import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";

class ColumnIcon extends Component {
  render() {
    const {t, data} = this.props;
    const {icon, style, title, label} = data;
    const classes = classNames(style);
    return <span className="icon-container" title={translateLabel(title || label, t)}>{getIconCode(icon, classes)}</span>;
  }
}

export default withTranslation()(ColumnIcon);
