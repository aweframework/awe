import React, {Component} from "react";
import {InputText} from "primereact/inputtext";
import {fromColor, toColor} from "../utilities/color";
import {OverlayPanel} from "primereact/overlaypanel";
import {ColorPicker} from "primereact/colorpicker";
import {bindMethods, formatMessage} from "../utilities";
import {classNames} from "../utilities/components";

class ColumnColor extends Component {

  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["onChange", "onBlur", "onShowColor", "onShowColorKey"]);

    this.state = {value: fromColor(this.props.data.value)};
  }

  onChange(e) {
    this.setState({value: e.target.value, writing: true});
  }

  onBlur() {
    const {value} = this.state;
    const {address, updateModelWithDependencies, data} = this.props;
    const color = toColor(value);
    if (data.value !== color) {
      updateModelWithDependencies(address, {values: color});
    }
    this.setState({writing: false});
  }

  onShowColor(e) {
    if (!this.props.readonly) {
      this.overlay.toggle(e);
    }
  }

  onShowColorKey(e) {
    _.debounce(() => this.onShowColor(e), 100);
  }

  render() {
    const {t, placeholder, required, readonly, size, style, data} = this.props;
    const {value} = this.state;
    const classes = classNames("p-inputgroup", "column-editor", {"p-invalid": data?.error});

    return <div className={classes}>
      <InputText
        value={toColor(value)}
        className={classNames(style, {[`p-inputtext-${size}`]: size, "p-invalid": data?.error})}
        placeholder={t(placeholder)}
        onChange={this.onChange}
        onBlur={this.onBlur}
        required={required}
        disabled={readonly}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
      <span className="p-inputgroup-addon">
        <button className={"colorpicker " + (value ? "" : "no-color")}
              style={{backgroundColor: toColor(value)}} onClick={this.onShowColor} onKeyDown={this.onShowColorKey}/>
        <OverlayPanel ref={(el) => this.overlay = el} dismissable appendTo={document.body} onHide={this.onBlur}>
          <ColorPicker
            inline
            value={fromColor(value)}
            disabled={readonly}
            onChange={this.onChange}
            format="hex"
          />
        </OverlayPanel>
      </span>
    </div>;
  }
}

export default ColumnColor;
