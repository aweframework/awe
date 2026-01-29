import React, { useCallback, useEffect, useState } from "react";
import { classNames } from "../utilities/components";
import { PDFObject } from 'react-pdfobject';
import "./AwePdfViewer.less";
import { Skeleton } from "primereact/skeleton";
import { useDispatch, useSelector } from "react-redux";
import { useComponentState } from "../hooks/useComponentState";
import { fetchPdfAction } from "../redux/thunks/files";

/**
 * AWE PDF viewer component
 * @category Widgets
 */
function AwePdfViewer(props) {

  const { id } = props;
  const { attributes = {}, context = {} } = useComponentState(id);

  const [pdf, setPdf] = useState(null);
  const dispatch = useDispatch();

  useEffect(() => {
    const { targetAction } = attributes;
    dispatch(fetchPdfAction(targetAction, setPdf));
  }, []);

  const getPdfTemplate = useCallback((pdf) => {
    if (pdf) {
      return <PDFObject url={pdf} />;
    } else {
      return <li className="mb-3">
        <div className="flex">
          <Skeleton shape="circle" size="4rem" className="mr-2" />
          <div style={{ flex: '1' }}>
            <Skeleton width="100%" className="mb-2" />
            <Skeleton width="75%" />
          </div>
        </div>
      </li>;
    }
  }, []);

  const { style, visible = true } = attributes;
  return visible ? <div id={id} className={classNames("pdf-viewer", "expand", style)}>
    {getPdfTemplate(pdf)}
  </div> : <></>;
}

export default AwePdfViewer;
