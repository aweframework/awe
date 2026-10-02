import * as screenThunks from '../../../../src/redux/thunks/screen';
import { ADD_ACTIONS_TOP } from '../../../../src/redux/actions/actions';
import { navigationActions } from '../../../../src/redux/actions/navigation';
import { clearClassChanges, rememberClassChange } from '../../../../src/utilities/classChanges';

describe('awe-react-client/test/js/redux/thunks/screenThunkTest.js', () => {
  let dispatch;
  let getState;

  beforeEach(() => {
    dispatch = jest.fn();
    getState = jest.fn().mockReturnValue({
      settings: {},
      components: {}
    });
    global.fetch = jest.fn();
    window.fetch = global.fetch;
    // Clean body between tests
    document.body.innerHTML = '';
  });

  describe('logoutAction', () => {
    it('debería limpiar el stack, desconectar el websocket y enviar un formulario de logout', () => {
      // Espiamos el submit para no navegar y poder comprobar que se invoca
      const submitSpy = jest.spyOn(HTMLFormElement.prototype, 'submit').mockImplementation(() => {});

      // Act
      screenThunks.logoutAction()(dispatch, getState);

      // Assert dispatches
      expect(dispatch).toHaveBeenCalled();
      const calls = dispatch.mock.calls.map(a => a[0]);
      // 1) DELETE_STACK
      expect(calls[0].type).toBe('DELETE_STACK');
      // 2) ADD_ACTIONS_TOP con disconnectWebsocket
      expect(calls[1].type).toBe(ADD_ACTIONS_TOP);
      expect(Array.isArray(calls[1].payload)).toBe(true);
      expect(calls[1].payload[0]).toEqual({ type: 'disconnectWebsocket' });

      // Assert formulario creado y enviado
      expect(submitSpy).toHaveBeenCalled();
      const form = document.body.querySelector('form');
      expect(form).not.toBeNull();
      expect(form.method.toLowerCase()).toBe('post');
      expect(typeof form.action).toBe('string');
      expect(form.action).toContain('/action/logout');
    });
  });

  describe('redirectAction', () => {
    it('debería abrir en nueva ventana cuando newWindow=true y aceptar la acción', () => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      const action = { target: 'https://example.com', parameters: { newWindow: true } };

      screenThunks.redirectAction(action)(dispatch, getState);

      expect(openSpy).toHaveBeenCalledWith('https://example.com', '_blank');
      const last = dispatch.mock.calls.at(-1)[0];
      expect(last.type).toBe('ACCEPT_ACTION');
    });

    it('debería redirigir en la misma ventana por defecto y aceptar la acción', () => {
      const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);
      const action = { target: 'https://example.com' };

      screenThunks.redirectAction(action)(dispatch, getState);

      expect(openSpy).toHaveBeenCalledWith('https://example.com', '_self');
      const last = dispatch.mock.calls.at(-1)[0];
      expect(last.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('changeLanguageAction', () => {
    it('debería actualizar el idioma desde parámetros y aceptar', () => {
      const action = { parameters: { language: 'es' } };
      screenThunks.changeLanguageAction(action)(dispatch, getState);
      const types = dispatch.mock.calls.map(a => a[0].type);
      expect(types[0]).toBe('UPDATE_SETTINGS');
      expect(types[1]).toBe('ACCEPT_ACTION');
    });

    it('debería tomar el idioma del componente target si no viene en parámetros', () => {
      const state = {
        settings: {},
        components: {
          langComp: { model: { values: [{ value: 'fr', selected: true }] } }
        }
      };
      getState.mockReturnValue(state);
      const action = { parameters: { target: 'langComp' } };
      screenThunks.changeLanguageAction(action)(dispatch, getState);
      const [update] = dispatch.mock.calls.map(a => a[0]).filter(a => a.type === 'UPDATE_SETTINGS');
      expect(update.payload.language).toBe('fr');
    });
  });

  describe('changeThemeAction', () => {
    it('debería actualizar el tema desde parámetros y aceptar', () => {
      const action = { parameters: { theme: 'dark' } };
      screenThunks.changeThemeAction(action)(dispatch, getState);
      const types = dispatch.mock.calls.map(a => a[0].type);
      expect(types[0]).toBe('UPDATE_SETTINGS');
      expect(types[1]).toBe('ACCEPT_ACTION');
    });

    it('debería tomar el tema del componente target si no viene en parámetros', () => {
      const state = {
        settings: {},
        components: {
          themeComp: { model: { values: [{ value: 'light', selected: true }] } }
        }
      };
      getState.mockReturnValue(state);
      const action = { parameters: { target: 'themeComp' } };
      screenThunks.changeThemeAction(action)(dispatch, getState);
      const [update] = dispatch.mock.calls.map(a => a[0]).filter(a => a.type === 'UPDATE_SETTINGS');
      expect(update.payload.theme).toBe('light');
    });
  });

  describe('reloadScreenAction', () => {
    it('debería navegar con replace:true y aceptar', () => {
      jest.spyOn(navigationActions, 'navigateTo').mockImplementation((target, options) => ({ type: 'NAVIGATE_TO', payload: { target, options } }));
      const action = { type: 'screen', target: '/home' };
      screenThunks.reloadScreenAction(action, '/home')(dispatch, getState);

      const [nav, accept] = dispatch.mock.calls.map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.options.replace).toBe(true);
      expect(accept.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('backAction', () => {
    it('debería navegar hacia atrás y aceptar', () => {
      jest.spyOn(navigationActions, 'navigateTo').mockImplementation((target, options) => ({ type: 'NAVIGATE_TO', payload: { target, options } }));
      const action = { type: 'back' };
      screenThunks.backAction(action)(dispatch, getState);

      const [nav, accept] = dispatch.mock.calls.map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.target).toBe(-1);
      expect(accept.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('screenAction', () => {
    beforeEach(() => {
      jest.spyOn(navigationActions, 'navigateTo').mockImplementation((target, options) => ({ type: 'NAVIGATE_TO', payload: { target, options } }));
    });

    it('navega a ruta absoluta y acepta', () => {
      const action = { parameters: { screen: '/absoluta' }, target: '/absoluta' };
      screenThunks.screenAction(action, '/otro')(dispatch, () => ({ settings: {} }));
      const [nav, accept] = dispatch.mock.calls.map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.target).toBe('/absoluta');
      expect(accept.type).toBe('ACCEPT_ACTION');
    });

    it('navega a ruta relativa usando context', () => {
      const action = { context: 'orders', parameters: { screen: 'list' }, target: 'list' };
      screenThunks.screenAction(action, '/app/home')(dispatch, () => ({ settings: {} }));
      const [nav] = dispatch.mock.calls.map(a => a[0]);
      expect(nav.payload.target).toBe('/orders/list');
      expect(nav.payload.options.relative).toBe('path');
    });

    it('navega a ruta relativa usando parent del pathname cuando no hay context', () => {
      const action = { parameters: { screen: 'd' }, target: 'd' };
      screenThunks.screenAction(action, '/a/b/c')(dispatch, () => ({ settings: {} }));
      const [nav] = dispatch.mock.calls.map(a => a[0]);
      expect(nav.payload.target).toBe('/a/b/d');
    });

    it('actualiza token si viene en parámetros', () => {
      const action = { parameters: { token: 'TKN', screen: '/home' }, target: '/home' };
      screenThunks.screenAction(action, '/old')(dispatch, () => ({ settings: {} }));
      const types = dispatch.mock.calls.map(a => a[0].type);
      expect(types[0]).toBe('UPDATE_SETTINGS');
    });

    it('si target es igual al pathname y reloadCurrentScreen=true, hace reload', () => {
      const action = { parameters: { screen: '/same' }, target: '/same' };
      screenThunks.screenAction(action, '/same')(dispatch, () => ({ settings: { reloadCurrentScreen: true } }));

      // Debe despacharse una función (thunk) de reloadScreenAction como segundo dispatch
      const calls = dispatch.mock.calls;
      expect(typeof calls[0][0]).toBe('function');

      // Ejecutamos ese thunk y comprobamos que navega con replace:true
      const innerDispatch = jest.fn();
      calls[0][0](innerDispatch);
      const [nav, accept] = innerDispatch.mock.calls.map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.options.replace).toBe(true);
      expect(accept.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('loadScreen', () => {
    it('gestiona error cuando no hay structure enviando mensaje', async () => {
      jest.spyOn(window, 'fetch').mockReturnValue(Promise.resolve({ status: 500, message: 'ERR' }));
      const t = (k) => k;
      await screenThunks.loadScreen('v', 'op', t)(dispatch, () => ({ settings: { token: 'TOK' } }));
      const addTop = dispatch.mock.calls.map(a => a[0]).find(a => a.type === ADD_ACTIONS_TOP);
      expect(addTop).toBeDefined();
    });

    it('dispatches response.actions on initial screen load', async () => {
      jest.spyOn(window, 'fetch').mockResolvedValue({
        ok: true,
        json: async () => ({
          structure: {},
          components: [],
          messages: [],
          screen: {},
          actions: [
            {
              type: 'message',
              parameters: {
                type: 'error',
                title: 'Initial load failed',
                message: 'Backend action should be shown'
              }
            }
          ]
        })
      });

      await screenThunks.loadScreen('view-a', 'option-a', (key) => key)(dispatch, () => ({
        settings: { token: 'TOK' },
        components: {}
      }));

      const actionDispatch = dispatch.mock.calls
        .map(a => a[0])
        .find(a => a?.type === ADD_ACTIONS_TOP && a.payload?.[0]?.type === 'message');

      expect(actionDispatch).toBeDefined();
      expect(actionDispatch.payload).toEqual([
        expect.objectContaining({
          type: 'message',
          parameters: expect.objectContaining({
            title: 'Initial load failed',
            message: 'Backend action should be shown'
          })
        })
      ]);
    });

    it('forgets the remembered class changes of the previous screen when a screen is loaded', async () => {
      jest.spyOn(window, 'fetch').mockResolvedValue({
        ok: true,
        json: async () => ({ structure: {}, components: [], messages: [], screen: {} })
      });
      rememberClassChange('#previousScreenNode', 'hidden', 'remove');

      await screenThunks.loadScreen('view-a', 'option-a', (key) => key)(dispatch, () => ({
        settings: { token: 'TOK' },
        components: {}
      }));

      const node = document.createElement('div');
      node.id = 'previousScreenNode';
      node.className = 'hidden';
      document.body.appendChild(node);
      await Promise.resolve();
      expect(node.classList.contains('hidden')).toBe(true);
      clearClassChanges();
    });

    it('navega sin mensaje UI cuando el estado contiene componentes malformados sin address', async () => {
      const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
      jest.spyOn(window, 'fetch').mockResolvedValue({
        ok: true,
        json: async () => ({
          structure: {},
          components: [],
          messages: [],
          screen: {}
        })
      });

      await screenThunks.loadScreen('view-a', 'option-a', (key) => key)(dispatch, () => ({
        settings: { token: 'TOK' },
        components: {
          malformed: {
            uid: 'broken-load-screen',
            attributes: { id: 'broken-load-screen', component: 'text' },
            model: { values: [{ value: 'bad', selected: true }] },
            storedModel: { values: [{ value: 'bad', selected: true }] },
            context: { view: 'broken-view' }
          }
        }
      }));

      const addTop = dispatch.mock.calls
        .map(a => a[0])
        .find(a => a?.type === ADD_ACTIONS_TOP && Array.isArray(a.payload) && a.payload.some(item => item?.type === 'message'));

      expect(addTop).toBeUndefined();
      expect(warnSpy).toHaveBeenCalledWith(
        expect.stringContaining('[AWE] Malformed component detected in selector:collectFormValues'),
        expect.objectContaining({
          componentUid: 'broken-load-screen',
          attributesId: 'broken-load-screen',
          view: 'broken-view'
        })
      );
    });
  });

  describe('getFileAction', () => {
    const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

    it('accepts the action once the file is downloaded', async () => {
      const action = { type: 'get-file', parameters: {} };
      global.fetch.mockResolvedValue({
        headers: { get: () => 'file.txt' },
        blob: () => Promise.resolve(new Blob(['content']))
      });
      window.URL.createObjectURL = jest.fn().mockReturnValue('blob:file');
      jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

      screenThunks.getFileAction(action)(dispatch, getState);
      await flushPromises();

      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ type: 'ACCEPT_ACTION' }));
    });

    it('logs the error and does not accept the action when the download fails', async () => {
      const action = { type: 'get-file', parameters: {} };
      const error = new Error('network down');
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
      global.fetch.mockRejectedValue(error);

      screenThunks.getFileAction(action)(dispatch, getState);
      await flushPromises();

      expect(consoleError).toHaveBeenCalledWith('Error downloading file:', error);
      expect(dispatch).not.toHaveBeenCalled();
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete global.fetch;
    delete window.fetch;
  });
});
