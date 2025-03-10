import React, {Component} from "react";
import {withTranslation} from "react-i18next";

class ColumnImage extends Component {
  render() {
    const {t} = this.props;
    const {image, style, title, label} = this.props.data;
    return <img src={image} className={"grid-image " + (style || "")} alt={t(label)} title={t(title || label)}/>;
  }
}

export default withTranslation()(ColumnImage);