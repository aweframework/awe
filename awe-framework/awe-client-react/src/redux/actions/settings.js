/*
 * action types
 */
export const UPDATE_SETTINGS = 'UPDATE_SETTINGS';

/*
 * action functions
 */

/**
 * Retrieve an uuid (version 4)
 * @return {string} UUID
 */
export function getUID() {
  const cryptoApi = globalThis.crypto;
  // crypto.randomUUID is only available in secure contexts (https, localhost)
  if (typeof cryptoApi?.randomUUID === 'function') {
    return cryptoApi.randomUUID();
  }

  // crypto.getRandomValues is available everywhere: build the version 4 UUID by hand
  const bytes = cryptoApi.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
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
  activeDependencies: true,
  useComponentRegistry: true,
  // Menu option search (command palette) is enabled unless explicitly disabled
  menuSearchEnabled: true
};

/*
 * action creators
 */
export function updateSettings(payload) {
  return { type: UPDATE_SETTINGS, payload };
}
