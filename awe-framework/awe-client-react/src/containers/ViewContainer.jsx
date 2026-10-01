import React, {useCallback, useEffect, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {Helmet} from "react-helmet";
import Templates from "../templates";
import SizeRegistry from "../redux/registry/SizeRegistry";
import {addActionsTop} from "../redux/actions/actions";
import {useTranslation} from 'react-i18next';
import {useLocation, useParams} from "react-router";
import i18n from "../i18n/i18n";
import {loadScreen} from "../redux/thunks/screen";
import {ProgressSpinner} from "primereact/progressspinner";
import {TestIds, testHook} from "../utilities/testIds";
import {translateLabel} from "../utilities";
import { useView } from "../hooks/useViewRegistry";

const VIEW = "base";

/**
 * View container (funcional)
 * @category Containers
 * @subcategory View
 */
function ViewContainer() {
  const {screenId} = useParams();
  const location = useLocation();
  const prevSettings = useRef({});
  const {t} = useTranslation();

  // Accedemos a Redux usando los hooks de Redux
  const dispatch = useDispatch();

  const {settings} = useSelector((state) => ({
    settings: state.settings,
  }));
  const view = useView(VIEW);
  const screenReloadToken = location.state?.screenReloadToken;

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
    dispatch(loadScreen(VIEW, screenId, t));
  }, [dispatch, screenId, screenReloadToken]);

  return view.loading ?
    <div className="expand grid animate__animated animate__fadeIn"><ProgressSpinner className="p-col align-self-center" pt={{root: testHook(TestIds.loadingSpinner)}}/></div>: (
    <div className={"expand expandible-vertical"}>
      <Helmet>
        <title>{translateLabel(view.title, t)}</title>
      </Helmet>
      {Templates(view.structure)}
    </div>
  );
}

export default ViewContainer;
