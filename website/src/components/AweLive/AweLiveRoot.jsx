import React, {useLayoutEffect, useMemo, useState} from 'react';
import {Provider} from 'react-redux';
import {configureStore} from '@reduxjs/toolkit';
import {I18nextProvider} from 'react-i18next';
import {PrimeReactProvider} from 'primereact/api';
import i18next from 'i18next';
import {useBaseUrlUtils} from '@docusaurus/useBaseUrl';
import createRootReducer from '@awe-client/redux/reducers';
import {DEFAULT_SETTINGS} from '@awe-client/redux/actions/settings';
import {buildPreloadedState, completeMessages} from './buildPreloadedState';
import {breadcrumbItems, releaseBreadcrumbs} from './breadcrumbs';
import MenuRegistry from '@awe-client/redux/registry/MenuRegistry';
import ComponentRegistry from '@awe-client/redux/registry/ComponentRegistry';
import {liveRegistration} from './registerComponents';
import {storeKey} from './storeKey';
import {LIVE_FRAME_ELEMENT, liveFrameProps} from './liveFrame';
import {liveView} from './liveView';
import {simulateFiles, simulateOptions, withBaseUrls} from './simulateFiles';
import {acceptsUpload, simulateUploads} from './simulateUploads';
import {updateModelWithDependencies} from '@awe-client/redux/thunks/components';
import {EN_TEXTS} from './locale';
// The client components load first: its modules import each other, and the thunks of the middleware must not be
// the entry into that cycle
import * as clientComponents from './clientComponents';
import {createClientActionsMiddleware} from './clientActionsMiddleware';
import MessageContainer from '@awe-client/containers/MessageContainer';
import './aweLiveStyles';

// English client without backend: the labels of the examples are literal; the few keys the client itself shows are in locale.js.
// initImmediate: false makes init synchronous with inline resources: `t` is ready on the first render
const createI18n = () => {
  const instance = i18next.createInstance();
  instance.init({
    lng: 'en',
    fallbackLng: 'en',
    resources: {en: {translation: EN_TEXTS}},
    // As the application: a colon is part of a text ("Plan: monthly"), not a namespace separator
    nsSeparator: false,
    interpolation: {escapeValue: false},
    initImmediate: false,
  });
  return instance;
};

// No ripple on the clicks, as the application of the client sets it (main.jsx)
const PRIME_REACT = {ripple: false};

// The client stores hold functions in some attributes (onChange, renderers): the check would reject them
// The client-only actions (open a dialog, next step) run for real: see clientActions.js
const createLiveStore = (preloadedState, ...middleware) => configureStore({
  reducer: createRootReducer(),
  preloadedState,
  middleware: getDefaultMiddleware =>
    getDefaultMiddleware({serializableCheck: false, immutableCheck: false}).prepend(...middleware),
});

const LiveFrame = LIVE_FRAME_ELEMENT;

// Examples mounted so far: each one gets a view of its own (see liveView.js)
let instances = 0;

/**
 * Own store and i18n per example: examples never share state. `bare` drops the frame of the example (a playground
 * stage draws its own) and `colorScheme` ('light' | 'dark') forces its colour mode: the AWE tokens read it through
 * light-dark(), so the example switches without any mode selector.
 *
 * The store is rebuilt when the CONTENT of the description changes, not when its identity does: MDX writes
 * `components={{...}}` inline, so the parent hands a new object on every render and a reference dependency would
 * drop the state of the example each time. A playground that edits attributes live changes the content, which gives
 * the example a fresh store with the new attributes.
 */
export default function AweLiveRoot({components, nodes, view, settings, files, options, uploads, messages, answers, breadcrumbs, height, bare, colorScheme, children}) {
  // Each example has a view of its own: the registry of the client is one for the page (radio groups, uploads)
  const [instance] = useState(() => ++instances);
  const storeView = liveView(view, instance);
  const key = storeKey({components, nodes, view: storeView, settings, files, messages, answers});
  const i18n = useMemo(createI18n, []);
  const [registration] = useState(() => liveRegistration(ComponentRegistry));
  // Examples of a page can share component ids (two playgrounds with a `demo`): the registry is one for the page, so
  // every action of this example first makes its base the one the registry shows, and the reducers merge with it
  const [raiseBeforeAction] = useState(() => () => next => action => {
    registration.raise();
    return next(action);
  });
  const store = useMemo(() => {
    const state = buildPreloadedState(components, {view: storeView, settings, messages});
    // The messages slice goes on top of what the reducer of the client starts with
    const reducer = createRootReducer();
    if (state.messages) state.messages = completeMessages(state.messages, reducer(undefined, {type: '@@awe-live/init'}).messages);
    // The actions stack starts with its stacks, as the reducer initialises it: dialogs push and pop their own
    return createLiveStore({
      ...state,
      settings: {...DEFAULT_SETTINGS, ...state.settings},
      actions: {sync: [[]], async: [], ...state.actions},
    }, raiseBeforeAction, createClientActionsMiddleware({t: i18n.t, answers}));
    // The key stands for components, nodes, view, settings, files, messages and answers
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);
  // As a screen load does in the application, the components are the base of the registry: the state only keeps
  // deltas (a group selection stores no address) and the merge takes the address from the base. The base of a new
  // store is registered before the example renders it (see liveRegistration); the registry is one for the page, and
  // this example gives back only what it registered when it unmounts
  registration.use(key, store.getState().components);
  // Registers again after a release (StrictMode runs the effects twice)
  useLayoutEffect(() => registration.use(key, store.getState().components), [registration, key, store]);
  useLayoutEffect(() => () => registration.release(), [registration]);

  // The files are urls of the static folder of the site: they go under its base url. The viewers read their file when
  // they mount (a passive effect), and a layout effect of the parent runs before it
  const {withBaseUrl} = useBaseUrlUtils();
  const filesKey = storeKey({files});
  useLayoutEffect(() => simulateFiles(withBaseUrls(files, withBaseUrl)),
    // The key stands for the content of files
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [filesKey, withBaseUrl]);

  // The searches of the suggests are answered from the options of the example (see optionFixtures.js)
  const optionsKey = storeKey({files: options});
  useLayoutEffect(() => simulateOptions(options),
    // The key stands for the content of options
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [optionsKey]);
  // The uploads never leave the page: the file chosen shows as the value of its criterion (see simulateUploads.js)
  useLayoutEffect(() => {
    if (!uploads) return undefined;
    return simulateUploads(({address, file}) => {
      if (file && acceptsUpload(address, storeView, store.getState().components)) {
        store.dispatch(updateModelWithDependencies(address, {
          values: [{value: file.name, label: file.name, size: file.size, selected: true}],
        }));
      }
    });
  }, [uploads, store, storeView]);

  // The window template draws the breadcrumb that the menu registers: the example registers the path it shows
  const breadcrumbsKey = storeKey({components: breadcrumbs});
  useLayoutEffect(() => {
    if (!breadcrumbs) return undefined;
    // The registry is one for the page: the last example that mounts shows its path, and one that unmounts clears
    // the registry only if its path is still the one in it
    const items = breadcrumbItems(breadcrumbs);
    MenuRegistry.setBreadcrumbs('live', items);
    return () => releaseBreadcrumbs(MenuRegistry, items);
    // The key stands for the content of breadcrumbs
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [breadcrumbsKey]);

  return (
    <I18nextProvider i18n={i18n}>
      {/* The store is rebuilt with the key: the tree remounts with it, because the components that set their model up on
          mount (an accordion with its open items) would keep the state of the old store */}
      <Provider key={key} store={store}>
        <PrimeReactProvider value={PRIME_REACT}>
        {/* A form, as the screens of the application: the radio groups of the example are its own (see liveFrame.js) */}
        <LiveFrame {...liveFrameProps({bare, height, colorScheme})}>
          {nodes ? nodes.map((node, index) => clientComponents.Components(node, index))
            : typeof children === 'function' ? children(clientComponents) : children}
          {/* The toasts, popovers and confirm dialog of the messages: drawn over the page, as the application does.
              Always there: a `confirm` or `message` action of any example needs it to show */}
          <MessageContainer />
        </LiveFrame>
        </PrimeReactProvider>
      </Provider>
    </I18nextProvider>
  );
}
