import React, {useEffect, useState} from "react";
import {translateLabel} from "../utilities";
import {classNames, isVoidElement, parseValidationRules} from "../utilities/components";
import {Components} from "../utilities/structure";
import {useTranslation} from "react-i18next";
import {updateMultipleComponentsWithDependencies} from "../redux/thunks/components";
import {useDispatch} from "react-redux";
import {useComponentState} from "../hooks/useComponentState";
import useComponent from "../hooks/useComponent";
import PropTypes from "prop-types";
import {TestIds, testHook} from "../utilities/testIds";

function generateTagListRow(elements, row) {
  let template = JSON.stringify(elements);
  let replaced = Object.keys(row).reduce((prev, key) => prev.replaceAll(`[${key}]`, row[key]), template);
  return JSON.parse(replaced);
}

function isComponent(element) {
  return "id" in element;
}

function getComponent(element, view) {
  if (isComponent(element)) {
    const address = { view, component: element.id };
    const validationRules = parseValidationRules(element.validation, address);
    return [{
      uid: element.id,
      address: address,
      model: { values: [] },
      storedModel: { values: [] },
      attributes: { ...element },
      storedAttributes: { ...element },
      validationRules: { ...validationRules },
      storedValidationRules: { ...validationRules },
      actions: [...element.actions || []],
      dependencies: [...element.dependencies || []],
      contextMenu: [...element.contextMenu || []],
      context: {}
    }];
  }
  return [];
}

function findComponents(view, elementList = []) {
  return elementList.reduce((components, element) => [...components, ...getComponent(element, view), ...findComponents(view, element.elementList)], []);
}

function AweTagList(props) {

  const { type, id, elementList } = props;
  const [tagList, setTagList] = useState([]);
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { address } = useComponent(id);
  const { model = { values: [] }, attributes = {} } = useComponentState(id);
  const { label, style, expand, visible = true } = attributes;

  // Initialize on mount or model changes
  useEffect(() => {
    const fixedElements = model.values.map(row => generateTagListRow(elementList, row)).flat();
    const components = findComponents(address.view, fixedElements);
    dispatch(updateMultipleComponentsWithDependencies(components));
    setTagList(fixedElements);
  }, [elementList, model.values, address]);

  const classes = classNames({ [`expandible-${expand}`]: expand }, style, { "hidden": !visible });
  const hook = testHook(TestIds.tagList, { attributes: { "tag-list-id": id } });

  // Void elements (hr, br...) cannot have children
  if (isVoidElement(type)) {
    return React.createElement(type, {id: id, className: classes, ...hook});
  }

  return React.createElement(type || "div", {
    id: id,
    className: classes,
    ...hook,
    children: [...[translateLabel(label, t)], ...((tagList || []).map((node, index) => Components(node, index)))]
  });
}

AweTagList.propTypes = {
  elementList: PropTypes.any,
  id: PropTypes.string,
  type: PropTypes.any,
};

export default AweTagList;
