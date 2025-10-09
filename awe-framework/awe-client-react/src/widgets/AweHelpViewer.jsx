import React, { useEffect, useState, useCallback } from "react";
import { translateLabel } from "../utilities";
import "./AweHelpViewer.less";
import { Skeleton } from "primereact/skeleton";
import {useTranslation} from "react-i18next";
import {useDispatch} from "react-redux";
import {isEmpty} from "../utilities/general";
import {fetchHelpAction} from "../redux/thunks/files";

/**
 * AWE Help Viewer component (Functional)
 * @category Widgets
 */
function AweHelpViewer(props) {
  const { id } = props;
  const { t } = useTranslation();
  const [help, setHelp] = useState("");
  const dispatch = useDispatch();

  const translateHelp = useCallback(
    (text) => text.replace(/\{t\{'([\w\.\$ ]*)'\}\}/g, (m, label) => translateLabel(label, t)),
    [t]
  );

  useEffect(() => {
    dispatch(fetchHelpAction(setHelp));
  }, []);

  const skeletonTemplate = (key) => (
    <li className="mb-3" key={`skeleton-${key}`}>
      <div className="flex">
        <Skeleton shape="circle" size="4rem" className="mr-2" />
        <div style={{ flex: "1" }}>
          <Skeleton width="100%" className="mb-2" />
          <Skeleton width="75%" />
        </div>
      </div>
    </li>
  );

  if (isEmpty(help)) {
    const skeletons = [1, 2, 3, 4, 5];
    return (
      <ul className="m-0 p-0" style={{ width: "800px" }} role="alert" aria-busy="true">
        {skeletons.map(skeletonTemplate)}
      </ul>
    );
  }

  return (
    <div className="help-viewer m-4" id={id} dangerouslySetInnerHTML={{ __html: translateHelp(help) }} />
  );
}

export default AweHelpViewer;
