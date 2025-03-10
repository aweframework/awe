import React from "react";
import {connectComponent} from "./AweComponent";
import {Steps} from "primereact/steps";
import {AwePanelableComponent} from "./AwePanelableComponent";
import {Components} from "../utilities/structure";

class AweSteps extends AwePanelableComponent {

  render() {
    const {model, t, disabled, elementList = []} = this.props;
    const activeIndex = this.getActiveIndex();
    return <div className={"p-steps-container expand expandible-vertical"}>
      <Steps model={model.values.map((item, index) => ({label: t(item.label), icon: item.icon, disabled: disabled || index > activeIndex }))}
             activeIndex={activeIndex}
             onSelect={this.onChange}
             readOnly={false}
             ref={(el) => this.steps = el}/>
      {elementList.filter((item, index) => index === activeIndex).map((node, index) => Components(node, index))}
    </div>;
  }
}

export default connectComponent(AweSteps);
