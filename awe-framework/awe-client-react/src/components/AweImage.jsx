import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import {classNames} from "../utilities/components";

class AweImage extends AweComponent {

  render() {
    const {t, attributes, address} = this.props;
    const {url, alternateUrl, style, title} = attributes;
    const classes = classNames(style);

    return <img
          id={address.component}
          src={url}
          alt={alternateUrl}
          className={classes}
          title={t(title)}/>;
  }
}

export default connectComponent(AweImage);
