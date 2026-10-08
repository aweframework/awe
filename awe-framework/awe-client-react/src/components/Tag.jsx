import React from "react";
import {useTranslation} from 'react-i18next';
import {classNames, isVoidElement} from "../utilities/components";
import {Components} from "../utilities/structure";
import {renderSafeHtml} from "../utilities/sanitizeHtml";
import {translateLabel} from "../utilities";
import PropTypes from "prop-types";

function Tag(props) {
  const {type, id, style, label, expand, elementList, value} = props;
  const { t } = useTranslation();
  const classes = classNames({[`expandible-${expand}`]: expand}, style);

  // Void elements (hr, br...) cannot have children
  if (isVoidElement(type)) {
    return React.createElement(type, {id: id, className: classes});
  }

  return React.createElement(type || "div", {
    id: id,
    className: classes,
    ...(elementList.length > 0 ? {children: [...[renderSafeHtml(translateLabel(label, t)), value], ...(elementList.map((node, index) => Components(node, index)))]} :
      {children: [renderSafeHtml(translateLabel(label, t)), value]})
  });
}

Tag.propTypes = {
  elementList: PropTypes.any,
  expand: PropTypes.any,
  id: PropTypes.string,
  label: PropTypes.string,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  type: PropTypes.any,
  value: PropTypes.any,
};

export default Tag;
