import React from "react";
import {AweComponent, connectComponent} from "../components/AweComponent";
import {bindMethods, getIconCode, translateLabel} from "../utilities";
import {classNames} from "../utilities/components";
import {PickList} from "primereact/picklist";
import "./AwePicklist.less";

class AwePicklist extends AweComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["getValue", "onChange", "itemTemplate"])
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
  }

  onChange(e) {
    const {address, updateModelWithDependencies} = this.props;
    updateModelWithDependencies(address, {
      values: [
        ...e.source.map(v => ({...v, selected: false})),
        ...e.target.map(v => ({...v, selected: true}))
      ]
    });
  }

  getValue() {
    const {model} = this.props;
    return model.values.filter(item => item.selected);
  }

  itemTemplate(item) {
    const {t} = this.props;
    return (
      <div className="flex flex-wrap p-2 align-items-center gap-3">
        {item.image && <img className="picklist-image" src={item.image} alt={translateLabel(item.label, t)} />}
        <div className="flex-1 flex flex-column gap-2">
          <span className="picklist-title">{translateLabel(item.label, t)}</span>
          <div className="flex align-items-center gap-2">
            {item.icon && getIconCode(item.icon, "p-button-icon p-c p-button-icon-left")}
            {item.description && <span className="picklist-description">{translateLabel(item.description, t)}</span>}
          </div>
        </div>
        {item.unit && <span className="picklist-unit">{item.unit}</span>}
      </div>
    );
  }

  render() {
    const {t, attributes, model, address} = this.props;
    const {label, style} = attributes;
    const classes = classNames(style);

    // Paint component
    return <div className={classes} criterion-id={address.component}>
      <PickList
        dataKey="value"
        source={model.values.filter(v => !v.selected)}
        target={model.values.filter(v => v.selected)}
        onChange={this.onChange}
        showSourceControls={false}
        showTargetControls={false}
        sourceHeader={translateLabel(label, t)}
        targetHeader={translateLabel("SCREEN_TEXT_SELECTED", t)}
        itemTemplate={this.itemTemplate}
      />
    </div>;
  }
}

export default connectComponent(AwePicklist);
