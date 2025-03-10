import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import {classNames} from "../utilities/components";

class AweLink extends AweComponent {

  render() {
    const {t, attributes, address} = this.props;
    const {url, style, title, label} = attributes;
    const classes = classNames(style);

    return <a
          id={address.component}
          href={url}
          target={"_blank"}
          className={classes}
          title={t(title)}>
      <span className={"link-text"}>{t(label)}</span>
    </a>;
  }
}

export default connectComponent(AweLink);
