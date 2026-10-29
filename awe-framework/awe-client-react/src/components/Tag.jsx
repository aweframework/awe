import React from "react";
import {useTranslation} from 'react-i18next';
import {classNames, isVoidElement} from "../utilities/components";
import {Components} from "../utilities/structure";
import {renderSafeHtml} from "../utilities/sanitizeHtml";
import {getIconNode, translateLabel} from "../utilities";
import {isLoginPage} from "../utilities/signin";
import SigninPage, {useSigninPart} from "./SigninPage";
import PropTypes from "prop-types";
import "./Tag.less";

function Tag(props) {
  const {type, id, style, label, expand, elementList, value, icon} = props;
  const { t } = useTranslation();
  const classes = classNames({[`expandible-${expand}`]: expand}, style);
  const signinPart = useSigninPart(style);

  // Void elements (hr, br...) cannot have children
  if (isVoidElement(type)) {
    return React.createElement(type, {id: id, className: classes});
  }

  // Same precedence as the AngularJS engine: the label, else the value, else the children
  // A tag with a label can show a decorative icon before it (a section title)
  const labelIcon = label && icon ? getIconNode(icon, {className: "tag-icon", "aria-hidden": "true"}) : null;
  const content = label ? <>{labelIcon}{renderSafeHtml(translateLabel(label, t))}</> : (value ?? elementList.map((node, index) => Components(node, index)));

  // A tag without type generates no element: its content goes straight to the parent
  if (!type) {
    return <>{content}</>;
  }

  // The sign-in page and the other login pages are branded through the settings (awe.application.login.*)
  if (isLoginPage(id, elementList)) {
    return <SigninPage id={id} className={classes}>{content}</SigninPage>;
  }

  if (signinPart) {
    return React.createElement(type,
      {id: id, className: classNames(classes, signinPart.className), ...signinPart.attributes},
      signinPart.before, content, signinPart.after);
  }

  return React.createElement(type, {id: id, className: classes}, content);
}

Tag.propTypes = {
  elementList: PropTypes.any,
  expand: PropTypes.any,
  icon: PropTypes.string,
  id: PropTypes.string,
  label: PropTypes.string,
  style: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  type: PropTypes.any,
  value: PropTypes.any,
};

export default Tag;
