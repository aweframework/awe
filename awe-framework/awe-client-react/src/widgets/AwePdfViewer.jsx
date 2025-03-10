import React from "react";
import {AweWidget, connectWidget} from "./AweWidget";
import {getContextPath, getCookie, getRestUrl} from "../utilities";
import {classNames, getFormValues} from "../utilities/components";
import {PDFObject} from 'react-pdfobject';
import "./AwePdfViewer.less";
import {Skeleton} from "primereact/skeleton";

/**
 * AWE PDF viewer component
 * @extends AweWidget
 * @category Widgets
 */
class AwePdfViewer extends AweWidget {

  constructor(props) {
    super(props);
  }

  componentDidMount() {
    const {attributes, settings} = this.props;
    const {targetAction} = attributes;
    const {token} = settings;

    // Get PDF file
    fetch(`${getContextPath()}${getRestUrl("file", "stream", "maintain", targetAction)}`, {
      method: 'POST',
      body: JSON.stringify(getFormValues(this.props)),
      headers: {
        "Accept": "*/*",
        "Authorization": token,
        "Content-Type": "application/json",
        "X-XSRF-TOKEN": getCookie("XSRF-TOKEN")
      }
    }).then(response => response.blob())
      .then(blob => this.setState({pdf: window.URL.createObjectURL(blob)}))
  }

  getPdfTemplate(pdf) {
    if (pdf) {
      return <PDFObject url={pdf}/>;
    } else {
      return <li className="mb-3">
        <div className="flex">
          <Skeleton shape="circle" size="4rem" className="mr-2"/>
          <div style={{flex: '1'}}>
            <Skeleton width="100%" className="mb-2"/>
            <Skeleton width="75%"/>
          </div>
        </div>
      </li>;
    }
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {attributes, address} = this.props;
    const {style} = attributes;
    const {pdf} = this.state;
    return <div id={address.component} className={classNames("pdf-viewer", "expand", style)}>
      {this.getPdfTemplate(pdf)}
    </div>
  }
}

export default connectWidget(AwePdfViewer);
