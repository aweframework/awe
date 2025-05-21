import React from "react";
import {connectComponent} from "./AweComponent";
import {Accordion, AccordionTab} from "primereact/accordion";
import {bindMethods, translateLabel} from "../utilities";
import {AwePanelableComponent} from "./AwePanelableComponent";
import {Components} from "../utilities/structure";

class AweAccordionComponent extends AwePanelableComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["getHeader", "getActiveIndex", "onChange"]);
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    const {elementList, updateModelWithDependencies, address} = this.props;
    updateModelWithDependencies(address, {
      values: elementList
        .filter(node => node.elementType === "AccordionItem")
        .map(item => ({value: item.id, label: item.id, selected: false}))
    });
  }

  getActiveIndex() {
    const {autocollapse, model} = this.props;
    if (autocollapse === false) {
      return model.values.reduce((all, item, index) => item.selected ? [...all, index] : all, []);
    } else {
      return super.getActiveIndex();
    }
  }

  onChange(e) {
    const {autocollapse, address, model, updateModelWithDependencies} = this.props;
    if (autocollapse === false) {
      updateModelWithDependencies(address, {
        values: model.values.map((item, index) => ({
          ...item,
          selected: e.index.includes(index)
        }))
      });
    } else {
      super.onChange(e);
    }
  }

  getHeader(node) {
    const {t} = this.props;
    const {label} = node;
    if (label) {
      return <span className={"window-header"}>{translateLabel(label, t)}</span>
    }
    return null;
  }

  render() {
    const {elementList, autocollapse, style} = this.props;
    return <Accordion className={style}
                      activeIndex={this.getActiveIndex()}
                      onTabChange={this.onChange}
                      multiple={autocollapse === false}>
      {elementList
        .filter(node => node.elementType === "AccordionItem")
        .map((node, index) => <AccordionTab key={node.id || `accordion-${index}`} header={this.getHeader(node)}
                                            children={node.elementList.map((subnode, subindex) => Components(subnode, subindex))}/>)}
    </Accordion>;
  }
}

export default connectComponent(AweAccordionComponent);
