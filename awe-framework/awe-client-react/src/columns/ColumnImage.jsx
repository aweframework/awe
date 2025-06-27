import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

class ColumnImage extends Component {
  render() {
    const {t, style, data} = this.props;
    const {image, title, label} = data;
    const classes = classNames(style, data?.style, "grid-image");
    return <img src={image} className={classes} alt={translateLabel(label, t)} title={translateLabel(title || label, t)}/>;
  }
}

ColumnImage.propTypes = {
  data: PropTypes.object.isRequired,
  t: PropTypes.func.isRequired,
  style: PropTypes.string
};

export default withTranslation()(ColumnImage);