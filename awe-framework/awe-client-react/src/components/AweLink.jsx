import React from "react";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";
import {useSelector} from "react-redux";
import {useTranslation} from "react-i18next";

function AweLink(props) {
  const { id } = props;
  const { attributes = {} } = useSelector(state => ({
    attributes: state.components[id]?.attributes
  }));
  const {url, style, title, label, visible = true} = attributes;
  const { t } = useTranslation();
  const classes = classNames(style, {"hidden": !visible});

  return <a
        id={id}
        href={url}
        target={"_blank"}
        className={classes}
        title={translateLabel(title, t)}>
    <span className={"link-text"}>{translateLabel(label, t)}</span>
  </a>;

}

export default AweLink;
