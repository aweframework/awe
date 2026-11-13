import React from "react";
import {useTranslation} from "react-i18next";
import {translateLabel} from "../utilities";
import "./ColumnTextView.less";
import {classNames} from "../utilities/components";
import PropTypes from "prop-types";

function ColumnLink(props) {
  const { data, align, style } = props;
  const { t } = useTranslation();
  const { value, label, title = "" } = data;
  const classes = classNames("link", style, data?.style);
  return <a
    href={value}
    className={classes}
    title={translateLabel(title, t)}
    style={{ textAlign: align || "left" }}
    target="_blank" rel="noreferrer">{translateLabel(label, t) || value}</a>;
}

ColumnLink.propTypes = {
  data: PropTypes.object.isRequired,
  align: PropTypes.string,
  style: PropTypes.string
};

export default ColumnLink;
