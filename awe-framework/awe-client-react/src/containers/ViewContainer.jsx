import React, {useCallback, useEffect, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {selectSettings} from "../redux/selectors/stateSelectors";
import {useDocumentTitle} from "../hooks/useDocumentHead";
import Templates from "../templates";
import SizeRegistry from "../redux/registry/SizeRegistry";
import {addActionsTop} from "../redux/actions/actions";
import {useTranslation} from 'react-i18next';
import {useLocation, useOutlet, useParams} from "react-router";
import i18n from "../i18n/i18n";
import {loadScreen} from "../redux/thunks/screen";
import AweScreenSkeleton, {endInitialLoad} from "../components/AweScreenSkeleton";
import {translateLabel} from "../utilities";
import { useView } from "../hooks/useViewRegistry";
import ViewRegistry from "../redux/registry/ViewRegistry";
import ErrorBoundary from "../components/ErrorBoundary";

const VIEW = "base";

/**
 * View container (funcional)
 * @category Containers
 * @subcategory View
 */
// Builds the templates of the view inside its error boundary: a structure that fails to build is caught there too
const ViewTemplates = ({structure}) => Templates(structure);

function ViewContainer() {
  const {screenId} = useParams();
  const location = useLocation();
  const prevSettings = useRef({});
  const {t} = useTranslation();

  // Accedemos a Redux usando los hooks de Redux
  const dispatch = useDispatch();

  const settings = useSelector(selectSettings);
  const view = useView(VIEW);
  const hasSubView = Boolean(useOutlet());
  const screenReloadToken = location.state?.screenReloadToken;
  useDocumentTitle(view.loading ? null : translateLabel(view.title, t), 0);

  const changeLanguage = useCallback((language) => {
    i18n.changeLanguage(language);
  }, []);

  // Check for updates in language and theme when props change
  useEffect(() => {
    // Set new language if it has changed
    if (settings.language !== prevSettings.language) {
      changeLanguage(settings.language);
    }

    prevSettings.current = {...settings};
  }, [settings]);

  const updateWindowDimensions = useCallback(() => {
    SizeRegistry.setSize({ width: window.innerWidth, height: window.innerHeight });
  }, []);

  useEffect(() => {
    updateWindowDimensions();
    window.addEventListener('resize', updateWindowDimensions);
    return () => {
      window.removeEventListener('resize', updateWindowDimensions);
    };
  }, []);

  useEffect(() => {
    dispatch(addActionsTop([{ type: "disconnectWebsocket" },{ type: "connectWebsocket", parameters: { token: settings.token } }]));
  }, [settings.token, dispatch]);

  useEffect(() => {
    // The initial load of the page ends with the screen, or with its sub screen when it has one (unless it fails)
    const endLoad = () => {
      if (!hasSubView || ViewRegistry.get(VIEW).loading) {
        endInitialLoad();
      }
    };
    Promise.resolve(dispatch(loadScreen(VIEW, screenId, t))).then(endLoad, endInitialLoad);
  }, [dispatch, screenId, screenReloadToken]);

  return view.loading ? <AweScreenSkeleton /> : (
    <ErrorBoundary scope="view" resetKey={`${screenId}-${screenReloadToken}`}>
      <div className={"expand expandible-vertical"} view-id={VIEW}>
        <ViewTemplates structure={view.structure}/>
      </div>
    </ErrorBoundary>
  );
}

export default ViewContainer;
