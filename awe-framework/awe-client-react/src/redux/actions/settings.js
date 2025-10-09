/*
 * action types
 */
export const UPDATE_SETTINGS = 'UPDATE_SETTINGS';

/*
 * action functions
 */

/**
 * Retrieve an uuid
 * @return {string} UUID
 */
export function getUID() {
  function s4() {
    return Math.floor((1 + Math.random()) * 0x10000)
      .toString(16)
      .substring(1);
  }
  return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
}

/*
 * initial settings
 */
const token = getUID();
export const DEFAULT_SETTINGS = {
  // Paths
  pathServer: "./",
  initialURL: "./",
  // Globals
  language: null,
  theme: "default",
  screen: "",
  charset: "UTF-8",
  applicationName: "AWE (Almis Web Engine)",
  serverActionKey: "serverAction",
  targetActionKey: "targetAction",
  screenKey: "screen",
  optionKey: "option",
  dataSuffix: ".data",
  homeScreen: "home",
  recordsPerPage: 30,
  pixelsPerCharacter: 7,
  defaultComponentSize: "md",
  shareSessionInTabs: false,
  reloadCurrentScreen: false,
  suggestTimeout: 200,
  // Connection
  connectionProtocol: "AJAX",
  connectionTransport: "websocket",
  connectionBackup: "streaming",
  connectionTimeout: 300000,
  connectionId: "s",
  cometUID: token,
  token: token,
  // Upload / Download
  uploadIdentifier: 'u',
  uploadMaxSize: 500 * 1024 * 1024,
  downloadIdentifier: 'd',
  addressIdentifier: 'address',
  // Security
  passwordPattern: ".*",
  minlengthPassword: 4,
  encodeTransmission: false,
  encodeKey: "p",
  tokenKey: "t",
  // Debug
  actionsStack: 0,
  debug: "INFO",
  // Screen loading
  loadingTimeout: 20000,
  // Help
  helpTimeout: 1000,
  // Messages
  messagePosition: "bottom-center",
  messageTimeout: {
    info: 2000,
    error: 0,
    validate: 2000,
    help: 4000,
    warning: 4000,
    ok: 2000,
    wrong: 0,
    chat: 0
  },
  // Numeric options
  numericOptions: {
    digitGroupSeparator: ',',
    digitalGroupSpacing: '3',
    decimalCharacter: '.',
    currencySymbol: '',
    currencySymbolPlacement: 'p',
    minimumValue: '-9999999999.99',
    maximumValue: '9999999999.99',
    decimalPlaces: 2,
    roundingMethod: 'S',
    //allowDecimalPadding: false,
    emptyInputBehavior: 'null'
  },
  // Pivot options
  pivotOptions: {
    numGroup: 5000
  },
  // Chart options
  chartOptions: {
    limitPointsSerie: 1000000
  },
  activeDependencies: true
};

/*
 * action creators
 */
export function updateSettings(payload) {
  return { type: UPDATE_SETTINGS, payload };
}
