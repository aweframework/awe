import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";

class ColumnIcon extends Component {
  render() {
    const {t, data} = this.props;
    const {icon, style, title, label} = data;
    const classes = classNames("pi", "fa", icon, style);
    return <span className={classes} title={translateLabel(title || label, t)}/>;
  }
}

export default withTranslation()(ColumnIcon);
