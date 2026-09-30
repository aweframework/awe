import React, {useEffect} from "react";
import {PrimeReactProvider} from 'primereact/api';

import ReactDOM from "react-dom/client";

import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';
import 'primereact/resources/themes/saga-blue/theme.css';
import 'primereact/divider';

import './main.css';

import i18n from "./i18n/i18n";

import 'font-awesome/css/font-awesome.css';
import 'animate.css/animate.css';
import 'material-icons/iconfont/material-icons.css';
import './assets/css/layout.css';

// Pages
import './assets/css/pages/signin.css';
import './assets/css/pages/home.css';
import './assets/css/pages/error-pages.css';
import './assets/css/pages/sso-logout.css';

import AweApp from './components/AweApp';
import {DEFAULT_SETTINGS, updateSettings} from "./redux/actions/settings";
import {fetchJson, getContextPath} from "./utilities";
import {Provider} from "react-redux";
import {BrowserRouter, useNavigate} from "react-router";
import { createStore, setNavigateFn } from "./redux/store";

const store = createStore();

const AppWithStore = (props) => {
  const { settings } = props;
  const navigate = useNavigate();

  useEffect(() => {
    setNavigateFn(navigate);
    store.dispatch(updateSettings(settings));
  }, []);

  return (
    <Provider store={store}>
      <AweApp/>
    </Provider>
  );
};

// Init application
fetchJson("POST", "/settings", {}, DEFAULT_SETTINGS.cometUID)
  .then((settings) => {
    return i18n.init({
      //debug: true,
      backend: {
        loadPath: `${getContextPath()}/locales/{{lng}}`
      },
      lng: settings.language,
      fallbackLng: "en-GB",

      interpolation: {
        escapeValue: false
      }
    }).then(() => {
      const value = {ripple: true};
      const root = ReactDOM.createRoot(document.getElementById('root'));
      root.render(
        <BrowserRouter basename={getContextPath()}>
          <PrimeReactProvider value={value}>
            <AppWithStore settings={settings}/>
          </PrimeReactProvider>
        </BrowserRouter>);
    })
  })
  .catch((reason) => console.error("Error initialising the application:", reason));


