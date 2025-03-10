import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import {OverlayPanel} from "primereact/overlaypanel";
import {classNames, clickDropdown} from "../utilities/components";
import {getFirstDefinedValue, getIconCode} from "../utilities";

import "./AweAvatar.less";
import {Avatar} from "primereact/avatar";
import {Badge} from "primereact/badge";
import {Components} from "../utilities/structure";

class AweAvatar extends AweComponent {

  constructor(props) {
    super(props);

    this.onClick = this.onClick.bind(this);
  }

  onClick(e) {
    const {addActionsTop, address, elementList = [], actions = []} = this.props;
    if (actions.length > 0) {
      addActionsTop(actions.map(action => ({...action, address})));
    } else if (elementList.length > 0){
      clickDropdown(e, this.props, this.op);
    }
  }

  render() {
    const {t, elementList, attributes, model, actions} = this.props;
    const {values = []} = model;
    const {style, icon, disabled, label, size, unit, image, showLabel = true} = attributes;
    const classes = classNames("p-button-rounded", "p-button-text", "p-button-secondary", {[`p-button-${size}`]: size}, style);

    let computedLabel = getFirstDefinedValue(values?.[0]?.label, label);
    let computedImage = getFirstDefinedValue(values?.[0]?.image, image);
    let computedIcon = getFirstDefinedValue(values?.[0]?.icon, icon);
    let computedUnit = getFirstDefinedValue(values?.[0]?.unit, unit);
    let unitBadge = computedUnit && <Badge value={computedUnit}/>;
    let labelSpan = showLabel && <span className={"avatar-name"} >{t(computedLabel)}</span>;
    let dropdown = actions.length === 0 && elementList.length > 0 && <OverlayPanel ref={(el) => this.op = el} dismissable className={"info-dropdown"}>
      {elementList.map((node, index) => Components(node, index))}
    </OverlayPanel>;
    return <>
      <div className="avatar-component p-overlay-badge" onClick={this.onClick} role="button" onKeyDown={this.onClick}>
        <Avatar
          id={this.props.address.component}
          type="button"
          className={classes}
          shape={"circle"}
          image={computedImage}
          icon={!computedImage && getIconCode(computedIcon, "p-button-icon p-c p-button-icon-left")}
          disabled={this.props.disabled || disabled}
          label={!computedImage && !computedIcon && (computedLabel || "").charAt(0).toUpperCase()}
          title={t(computedLabel)}>
          {unitBadge}
        </Avatar>
        {labelSpan}
      </div>
      {dropdown}
    </>;
  }
}

export default connectComponent(AweAvatar);
