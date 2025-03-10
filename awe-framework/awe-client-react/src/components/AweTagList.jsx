import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import {bindMethods} from "../utilities";
import {classNames, parseValidationRules} from "../utilities/components";
import {Components} from "../utilities/structure";

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
    const address = {view, component: element.id};
    const validationRules = parseValidationRules(element.validation, address);
    return [{
      uid: element.id,
      address: address,
      model: {values: []},
      storedModel: {values: []},
      attributes: {...element},
      storedAttributes: {...element},
      validationRules: {...validationRules},
      storedValidationRules: {...validationRules},
      actions: [...element.actions || []],
      dependencies: [...element.dependencies || []],
      contextMenu: [...element.contextMenu || []],
      context: {}
    }];
  }
  return [];
}

function findComponents(elementList, view) {
  return elementList.reduce((components, element) => [...components, ...getComponent(element, view), ...findComponents(element.elementList, view)], []);
}

class AweTagList extends AweComponent {

  constructor(props) {
    super(props);
    this.state = {};
    bindMethods(this, ["reloadElements"]);
  }

  reloadElements() {
    const {model, elementList, address} = this.props;
    const fixedElements = model.values.map(row => generateTagListRow(elementList, row)).flat();
    const components = findComponents(fixedElements, address.view);
    this.props.updateMultipleComponentsWithDependencies(components);
    this.setState({elementList: fixedElements, components});
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    this.reloadElements();
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    if (!_.isEqual(prevProps.model.values, this.props.model.values)) {
      this.reloadElements();
    }
  }

  render() {
    const {t, type, id, style, label, expand} = this.props;
    const {elementList} = this.state;
    const classes = classNames({[`expandible-${expand}`]: expand}, style);

    return React.createElement(type || "div", {
      id: id,
      className: classes,
      children: [t(label)].concat((elementList || []).map((node, index) => Components(node, index)))
    });
  }
}

export default connectComponent(AweTagList);
