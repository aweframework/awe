import React from "react";
import { classNames } from "../utilities/components";
import { translateLabel } from "../utilities";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";

function AweImage(props) {
  const { id } = props;
  const { attributes = {} } = useComponentState(id);
  const { url, alternateUrl, style, title, visible = true } = attributes;
  const classes = classNames(style, { "hidden": !visible });
  const { t } = useTranslation();

  return (<img
    id={id}
    src={url}
    alt={alternateUrl}
    className={classes}
    title={translateLabel(title, t)} />);
}

AweImage.propTypes = {
  id: PropTypes.string
};


export default AweImage;
