import React from "react";
import {connectComponent} from "./AweComponent";
import {TabMenu} from "primereact/tabmenu";
import {AwePanelableComponent} from "./AwePanelableComponent";
import {Components} from "../utilities/structure";

class AweTabs extends AwePanelableComponent {

  render() {
    const {id, model, elementList, t, disabled} = this.props;
    const activeIndex = this.getActiveIndex();
    return <>
      <TabMenu id={id} model={model.values.map((item, index) => ({
        ...item,
        label: t(item.label),
        disabled,
        className: [item.style || "",  "label-" + item.label].join(" ")
      }))}
               activeIndex={activeIndex}
               onTabChange={this.onChange}/>
      {elementList.filter((item, index) => index === activeIndex).map((node, index) => Components(node, index))}
    </>;
  }
}

export default connectComponent(AweTabs);
