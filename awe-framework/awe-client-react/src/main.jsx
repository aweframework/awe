import React from "react";
import {PrimeReactProvider} from 'primereact/api';

import {configureStore} from '@reduxjs/toolkit';
import createRootReducer from "./redux/reducers";
import {history} from "./redux/store";
import {thunk} from "redux-thunk";
import {render} from "react-dom";

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

import AweApp from './components/AweApp';
import {DEFAULT_SETTINGS, updateSettings} from "./redux/actions/settings";
import {fetchJson, getContextPath} from "./utilities";

const store = configureStore({
  preloadedState: {settings: DEFAULT_SETTINGS},
  reducer: createRootReducer(history),
  devTools: true,
  middleware: [thunk]
});

fetchJson("POST", "/settings", {}, DEFAULT_SETTINGS.cometUID)
  .then((settings) => {
    store.dispatch(updateSettings(settings));
    i18n.init({
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
      // Initialise application
      const value = { ripple: true };
      render(
        <PrimeReactProvider value={value}><AweApp store={store} settings={settings}/></PrimeReactProvider>,
        document.getElementById('root')
      );
    });
  });
