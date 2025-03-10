import React, {Component} from "react";
import {withTranslation} from 'react-i18next';
import {classNames} from "../utilities/components";
import {Components} from "../utilities/structure";
import parse from 'html-react-parser';

class Tag extends Component {

  render() {
    const {t, type, id, style, label, expand, elementList, value} = this.props;
    const classes = classNames({[`expandible-${expand}`]: expand}, style);

    return React.createElement(type || "div", {
      id: id,
      className: classes,
      ...(elementList.length > 0 ? {children: [parse(t(label) || ""), value].concat(elementList.map((node, index) => Components(node, index)))} :
        {children: [parse(t(label) || ""), value]})
    });
  }
}

export default withTranslation()(Tag);
