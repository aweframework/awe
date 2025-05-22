import React from "react";
import {connectComponent} from "./AweComponent";
import {Steps} from "primereact/steps";
import {AwePanelableComponent} from "./AwePanelableComponent";
import {Components} from "../utilities/structure";
import './AweSteps.less';
import {getIconCode, translateLabel} from "../utilities";

class AweSteps extends AwePanelableComponent {

  titleRenderer(title) {
    const {t} = this.props;
    return title ? <span className={`p-steps-supertitle`}>{translateLabel(title, t)}</span> : null;
  }

  labelRenderer(label) {
    const {t} = this.props;
    return label ? <span className={`p-steps-title`}>{translateLabel(label, t)}</span> : null;
  }

  iconRenderer(icon, number) {
    return icon ? getIconCode(icon) : number;
  }

  itemRenderer(item, itemIndex) {

    const activeIndex = this.getActiveIndex();
    let icon = item.icon;
    let stepClass = "p-step-pending";

    if (activeIndex === itemIndex) {
      stepClass = "p-step-current";
    } else if (activeIndex > itemIndex) {
      stepClass = "p-step-completed";
      icon = "pi:check";
    }

    return (
      <button className={`p-menuitem-link ${stepClass}`} onClick={() => this.onChange({index: itemIndex})} tabIndex={-1}>
        <span className="p-steps-number">{this.iconRenderer(icon, itemIndex + 1)}</span>
        <div className={`p-steps-text`}>
          {this.titleRenderer(item.title)}
          {this.labelRenderer(item.label)}
        </div>
      </button>
    );
  };

  render() {
    const {model, disabled, orientation = "horizontal", elementList = []} = this.props;
    const activeIndex = this.getActiveIndex();
    const expandible = orientation === "horizontal" ? "vertical" : "horizontal";
    return <div className={`p-steps-container expand expandible-${expandible} orientation-${orientation}`}>
      <Steps model={model.values.map((item, index) => ({
        ...item,
        disabled: disabled || index > activeIndex,
        template: (item) => this.itemRenderer(item, index)
      }))}
             activeIndex={activeIndex}
             onSelect={this.onChange}
             readOnly={false}
             ref={(el) => this.steps = el}/>
      {elementList.filter((item, index) => index === activeIndex).map((node, index) => Components(node, index))}
    </div>;
  }
}

export default connectComponent(AweSteps);
