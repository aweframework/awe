import React from "react";
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {InputNumber} from "primereact/inputnumber";
import {bindMethods, translateLabel} from "../utilities";
import {formatNumber, translateNumberFormat} from "../utilities/numbers";
import {classNames} from "../utilities/components";

import "./AweInputNumeric.less";
import {Slider} from "primereact/slider";


class AweInputNumeric extends AweTextComponent {

  constructor(props) {
    super(props);
    this.groupClass = props.attributes.showSlider ? "" : this.groupClass;
    bindMethods(this, ["getComponent", "onChange", "onSlide"]);
  }

  onSlide(e) {
    const {readonly} = this.props.attributes;
    if (!readonly) {
      this.setState({number: Number(e.value), sliding: true});
      e.originalEvent.type === "click" && this.onChange(e);
    }
  }

  getIcon() {
    return this.props.attributes.showSlider ? null : super.getIcon();
  }

  getUnit() {
    return this.props.attributes.showSlider ? null : super.getUnit();
  }

  onChange(e) {
    const value = this.state.sliding ? this.state.number : e.value;
    if (value !== Number(this.getValue())) {
      const {address, updateModelWithDependencies, attributes = {}} = this.props;
      updateModelWithDependencies(address, {
        values: [{
          value: value,
          label: formatNumber(value, attributes.numberFormat),
          selected: true
        }]
      });

      this.setState({sliding: false});
    }
  }

  getComponent(style) {
    const {t, address, attributes} = this.props;
    const {placeholder, required, readonly, numberFormat, size, align, icon, unit, showSlider = false} = attributes;
    const {maxFractionDigits, minFractionDigits, min, max, suffix, locale, step} = translateNumberFormat(numberFormat);
    const value = this.state.sliding ? this.state.number : this.getValue();
    const classes = classNames({"with-icon": icon, "with-unit": unit, "with-slider": showSlider,
      [`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style);
    return <><InputNumber
      id={address.component}
      value={value}
      className={classes}
      placeholder={translateLabel(placeholder, t)}
      required={required}
      disabled={readonly}
      mode="decimal"
      locale={locale}
      maxFractionDigits={maxFractionDigits}
      minFractionDigits={minFractionDigits}
      min={min}
      max={max}
      suffix={suffix}
      onValueChange={this.onChange}
      inputStyle={{textAlign: align || "right"}}
      onKeyPress={e => e.key === "Enter" && this.onSubmit()}
    />
      {showSlider ? <Slider
        value={Number(value)}
        disabled={readonly}
        min={min}
        max={max}
        step={step}
        onChange={this.onSlide}
        onSlideEnd={this.onChange}
      /> : null}
    </>;
  }
}

export default connectComponent(AweInputNumeric);
