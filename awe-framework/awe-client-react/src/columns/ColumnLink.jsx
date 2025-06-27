import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {translateLabel} from "../utilities";
import "./ColumnTextView.less";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnLink extends Component {

  constructor(props) {
    super(props);
  }

  render() {
    const {t, data, align, style} = this.props;
    const {value, label, title = ""} = data;
    const classes = classNames("link", style, data?.style);
    return <a
      href={value}
      className={classes}
      title={translateLabel(title, t)}
      style={{textAlign: align || "left"}}
      target="_blank">{translateLabel(label, t) || value}</a>;
  }
}

ColumnLink.propTypes = {
  data: PropTypes.object.isRequired,
  align: PropTypes.string,
  t: PropTypes.func.isRequired,
  style: PropTypes.string
};

export default withTranslation()(ColumnLink);
