import React, {useCallback, useEffect, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {Helmet} from "react-helmet";
import Templates from "../templates";
import {updateSize} from "../redux/actions/size";
import {addActionsTop} from "../redux/actions/actions";
import {useTranslation} from 'react-i18next';
import {useParams} from "react-router-dom";
import i18n from "../i18n/i18n";
import {loadScreen} from "../redux/thunks/screen";
import {ProgressSpinner} from "primereact/progressspinner";
import {translateLabel} from "../utilities";

const VIEW = "base";

/**
 * View container (funcional)
 * @category Containers
 * @subcategory View
 */
function ViewContainer() {
  const {screenId} = useParams();
  const prevSettings = useRef({});
  const prevScreenId = useRef(null);
  const {t} = useTranslation();

  // Accedemos a Redux usando los hooks de Redux
  const dispatch = useDispatch();

  const {settings, view} = useSelector((state) => ({
    settings: state.settings,
    view: state.view[VIEW],
  }));

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
    dispatch(updateSize({ width: window.innerWidth, height: window.innerHeight }));
  }, []);

  useEffect(() => {
    updateWindowDimensions();
    window.addEventListener('resize', updateWindowDimensions);
    return () => {
      window.removeEventListener('resize', updateWindowDimensions);
    };
  }, []);

  useEffect(() => {
    dispatch(addActionsTop([{ type: "connectWebsocket" }]));
    return () => {
      dispatch(addActionsTop([{ type: "disconnectWebsocket" }]));
    };
  }, []);

  useEffect(() => {
    if (prevScreenId.current !== screenId) {
      dispatch(loadScreen(VIEW, screenId, t));
    }
    prevScreenId.current = screenId;
  }, [screenId]);

  return view.loading ?
    <div className="expand grid animate__animated animate__fadeIn"><ProgressSpinner className="p-col align-self-center"/></div>: (
    <div className={"expand expandible-vertical"}>
      <Helmet>
        <title>{translateLabel(view.title, t)}</title>
      </Helmet>
      {Templates(view.structure)}
    </div>
  );
}

export default ViewContainer;
