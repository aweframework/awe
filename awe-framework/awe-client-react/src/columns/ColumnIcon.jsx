import React from "react";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {getIconCode, translateLabel} from "../utilities";

function ColumnIcon(props) {
  const {t} = useTranslation();
  const {data} = props;
  const {icon, style, title, label} = data;
  const classes = classNames(style, data?.style);

  return <span className="icon-container" title={translateLabel(title || label, t)}>{getIconCode(icon, classes)}</span>;
}

export default ColumnIcon;