import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import {InputNumber} from "primereact/inputnumber";
import {translateNumberFormat} from "../utilities/numbers";
import {formatMessage} from "../utilities";
import {classNames} from "../utilities/components";

class ColumnNumeric extends Component {

  constructor(props) {
    super(props);

    // Bind events
    this.onChange = this.onChange.bind(this);
  }

  onChange(e) {
    const {address, updateModelWithDependencies, data} = this.props;
    if (data.value !== e.value) {
      updateModelWithDependencies(address, {values: e.value});
    }
  }

  render() {
    const {t, placeholder, required, readonly, data, numberFormat, align} = this.props;
    const {maxFractionDigits, minFractionDigits, min, max, suffix, locale} = translateNumberFormat(numberFormat);
    const classes = classNames({"p-invalid": data?.error});
    return <div className={classNames(classes, "column-editor")}>
        <InputNumber
        value={data.value}
        mode="decimal"
        placeholder={t(placeholder)}
        required={required}
        disabled={readonly}
        locale={locale}
        maxFractionDigits={maxFractionDigits}
        minFractionDigits={minFractionDigits}
        min={min}
        max={max}
        suffix={suffix}
        className={classes}
        inputStyle={{textAlign: align || "right"}}
        onValueChange={this.onChange}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
    </div>;
  }
}

export default withTranslation()(ColumnNumeric);
