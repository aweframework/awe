import React, {Suspense, useCallback, useEffect, useState} from "react";
import {classNames} from "../utilities/components";
import {PDFObject} from "../utilities/lazyPackages";
import "./AwePdfViewer.less";
import {Skeleton} from "primereact/skeleton";
import {useDispatch} from "react-redux";
import {useComponentState} from "../hooks/useComponentState";
import {fetchPdfAction} from "../redux/thunks/files";
import PropTypes from "prop-types";

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
    const placeholder = <li className="mb-3">
      <div className="flex">
        <Skeleton shape="circle" size="4rem" className="mr-2" />
        <div style={{ flex: '1' }}>
          <Skeleton width="100%" className="mb-2" />
          <Skeleton width="75%" />
        </div>
      </div>
    </li>;
    // The PDF object is a heavy package loaded on demand: the same placeholder shows while it loads
    return pdf ? <Suspense fallback={placeholder}><PDFObject url={pdf} /></Suspense> : placeholder;
  }, []);

  const { style, visible = true } = attributes;
  return visible ? <div id={id} className={classNames("pdf-viewer", "expand", style)}>
    {getPdfTemplate(pdf)}
  </div> : <></>;
}

AwePdfViewer.propTypes = {
  id: PropTypes.string,
};

export default AwePdfViewer;
