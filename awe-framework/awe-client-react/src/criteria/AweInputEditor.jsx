import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {Editor} from "primereact/editor";
import {classNames} from "../utilities/components";

class AweInputEditor extends AweTextComponent {

  constructor(props) {
    super(props);
    this.groupClass = "col-12";
    this.getComponent = this.getComponent.bind(this);
  }

  getLabel() {
    return null;
  }

  getIcon() {
    return null;
  }

  getUnit() {
    return null;
  }

  onChange(e) {
    if (this.getValue() !== e.htmlValue) {
      this.props.updateModelWithDependencies(this.props.address, {values: [{value: e.htmlValue, selected: true}]});
    }
  }

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, size} = attributes;
    const classes = classNames(style, {[`text-${size}`]: size, [`p-inputtext-${size}`]: size});

    return <Editor
      id={address.component}
      style={{height: "100%", minHeight:"4rem"}}
      value={this.getValue()}
      className={classes}
      placeholder={t(placeholder)}
      onTextChange={this.onChange}
      required={required}
      disabled={readonly}
    />;
  }
}

export default connectComponent(AweInputEditor);
