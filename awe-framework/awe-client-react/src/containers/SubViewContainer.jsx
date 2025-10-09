import React, {useEffect, useRef} from "react";
import {useTranslation} from "react-i18next";
import {Helmet} from "react-helmet";
import Templates from "../templates";
import {useParams} from "react-router-dom";
import {loadScreen} from "../redux/thunks/screen";
import {useDispatch, useSelector} from "react-redux";
import {ProgressSpinner} from "primereact/progressspinner";
import {clearView} from "../redux/actions/view";

const VIEW = "report";
import {translateLabel} from "../utilities";

/**
 * View container (funcional)
 * @category Containers
 * @subcategory View
 */
function SubViewContainer() {
  const {subScreenId} = useParams();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const prevScreenId = useRef(null);
  const {view} = useSelector((state) => ({view: state.view[VIEW]}));

  useEffect(() => {
    if (prevScreenId.current !== subScreenId) {
      dispatch(loadScreen(VIEW, subScreenId, t));
    }
    prevScreenId.current = subScreenId;
    return () => {
      dispatch(clearView(VIEW));
    };
  }, [subScreenId]);

  return view.loading ?
    <div className="expand grid animate__animated animate__fadeIn"><ProgressSpinner className="p-col align-self-center"/></div> : (
    <div className={"expand expandible-vertical"}>
      <Helmet>
        <title>{translateLabel(view.title, t)}</title>
      </Helmet>
      {Templates(view.structure)}
    </div>
  );
}

export default SubViewContainer;
