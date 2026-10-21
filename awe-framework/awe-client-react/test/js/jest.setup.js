global._ = require('lodash');

const { TextEncoder, TextDecoder } = require('util');
const i18n = require('i18next');
const { initReactI18next } = require('react-i18next');

if (!global.TextEncoder) {
  global.TextEncoder = TextEncoder;
}

if (!global.TextDecoder) {
  global.TextDecoder = TextDecoder;
}

// The suites of the chart server run in the node environment, without a DOM
const hasDom = typeof window !== 'undefined';

if (hasDom && !Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = jest.fn();
}

if (!i18n.isInitialized) {
  i18n.use(initReactI18next).init({
    lng: 'en-GB',
    fallbackLng: 'en-GB',
    resources: {
      'en-GB': {
        translation: {}
      }
    },
    interpolation: {
      escapeValue: false
    },
    initImmediate: false
  });
}

function getMessageText(args) {
  return args
    .map(arg => {
      if (typeof arg === 'string') {
        return arg;
      }
      if (arg instanceof Error) {
        return arg.message;
      }
      try {
        return JSON.stringify(arg);
      } catch (e) {
        return String(arg);
      }
    })
    .join(' ');
}

if (!global.__AWE_JEST_CONSOLE_FILTERS__) {
  const originalWarn = console.warn.bind(console);
  const originalError = console.error.bind(console);

  const suppressWarn = (message) => (
    message.includes('react-i18next:: You will need to pass in an i18next instance') ||
    message.includes('A non-serializable value was detected in an action') ||
    message.includes('A non-serializable value was detected in the state')
  );

  const suppressError = (message) => (
    message.includes('Could not parse CSS stylesheet')
  );

  // Development checks of react-redux and reselect: a selector that returns a new result for the same state renders the
  // component again on every dispatch, so it fails the test instead of only warning
  const selectorInstability = (message) => (
    /Selector .* returned (a different result when called with the same parameters|the root state when called)/.test(message) ||
    message.includes('An input selector returned a different result when passed same arguments') ||
    message.includes('The result function returned its own inputs without modification')
  );

  console.warn = (...args) => {
    const message = getMessageText(args);
    if (selectorInstability(message)) {
      throw new Error(message);
    }
    if (suppressWarn(message)) {
      return;
    }
    return originalWarn(...args);
  };

  console.error = (...args) => {
    const message = getMessageText(args);
    if (suppressError(message)) {
      return;
    }
    return originalError(...args);
  };

  global.__AWE_JEST_CONSOLE_FILTERS__ = true;
}

// Mock window.matchMedia — jsdom does not implement it.
// Required by PrimeReact components (PickList, etc.) that use media queries.
if (hasDom) Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});
