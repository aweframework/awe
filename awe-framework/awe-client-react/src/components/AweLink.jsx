import React from "react";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";
import {useComponentState} from "../hooks/useComponentState";
import {useTranslation} from "react-i18next";
import PropTypes from "prop-types";

function AweLink(props) {
  const { id } = props;
  const { attributes = {} } = useComponentState(id);
  const { url, style, title, label, visible = true } = attributes;
  const { t } = useTranslation();
  const classes = classNames(style, { "hidden": !visible });

  return <a
    id={id}
    href={url}
    target={"_blank"}
    className={classes}
    title={translateLabel(title, t)}>
    <span className={"link-text"}>{translateLabel(label, t)}</span>
  </a>;

}

AweLink.propTypes = {
  id: PropTypes.string,
};

export default AweLink;
