import React from "react";
import {AweComponent, connectComponent} from "./AweComponent";
import PivotTableUI from 'react-pivottable/PivotTableUI';
import 'react-pivottable/pivottable.css';
import {bindMethods} from "../utilities";
import {extractCellValue} from "../utilities/grid";
import {isEmpty} from "../utilities/general";

/**
 * AWE Pivot Table component
 * @extends AweComponent
 * @category Components
 */
class AwePivotTable extends AweComponent {

  /**
   * Create a pivot table
   * @param {object} props Component properties
   */
  constructor(props) {
    super(props);
    bindMethods(this, ["onChange"]);
    this.state = {rendered: false};
  }

  readValueList(value) {
    return isEmpty(value) ? [] : value.split(",");
  }

  writeValueList(value) {
    return value.join(",");
  }

  /**
   * On attributes change
   * @param e Changed attributes
   */
  onChange(e) {
    const {address, updateAttributes} = this.props;
    const {cols, rows, vals} = e;
    updateAttributes(address, {
      ...e,
      vals: this.writeValueList(vals),
      cols: this.writeValueList(cols),
      rows: this.writeValueList(rows)
    });
  }

  /**
   * Component was mounted.
   * Perform a state change to update the component and define cols,
   * rows and vals, as the first time it returns error
   */
  componentDidMount() {
    super.componentDidMount();
    this.setState({rendered: true});
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {model, attributes} = this.props;
    const {cols, rows, vals, style} = attributes;
    let fixedAttributes = this.state.rendered ? {
      ...attributes,
      vals: this.readValueList(vals),
      cols: this.readValueList(cols),
      rows: this.readValueList(rows)
    } : {};
    return (<div className={style}>
      <PivotTableUI
        data={model.values.map(row => Object.entries(row).reduce((prev, [key, value]) =>
          ({...prev, [key]: extractCellValue(value)}), {}))}
        onChange={this.onChange}
        unusedOrientationCutoff={Infinity}
        {...fixedAttributes}
      />
    </div>);
  }
}

export default connectComponent(AwePivotTable);
