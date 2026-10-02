import React from "react";
import {useTranslation} from "react-i18next";
import {classNames} from "../utilities/components";
import {TestAttributes, TestIds, testHook} from "../utilities/testIds";
import {getIconCode, translateLabel} from "../utilities";
import PropTypes from "prop-types";

function ColumnIcon(props) {
  const {t} = useTranslation();
  const {data} = props;
  const {icon, style, title, label} = data;
  const classes = classNames(style, data?.style);

  return <span className="icon-container" title={translateLabel(title || label, t)}
    {...testHook(TestIds.columnIcon, {attributes: {[TestAttributes.icon]: icon}})}>{getIconCode(icon, classes)}</span>;
}

ColumnIcon.propTypes = {
  data: PropTypes.oneOfType([PropTypes.object, PropTypes.array]),
};

export default ColumnIcon;