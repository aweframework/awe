import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {ProgressBar} from "primereact/progressbar";
import {classNames} from "../utilities/components";

class ColumnProgress extends Component {
  render() {
    const {data} = this.props;
    const {style, value} = data;
    const classes = classNames(style);
    return <ProgressBar
      className={classes}
      style={{width: "100%"}}
      value={value || 0}/>;
  }
}

export default withTranslation()(ColumnProgress);
