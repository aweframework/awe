import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";
import PropTypes from "prop-types";

class ColumnIcon extends Component {
  render() {
    const {t, data, style} = this.props;
    const {icon, title, label} = data;
    const classes = classNames(style, data?.style);
    return <span className="icon-container" title={translateLabel(title || label, t)}>{getIconCode(icon, classes)}</span>;
  }
}

ColumnIcon.propTypes = {
  data: PropTypes.object.isRequired,
  t: PropTypes.func.isRequired,
  style: PropTypes.string
};

export default withTranslation()(ColumnIcon);
