import * as screenThunks from '../../../../src/redux/thunks/screen';
import { ADD_ACTIONS_TOP } from '../../../../src/redux/actions/actions';
import { navigationActions } from '../../../../src/redux/actions/navigation';

describe('awe-react-client/test/js/redux/thunks/screenThunkTest.js', () => {
  let dispatch;
  let getState;

  beforeEach(() => {
    dispatch = jasmine.createSpy('dispatch');
    getState = jasmine.createSpy('getState').and.returnValue({
      settings: {},
      components: {}
    });
    // Clean body between tests
    document.body.innerHTML = '';
  });

  describe('logoutAction', () => {
    it('debería limpiar el stack, desconectar el websocket y enviar un formulario de logout', () => {
      // Espiamos el submit para no navegar y poder comprobar que se invoca
      const submitSpy = spyOn(HTMLFormElement.prototype, 'submit').and.callFake(() => {});

      // Act
      screenThunks.logoutAction()(dispatch, getState);

      // Assert dispatches
      expect(dispatch).toHaveBeenCalled();
      const calls = dispatch.calls.allArgs().map(a => a[0]);
      // 1) DELETE_STACK
      expect(calls[0].type).toBe('DELETE_STACK');
      // 2) ADD_ACTIONS_TOP con disconnectWebsocket
      expect(calls[1].type).toBe(ADD_ACTIONS_TOP);
      expect(Array.isArray(calls[1].payload)).toBeTrue();
      expect(calls[1].payload[0]).toEqual({ type: 'disconnectWebsocket' });

      // Assert formulario creado y enviado
      expect(submitSpy).toHaveBeenCalled();
      const form = document.body.querySelector('form');
      expect(form).withContext('Debe haberse añadido un <form> al body').not.toBeNull();
      expect(form.method.toLowerCase()).toBe('post');
      expect(typeof form.action).toBe('string');
      expect(form.action).toContain('/action/logout');
    });
  });

  describe('redirectAction', () => {
    it('debería abrir en nueva ventana cuando newWindow=true y aceptar la acción', () => {
      const openSpy = spyOn(window, 'open');
      const action = { target: 'https://example.com', parameters: { newWindow: true } };

      screenThunks.redirectAction(action)(dispatch, getState);

      expect(openSpy).toHaveBeenCalledWith('https://example.com', '_blank');
      const last = dispatch.calls.mostRecent().args[0];
      expect(last.type).toBe('ACCEPT_ACTION');
    });

    it('debería redirigir en la misma ventana por defecto y aceptar la acción', () => {
      const openSpy = spyOn(window, 'open');
      const action = { target: 'https://example.com' };

      screenThunks.redirectAction(action)(dispatch, getState);

      expect(openSpy).toHaveBeenCalledWith('https://example.com', '_self');
      const last = dispatch.calls.mostRecent().args[0];
      expect(last.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('changeLanguageAction', () => {
    it('debería actualizar el idioma desde parámetros y aceptar', () => {
      const action = { parameters: { language: 'es' } };
      screenThunks.changeLanguageAction(action)(dispatch, getState);
      const types = dispatch.calls.allArgs().map(a => a[0].type);
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
      getState.and.returnValue(state);
      const action = { parameters: { target: 'langComp' } };
      screenThunks.changeLanguageAction(action)(dispatch, getState);
      const [update] = dispatch.calls.allArgs().map(a => a[0]).filter(a => a.type === 'UPDATE_SETTINGS');
      expect(update.payload.language).toBe('fr');
    });
  });

  describe('changeThemeAction', () => {
    it('debería actualizar el tema desde parámetros y aceptar', () => {
      const action = { parameters: { theme: 'dark' } };
      screenThunks.changeThemeAction(action)(dispatch, getState);
      const types = dispatch.calls.allArgs().map(a => a[0].type);
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
      getState.and.returnValue(state);
      const action = { parameters: { target: 'themeComp' } };
      screenThunks.changeThemeAction(action)(dispatch, getState);
      const [update] = dispatch.calls.allArgs().map(a => a[0]).filter(a => a.type === 'UPDATE_SETTINGS');
      expect(update.payload.theme).toBe('light');
    });
  });

  describe('reloadScreenAction', () => {
    it('debería navegar con replace:true y aceptar', () => {
      spyOn(navigationActions, 'navigateTo').and.callFake((target, options) => ({ type: 'NAVIGATE_TO', payload: { target, options } }));
      const action = { type: 'screen', target: '/home' };
      screenThunks.reloadScreenAction(action, '/home')(dispatch, getState);

      const [nav, accept] = dispatch.calls.allArgs().map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.options.replace).toBeTrue();
      expect(accept.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('backAction', () => {
    it('debería navegar hacia atrás y aceptar', () => {
      spyOn(navigationActions, 'navigateTo').and.callFake((target, options) => ({ type: 'NAVIGATE_TO', payload: { target, options } }));
      const action = { type: 'back' };
      screenThunks.backAction(action)(dispatch, getState);

      const [nav, accept] = dispatch.calls.allArgs().map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.target).toBe(-1);
      expect(accept.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('screenAction', () => {
    beforeEach(() => {
      spyOn(navigationActions, 'navigateTo').and.callFake((target, options) => ({ type: 'NAVIGATE_TO', payload: { target, options } }));
    });

    it('navega a ruta absoluta y acepta', () => {
      const action = { parameters: { screen: '/absoluta' }, target: '/absoluta' };
      screenThunks.screenAction(action, '/otro')(dispatch, () => ({ settings: {} }));
      const [nav, accept] = dispatch.calls.allArgs().map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.target).toBe('/absoluta');
      expect(accept.type).toBe('ACCEPT_ACTION');
    });

    it('navega a ruta relativa usando context', () => {
      const action = { context: 'orders', parameters: { screen: 'list' }, target: 'list' };
      screenThunks.screenAction(action, '/app/home')(dispatch, () => ({ settings: {} }));
      const [nav] = dispatch.calls.allArgs().map(a => a[0]);
      expect(nav.payload.target).toBe('/orders/list');
      expect(nav.payload.options.relative).toBe('path');
    });

    it('navega a ruta relativa usando parent del pathname cuando no hay context', () => {
      const action = { parameters: { screen: 'd' }, target: 'd' };
      screenThunks.screenAction(action, '/a/b/c')(dispatch, () => ({ settings: {} }));
      const [nav] = dispatch.calls.allArgs().map(a => a[0]);
      expect(nav.payload.target).toBe('/a/b/d');
    });

    it('actualiza token si viene en parámetros', () => {
      const action = { parameters: { token: 'TKN', screen: '/home' }, target: '/home' };
      screenThunks.screenAction(action, '/old')(dispatch, () => ({ settings: {} }));
      const types = dispatch.calls.allArgs().map(a => a[0].type);
      expect(types[0]).toBe('UPDATE_SETTINGS');
    });

    it('si target es igual al pathname y reloadCurrentScreen=true, hace reload', () => {
      const action = { parameters: { screen: '/same' }, target: '/same' };
      screenThunks.screenAction(action, '/same')(dispatch, () => ({ settings: { reloadCurrentScreen: true } }));

      // Debe despacharse una función (thunk) de reloadScreenAction como segundo dispatch
      const calls = dispatch.calls.allArgs();
      expect(typeof calls[0][0]).toBe('function');

      // Ejecutamos ese thunk y comprobamos que navega con replace:true
      const innerDispatch = jasmine.createSpy('innerDispatch');
      calls[0][0](innerDispatch);
      const [nav, accept] = innerDispatch.calls.allArgs().map(a => a[0]);
      expect(nav.type).toBe('NAVIGATE_TO');
      expect(nav.payload.options.replace).toBeTrue();
      expect(accept.type).toBe('ACCEPT_ACTION');
    });
  });

  describe('loadScreen', () => {
    it('gestiona error cuando no hay structure enviando mensaje', async () => {
      spyOn(window, 'fetch').and.returnValue(Promise.resolve({ status: 500, message: 'ERR' }));
      const t = (k) => k;
      await screenThunks.loadScreen('v', 'op', t)(dispatch, () => ({ settings: { token: 'TOK' } }));
      const addTop = dispatch.calls.allArgs().map(a => a[0]).find(a => a.type === ADD_ACTIONS_TOP);
      expect(addTop).toBeDefined();
    });
  });
});
