const js = require('@eslint/js');

// Globals available in the browser bundle (AngularJS app + jQuery plugins loaded by webpack/ProvidePlugin)
const browserGlobals = {
  window: 'readonly',
  document: 'readonly',
  navigator: 'readonly',
  location: 'readonly',
  history: 'readonly',
  localStorage: 'readonly',
  sessionStorage: 'readonly',
  console: 'readonly',
  setTimeout: 'readonly',
  clearTimeout: 'readonly',
  setInterval: 'readonly',
  clearInterval: 'readonly',
  requestAnimationFrame: 'readonly',
  URL: 'readonly',
  Blob: 'readonly',
  File: 'readonly',
  FileReader: 'readonly',
  FormData: 'readonly',
  XMLHttpRequest: 'readonly',
  WebSocket: 'readonly',
  Audio: 'readonly',
  Image: 'readonly',
  Event: 'readonly',
  CustomEvent: 'readonly',
  MouseEvent: 'readonly',
  self: 'readonly',
  structuredClone: 'readonly',
  MutationObserver: 'readonly',
  getComputedStyle: 'readonly',
  atob: 'readonly',
  btoa: 'readonly',
  angular: 'readonly',
  $: 'readonly',
  jQuery: 'readonly',
  _: 'readonly',
  moment: 'readonly',
  Highcharts: 'readonly',
  module: 'readonly',
  require: 'readonly',
  process: 'readonly',
  global: 'readonly',
  __dirname: 'readonly'
};

const jestGlobals = {
  describe: 'readonly',
  it: 'readonly',
  test: 'readonly',
  expect: 'readonly',
  beforeEach: 'readonly',
  afterEach: 'readonly',
  beforeAll: 'readonly',
  afterAll: 'readonly',
  jest: 'readonly',
  inject: 'readonly'
};

module.exports = [
  {
    ignores: [
      'node_modules/**',
      'target/**',
      'coverage/**',
      // Vendored third party libraries are not ours to lint
      'src/main/resources/js/lib/**'
    ]
  },
  js.configs.recommended,
  {
    files: ['src/main/resources/js/awe/**/*.js', 'src/test/jest/**/*.js'],
    languageOptions: {
      ecmaVersion: 2020,
      sourceType: 'module',
      globals: {
        ...browserGlobals
      }
    },
    rules: {
      semi: 2,
      'no-unused-vars': 1,
      eqeqeq: 1
    }
  },
  {
    // Jest specs and their setup rely on the Jest and angular-mocks globals
    files: ['src/test/jest/**/*.js'],
    languageOptions: {
      globals: {
        ...jestGlobals
      }
    }
  }
];
