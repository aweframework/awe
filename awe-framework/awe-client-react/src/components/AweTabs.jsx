import React from "react";
import {connectComponent} from "./AweComponent";
import {TabMenu} from "primereact/tabmenu";
import {AwePanelableComponent} from "./AwePanelableComponent";
import {Components} from "../utilities/structure";
import {getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import "./AweTabs.less";

class AweTabs extends AwePanelableComponent {

  titleRenderer(title) {
    const {t} = this.props;
    return title ? <span className={`p-tab-supertitle`}>{translateLabel(title, t)}</span> : null;
  }

  labelRenderer(label) {
    const {t} = this.props;
    return label ? <span className={`p-tab-title`}>{translateLabel(label, t)}</span> : null;
  }

  iconRenderer(icon) {
    return icon ? <span className="p-tab-icon">{getIconCode(icon)}</span> : null;
  }

  unitRenderer(unit) {
    const {t} = this.props;
    return unit ? <span className="p-tab-unit p-badge">{translateLabel(unit, t)}</span> : null;
  }

  itemRenderer(item, itemIndex) {
    return (
      <button className={`p-menuitem-link`} onClick={() => this.onChange({index: itemIndex})} tabIndex={-1}>
        {this.iconRenderer(item.icon)}
        <div className={`p-tab-text`}>
          {this.titleRenderer(item.title)}
          {this.labelRenderer(item.label)}
        </div>
        {this.unitRenderer(item.unit)}
      </button>
    );
  };

  render() {
    const activeIndex = this.getActiveIndex();
    const {id, model, disabled, orientation = "horizontal", elementList = []} = this.props;
    const expandible = orientation === "horizontal" ? "vertical" : "horizontal";
    const className = classNames("p-tabmenu-container", "expand", "expandible-" + expandible, "orientation-" + orientation);
    return <div className={className}>
      <TabMenu id={id} model={model.values.map((item, index) => ({
        ...item,
        disabled,
        template: (item) => this.itemRenderer(item, index)
      }))}
               activeIndex={activeIndex}
               onTabChange={this.onChange}/>
      {elementList.filter((item, index) => index === activeIndex).map((node, index) => Components(node, index))}
    </div>;
  }
}

export default connectComponent(AweTabs);
