import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {translateLabel} from "../utilities";

class ColumnImage extends Component {
  render() {
    const {t} = this.props;
    const {image, style, title, label} = this.props.data;
    return <img src={image} className={"grid-image " + (style || "")} alt={translateLabel(label, t)} title={translateLabel(title || label, t)}/>;
  }
}

export default withTranslation()(ColumnImage);