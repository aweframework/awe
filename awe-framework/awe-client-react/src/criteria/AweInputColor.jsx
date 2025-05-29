import React from "react";
import {connectComponent} from "../components/AweComponent";
import {ColorPicker} from "primereact/colorpicker";
import {bindMethods, translateLabel} from "../utilities";
import {InputText} from "primereact/inputtext";
import {OverlayPanel} from "primereact/overlaypanel";
import AweTextComponent from "./AweTextComponent";
import {classNames} from "../utilities/components";

import "./AweInputColor.less";
import {fromColor, toColor} from "../utilities/color";

class AweInputColor extends AweTextComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["getComponent", "getValue", "onShowColor", "onShowColorKey"])
  }

  onShowColor(e) {
    if (!this.props.attributes.readonly) {
      this.overlay.toggle(e);
    }
  }

  onShowColorKey(e) {
    _.debounce(() => this.onShowColor(e), 100);
  }

  onChange(e) {
    super.onChange({target:{value: toColor(e.target.value)}});
  }

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});

    return <>
      <InputText
        id={address.component}
        value={toColor(this.state.value)}
        className={classes}
        placeholder={translateLabel(placeholder, t)}
        onChange={this.onChange}
        onBlur={this.onBlur}
        onKeyPress={e => e.key === "Enter" && this.onSubmit()}
        required={required}
        disabled={readonly}
      />
      <span className="p-inputgroup-addon">
        <button className={classNames("colorpicker", {"no-color": !this.state.value, [`colorpicker-${size}`]: size})}
              style={{backgroundColor: toColor(this.state.value)}} onClick={this.onShowColor} onKeyDown={this.onShowColorKey}/>
        <OverlayPanel ref={(el) => this.overlay = el} dismissable appendTo={document.body} onHide={this.onBlur}>
          <ColorPicker
            inline
            value={fromColor(this.state.value)}
            disabled={readonly}
            onChange={this.onChange}
            format="hex"
          />
        </OverlayPanel>
      </span>
    </>;
  }
}

export default connectComponent(AweInputColor);
