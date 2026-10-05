import React, {useEffect} from "react";
import {useTranslation} from "react-i18next";
import {Helmet} from "react-helmet";
import Templates from "../templates";
import {useLocation, useParams} from "react-router";
import {loadScreen} from "../redux/thunks/screen";
import {useDispatch} from "react-redux";
import {ProgressSpinner} from "primereact/progressspinner";
import {TestIds, testHook} from "../utilities/testIds";
import ViewRegistry from "../redux/registry/ViewRegistry";
import { useView } from "../hooks/useViewRegistry";
import ErrorBoundary from "../components/ErrorBoundary";

const VIEW = "report";
import {translateLabel} from "../utilities";

/**
 * View container (funcional)
 * @category Containers
 * @subcategory View
 */
// Builds the templates of the view inside its error boundary: a structure that fails to build is caught there too
const ViewTemplates = ({structure}) => Templates(structure);

function SubViewContainer() {
  const {subScreenId} = useParams();
  const location = useLocation();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const view = useView(VIEW);
  const screenReloadToken = location.state?.screenReloadToken;

  useEffect(() => {
    dispatch(loadScreen(VIEW, subScreenId, t));
    return () => {
      ViewRegistry.clearView(VIEW);
    };
  }, [dispatch, subScreenId, screenReloadToken]);

  return view.loading ?
    <div className="expand grid animate__animated animate__fadeIn"><ProgressSpinner className="p-col align-self-center" pt={{root: testHook(TestIds.loadingSpinner)}}/></div> : (
    <ErrorBoundary scope="view" resetKey={`${subScreenId}-${screenReloadToken}`}>
      <div className={"expand expandible-vertical"}>
        <Helmet>
          <title>{translateLabel(view.title, t)}</title>
        </Helmet>
        <ViewTemplates structure={view.structure}/>
      </div>
    </ErrorBoundary>
  );
}

export default SubViewContainer;
