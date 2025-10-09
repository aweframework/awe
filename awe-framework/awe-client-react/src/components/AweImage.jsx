import React from "react";
import {classNames} from "../utilities/components";
import {translateLabel} from "../utilities";
import PropTypes from "prop-types";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";

function AweImage(props) {
  const { id } = props;
  const { address, attributes = {} } = useSelector(state => ({
    address: state.components[id]?.address,
    attributes: state.components[id]?.attributes
  }));
  const {url, alternateUrl, style, title} = attributes;
  const classes = classNames(style);
  const { t } = useTranslation();

  return (<img
        id={address.component}
        src={url}
        alt={alternateUrl}
        className={classes}
        title={translateLabel(title, t)}/>);
}

AweImage.propTypes = {
  id: PropTypes.string
};


export default AweImage;
