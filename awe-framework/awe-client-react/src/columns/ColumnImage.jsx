import React from "react";
import {useTranslation} from "react-i18next";
import {translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

function ColumnImage(props) {
  const { style, data } = props;
  const { t } = useTranslation();
  const { image, title, label } = data;
  const classes = classNames(style, data?.style, "grid-image");
  return <img src={image} className={classes} alt={translateLabel(label, t)} title={translateLabel(title || label, t)} />;
}

ColumnImage.propTypes = {
  data: PropTypes.object.isRequired,
  style: PropTypes.string
};

export default ColumnImage;