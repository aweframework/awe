import React from "react";
import {useTranslation} from 'react-i18next';
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import parse from 'html-react-parser';
import {translateLabel} from "../utilities";

function Tag(props) {
  const {type, id, style, label, expand, elementList, value} = props;
  const { t } = useTranslation();
  const classes = classNames({[`expandible-${expand}`]: expand}, style);

  return React.createElement(type || "div", {
    id: id,
    className: classes,
    ...(elementList.length > 0 ? {children: [...[parse(translateLabel(label, t) || ""), value], ...(elementList.map((node, index) => Components(node, index)))]} :
      {children: [parse(translateLabel(label, t) || ""), value]})
  });
}

export default Tag;
