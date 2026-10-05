const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

describe('awe-react-client/test/js/mainTest.jsx', () => {
  let fetchJson;
  let i18nInit;
  let render;

  // main.jsx boots the application when it is imported
  const loadMain = () => {
    jest.isolateModules(() => {
      require('../../src/main');
    });
  };

  beforeEach(() => {
    jest.resetModules();
    document.body.innerHTML = '<div id="root"></div>';
    fetchJson = jest.fn();
    i18nInit = jest.fn();
    render = jest.fn();

    jest.doMock('../../src/utilities', () => ({
      fetchJson,
      getContextPath: () => '/'
    }));
    jest.doMock('../../src/i18n/i18n', () => ({__esModule: true, default: {init: i18nInit}}));
    jest.doMock('../../src/components/AweApp', () => ({__esModule: true, default: () => null}));
    jest.doMock('../../src/redux/store', () => ({
      createStore: () => ({dispatch: jest.fn()}),
      setNavigateFn: jest.fn()
    }));
    jest.doMock('react-dom/client', () => ({createRoot: () => ({render})}));
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders the application once the settings are loaded and the translations are initialised', async () => {
    fetchJson.mockResolvedValue({language: 'es-ES'});
    i18nInit.mockResolvedValue();

    loadMain();
    await flushPromises();

    expect(i18nInit).toHaveBeenCalledWith(expect.objectContaining({lng: 'es-ES'}));
    expect(render).toHaveBeenCalledTimes(1);
  });

  it('protects the whole application with an error boundary, so a render error does not leave an empty page', async () => {
    fetchJson.mockResolvedValue({language: 'es-ES'});
    i18nInit.mockResolvedValue();

    loadMain();
    await flushPromises();

    const root = render.mock.calls[0][0];
    expect(root.type.name).toBe('ErrorBoundary');
    expect(root.props.scope).toBe('app');
  });

  it('keeps a colon in a label (a time such as "00:00" is not a namespace and a key)', async () => {
    fetchJson.mockResolvedValue({language: 'es-ES'});
    i18nInit.mockResolvedValue();

    loadMain();
    await flushPromises();

    expect(i18nInit).toHaveBeenCalledWith(expect.objectContaining({nsSeparator: false}));
  });

  it('logs the error and does not render when the settings cannot be loaded', async () => {
    const error = new Error('settings unavailable');
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    fetchJson.mockRejectedValue(error);

    loadMain();
    await flushPromises();

    expect(consoleError).toHaveBeenCalledWith('Error initialising the application:', error);
    expect(render).not.toHaveBeenCalled();
  });

  it('logs the error and does not render when the translations cannot be initialised', async () => {
    const error = new Error('locales unavailable');
    const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
    fetchJson.mockResolvedValue({language: 'es-ES'});
    i18nInit.mockRejectedValue(error);

    loadMain();
    await flushPromises();

    expect(consoleError).toHaveBeenCalledWith('Error initialising the application:', error);
    expect(render).not.toHaveBeenCalled();
  });
});
