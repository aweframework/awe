import {serverAction, serverDownloadAction} from '../../../../src/redux/thunks/server';
import {runRefusal} from '../../../../src/redux/thunks/refusal';
import {saveGridLayout} from '../../../../src/redux/thunks/gridLayout';
import {launchWindowService} from '../../../../src/redux/thunks/windowAdvancedState';
import {fetchLogAction} from '../../../../src/redux/thunks/files';
import {ACCEPT_ACTION, ADD_ACTIONS_TOP} from '../../../../src/redux/actions/actions';
import {configureStore} from '@reduxjs/toolkit';
import createRootReducer from '../../../../src/redux/reducers';
import {DEFAULT_SETTINGS} from '../../../../src/redux/actions/settings';

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

describe('awe-react-client/test/js/redux/thunks/serverThunkTest.js', () => {
  let dispatch;
  let getState;
  let action;

  beforeEach(() => {
    dispatch = jest.fn();
    const address = {view: 'base', component: 'grid1'};
    getState = jest.fn().mockReturnValue({
      settings: {token: 'token', targetActionKey: 'targetAction'},
      components: {grid1: {address, specificAttributes: {targetAction: 'download-target'}}}
    });
    action = {type: 'server-download', address, parameters: {}};
    global.fetch = jest.fn();
    window.fetch = global.fetch;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('serverAction', () => {
    it('sets the error attributes of the action on its component when the call fails (#824)', async () => {
      const failing = {view: 'base', component: 'Dia'};
      global.fetch.mockRejectedValue(new Error('network down'));
      jest.spyOn(console, 'error').mockImplementation(() => {});

      const result = await serverAction({
        type: 'server', address: failing, errorAttributes: {deferredLoaded: false},
        parameters: {serverAction: 'deferred-data', targetAction: 'Dia'}
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));

      expect(result).toBe(false);
      const update = dispatch.mock.calls.map(([call]) => call).find(call => call.type === 'UPDATE_ATTRIBUTES');
      expect(update.address).toEqual(failing);
      expect(update.data).toEqual({deferredLoaded: false});
    });

    describe('action called from a cell of a grid (#341)', () => {
      const gridAddress = {view: 'report', component: 'GrdUsr'};
      const newStore = (selected, multiselect = false) => configureStore({
        reducer: createRootReducer({}),
        preloadedState: {
          settings: DEFAULT_SETTINGS,
          actions: {running: false},
          components: {
            GrdUsr: {
              address: gridAddress,
              attributes: {id: 'GrdUsr', component: 'grid', multiselect, columnModel: [{name: 'Name', sendable: true}]},
              model: {values: [1, 2, 3].map(id => ({id, Name: `N${id}`, selected: selected.includes(id)}))},
              storedModel: {values: []}
            }
          }
        },
        middleware: getDefaultMiddleware => getDefaultMiddleware({serializableCheck: false, immutableCheck: false})
      });
      const callFrom = async (store, address) => {
        global.fetch.mockResolvedValue({ok: true, json: () => Promise.resolve([])});
        await store.dispatch(serverAction({
          type: 'server', address, parameters: {serverAction: 'color-cell', targetAction: 'target'}
        }));
        return JSON.parse(global.fetch.mock.calls[0][1].body);
      };

      it('sends the row and the column of the cell that calls it, selecting its row first', async () => {
        const store = newStore([1, 3]);

        const sent = await callFrom(store, {...gridAddress, row: 2, column: 'Name'});

        expect(sent['GrdUsr.selectedRowAddress']).toEqual({...gridAddress, row: 2, column: 'Name'});
        expect(sent['GrdUsr.selected']).toBe(2);
        expect(store.getState().components.GrdUsr.model.values.filter(row => row.selected).map(row => row.id)).toEqual([2]);
      });

      it('keeps the selected rows of a multiselect grid, still sending the cell that calls it', async () => {
        const store = newStore([1, 3], true);

        const sent = await callFrom(store, {...gridAddress, row: 2, column: 'Name'});

        expect(sent['GrdUsr.selectedRowAddress']).toEqual({...gridAddress, row: 2, column: 'Name'});
        expect(store.getState().components.GrdUsr.model.values.filter(row => row.selected).map(row => row.id)).toEqual([1, 3]);
      });

      it('sends the address of the selected row, without column, when the action does not come from a cell', async () => {
        const store = newStore([1]);

        const sent = await callFrom(store, {view: 'report', component: 'ButSea'});

        expect(sent['GrdUsr.selectedRowAddress']).toEqual({...gridAddress, row: 1});
      });
    });

    describe('action launched from a menu option (#934)', () => {
      const text = (view, id, value) => ({
        address: {view, component: id},
        attributes: {id, component: 'text'},
        model: {values: [{value, label: value, selected: true}]},
        storedModel: {values: [{value, label: value, selected: true}]}
      });
      const state = () => ({
        settings: {token: 'token'},
        components: {Menu: text('base', 'Menu', 'm'), Name: text('report', 'Name', 'leaked')}
      });
      const call = async (origin, answer = []) => {
        global.fetch.mockResolvedValue({ok: true, json: () => Promise.resolve(answer)});
        await serverAction({
          type: 'server', origin, address: {view: 'base', component: 'Menu'},
          parameters: {serverAction: 'screen', targetAction: 'target'}
        })(dispatch, state);
        return JSON.parse(global.fetch.mock.calls[0][1].body);
      };

      it('sends the values of the base view, not the ones of the report view', async () => {
        const sent = await call('menu');

        expect(sent.Menu).toBe('m');
        expect(sent).not.toHaveProperty('Name');
      });

      it('keeps sending the values of every view when the action is not from a menu', async () => {
        expect((await call(undefined)).Name).toBe('leaked');
      });

      it('passes its origin to the actions of the response', async () => {
        await call('menu', [{type: 'screen'}]);

        const added = dispatch.mock.calls.map(([c]) => c).find(c => c.type === 'ADD_ACTIONS_TOP');
        expect(added.payload[0].origin).toBe('menu');
      });
    });

    describe('loading of the target', () => {
      const launcher = {view: 'report', component: 'ButSea'};
      const target = {view: 'report', component: 'ChrLinTst'};
      const updates = () => dispatch.mock.calls.map(([call]) => call).filter(call => call.type === 'UPDATE_ATTRIBUTES');
      const launchOn = (actionTarget) => serverAction({
        type: 'server', address: launcher, target: actionTarget,
        parameters: {serverAction: 'data', targetAction: 'TstChrTwoSrc'}
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));

      it('marks the target of the action as loading before calling the server, as the AngularJS client does', async () => {
        let updatesAtCall = null;
        global.fetch.mockImplementation(() => {
          updatesAtCall = updates().map(({address: updated, data}) => ({address: updated, data}));
          return Promise.resolve({ok: true, json: () => Promise.resolve([])});
        });

        const result = await launchOn('ChrLinTst');

        expect(result).toBe(true);
        expect(updatesAtCall).toEqual([{address: target, data: {loading: true}}]);
        // The response (fill, end-load) ends the load: the thunk does not end it on success
        expect(updates()).toHaveLength(1);
      });

      it('ends the loading of the target when the call fails', async () => {
        global.fetch.mockRejectedValue(new Error('network down'));
        jest.spyOn(console, 'error').mockImplementation(() => {});

        const result = await launchOn('ChrLinTst');

        expect(result).toBe(false);
        expect(updates().map(({address: updated, data}) => ({address: updated, data}))).toEqual([
          {address: target, data: {loading: true}},
          {address: target, data: {loading: false}}
        ]);
      });

      it('marks nothing as loading when the action has no target', async () => {
        global.fetch.mockResolvedValue({ok: true, json: () => Promise.resolve([])});

        await launchOn(undefined);

        expect(updates()).toHaveLength(0);
      });
    });

    const address = {view: 'base', component: 'GrdTskVar'};
    // Editable send-all grid with a column named "value", like the scheduler launch variables dialog
    const grid = {
      address,
      attributes: {
        id: 'GrdTskVar',
        component: 'grid',
        sendAll: true,
        editable: true,
        columnModel: [
          {name: 'name', sendable: true},
          {name: 'value', sendable: true}
        ]
      },
      model: {selected: null, values: [{id: 2, name: 'secondsToWait', value: '3'}]}
    };

    const launch = async (parameters) => {
      getState.mockReturnValue({settings: {token: 'token'}, components: {GrdTskVar: grid}});
      global.fetch.mockResolvedValue({ok: true, json: () => Promise.resolve([])});
      await serverAction({type: 'server', address, parameters})(dispatch, getState);
      return JSON.parse(global.fetch.mock.calls[0][1].body);
    };

    it('sends the grid column values even when the action carries a null parameter with the same name', async () => {
      const body = await launch({serverAction: 'maintain-silent', targetAction: 'LchTskVar', value: null, label: null});

      expect(body.name).toEqual(['secondsToWait']);
      expect(body.value).toEqual(['3']);
    });

    it('keeps the server and target action of the action parameters', async () => {
      const body = await launch({serverAction: 'maintain-silent', targetAction: 'LchTskVar'});

      expect(body.serverAction).toBe('maintain-silent');
      expect(body.targetAction).toBe('LchTskVar');
    });
  });

  describe('refusal answers (#935)', () => {
    const address = {view: 'report', component: 'ButRefused'};
    const target = {view: 'report', component: 'Dia'};
    const launcher = () => serverAction({
      type: 'server', address, target: 'Dia',
      parameters: {serverAction: 'data', targetAction: 'Dia'}
    })(dispatch, () => ({settings: {token: 'token'}, components: {}}));
    const answer = (status, text) => global.fetch.mockResolvedValue({ok: false, status, text: () => Promise.resolve(text)});
    const added = () => dispatch.mock.calls.map(([call]) => call).filter(call => call.type === ADD_ACTIONS_TOP).flatMap(call => call.payload);
    const updates = () => dispatch.mock.calls.map(([call]) => call).filter(call => call.type === 'UPDATE_ATTRIBUTES');

    it('processes a 403 whose body is a list of client actions exactly like a 200 answer (#935, S3)', async () => {
      answer(403, JSON.stringify([
        {type: 'end-load'},
        {type: 'message', parameters: {type: 'error', title: 'ERROR_TITLE_ACCESS_DENIED', message: 'ERROR_MESSAGE_ACTION_NOT_PERMITTED'}},
        {type: 'cancel'}
      ]));

      const result = await serverAction({
        type: 'server', address, target: 'Dia', origin: 'menu',
        parameters: {serverAction: 'data', targetAction: 'Dia'}
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));

      expect(result).toBe(true);
      expect(added().map(a => a.type)).toEqual(['end-load', 'message', 'cancel']);
      expect(added().find(a => a.type === 'message').parameters.title).toBe('ERROR_TITLE_ACCESS_DENIED');
      // Every action of the body runs in the context the other actions of the answer would run: target and origin
      expect(added()[0].address.component).toBe('Dia');
      expect(added()[1].origin).toBe('menu');
      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({type: ACCEPT_ACTION}));
    });

    it('shows an access-denied message when the 403 body is not a list of actions, instead of staying silent (#935, S3)', async () => {
      answer(403, 'Access to the maintain has been denied');
      jest.spyOn(console, 'error').mockImplementation(() => {});

      const result = await launcher();

      expect(result).toBe(false);
      expect(added().map(a => a.type)).toEqual(['end-load', 'message']);
      expect(added()[1].parameters).toEqual({
        type: 'error', title: 'ERROR_TITLE_ACCESS_DENIED', message: 'Access to the maintain has been denied'
      });
      // The loading state ended anyway
      expect(updates().at(-1).data).toEqual({loading: false});
    });

    it('falls back to the locale key when the 403 body text is markup, so no markup is shown (#935, S3)', async () => {
      answer(403, '<html><body>Access denied</body></html>');
      jest.spyOn(console, 'error').mockImplementation(() => {});

      await launcher();

      expect(added()[1].parameters).toEqual({
        type: 'error', title: 'ERROR_TITLE_ACCESS_DENIED', message: 'ERROR_MESSAGE_ACTION_NOT_PERMITTED'
      });
    });

    it('answers a 401 with the session-expired client action and ends the load (#935, S5, S6)', async () => {
      answer(401, JSON.stringify({message: 'ERROR_MESSAGE_SESSION_EXPIRED'}));
      jest.spyOn(console, 'error').mockImplementation(() => {});

      const result = await launcher();

      expect(result).toBe(false);
      expect(added().map(a => a.type)).toEqual(['end-load', 'session-expired']);
      expect(updates().at(-1).data).toEqual({loading: false});
      expect(dispatch).not.toHaveBeenCalledWith(expect.objectContaining({type: ACCEPT_ACTION}));
    });

    it('answers a 404 with the not-found message, never with the raw body or a session message (#935, S6, S7)', async () => {
      answer(404, '<html><body>Page Not Found</body></html>');
      jest.spyOn(console, 'error').mockImplementation(() => {});

      await launcher();

      expect(added().map(a => a.type)).toEqual(['end-load', 'message']);
      expect(added()[1].parameters).toEqual({
        type: 'error', title: 'ERROR_TITLE_NOT_FOUND', message: 'ERROR_MESSAGE_NOT_FOUND'
      });
      expect(added().find(a => a.type === 'session-expired')).toBeUndefined();
    });

    it('keeps today\'s behaviour on any other non-ok answer (#935, S3)', async () => {
      answer(500, 'Internal server error');
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

      const result = await launcher();

      expect(result).toBe(false);
      expect(added()).toHaveLength(0);
      expect(updates().at(-1).data).toEqual({loading: false});
      expect(consoleError).toHaveBeenCalled();
    });
  });

  describe('refusals of every client request (#935, S6)', () => {
    const address = {view: 'report', component: 'GrdLay'};
    const addedOf = () => dispatch.mock.calls.map(([call]) => call).filter(call => call.type === ADD_ACTIONS_TOP).flatMap(call => call.payload);
    const sessionExpiredAdded = () => addedOf().find(a => a.type === 'session-expired');
    const notOk = (status, text = JSON.stringify({message: 'no session'})) =>
      global.fetch.mockResolvedValue({ok: false, status, text: () => Promise.resolve(text)});
    const locals = () => dispatch.mock.calls.map(([call]) => call).filter(call => call.type === ADD_ACTIONS_TOP)
      .flatMap(call => call.payload)
      .filter((a) => a.type === 'message' && a.address?.component === 'GrdLay');

    it('answers a 401 through the same refusal rule the shared helper runs for every caller', () => {
      const outcome = runRefusal({message: 'no session', status: 401, body: {message: 'ignored'}}, dispatch);

      expect(outcome).toBe('refusal');
      expect(addedOf().map(a => a.type)).toEqual(['session-expired']);
    });

    it('runs a 403 body of client actions exactly like a 200 answer through the shared helper', () => {
      const body = [{type: 'end-load'}, {type: 'cancel'}];

      const outcome = runRefusal({message: 'empty', status: 403, body}, dispatch);

      expect(outcome).toBe('actions');
      expect(addedOf()).toEqual(body);
    });

    it('leaves any other answer of the shared helper to the caller', () => {
      expect(runRefusal({message: 'boom', status: 500}, dispatch)).toBeNull();
      expect(runRefusal(new Error('network down'), dispatch)).toBeNull();
      expect(runRefusal(undefined, dispatch)).toBeNull();
      expect(dispatch).not.toHaveBeenCalled();
    });

    it('answers a 401 of a grid layout service with the session-expired panel, not with its own failure (#935, S6)', async () => {
      notOk(401);

      const result = await saveGridLayout(address, 'scr', {order: [], hidden: []}, {
        title: 'ERROR_TITLE_GRID_LAYOUT', message: 'ERROR_MESSAGE_GRID_LAYOUT'
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));

      expect(result).toBe(false);
      expect(sessionExpiredAdded()).toBeDefined();
      // The service failed, but the panel is the only answer the user sees
      expect(locals()).toHaveLength(0);
    });

    it('answers a 401 of a window service with the session-expired panel, not with its own failure (#935, S6)', async () => {
      notOk(401);

      const result = await launchWindowService('saveWindowAdvancedState', {view: 'base', component: 'WinAdv'}, {}, {
        title: 'ERROR_TITLE_WINDOW_STATE', message: 'ERROR_MESSAGE_WINDOW_STATE'
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));

      expect(result).toBeNull();
      expect(sessionExpiredAdded()).toBeDefined();
      expect(addedOf().some(a => a.type === 'message' && a.parameters?.title === 'ERROR_TITLE_WINDOW_STATE')).toBe(false);
    });

    it('runs the client actions of the 403 body of a window service exactly like a 200 answer (#935, S6)', async () => {
      notOk(403, JSON.stringify([
        {type: 'end-load'},
        {type: 'message', parameters: {type: 'error', title: 'ERROR_TITLE_ACCESS_DENIED', message: 'ERROR_MESSAGE_ACTION_NOT_PERMITTED'}},
        {type: 'cancel'}
      ]));

      const result = await launchWindowService('saveWindowAdvancedState', {view: 'base', component: 'WinAdv'}, {}, {
        title: 'ERROR_TITLE_WINDOW_STATE', message: 'ERROR_MESSAGE_WINDOW_STATE'
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));

      expect(result).toBeNull();
      expect(addedOf().map(a => a.type)).toEqual(['end-load', 'message', 'cancel']);
      expect(addedOf().some(a => a.type === 'message' && a.parameters?.title === 'ERROR_TITLE_WINDOW_STATE')).toBe(false);
    });

    it('answers a 401 of the log file request with the session-expired panel instead of silently failing (#935, S6)', async () => {
      notOk(401);
      const setLogText = jest.fn();
      const setOffset = jest.fn();

      fetchLogAction('slice-log', 'TgtLog', 0, setLogText, setOffset)(dispatch, getState);
      await flushPromises();

      expect(sessionExpiredAdded()).toBeDefined();
      expect(setLogText).not.toHaveBeenCalled();
      expect(setOffset).not.toHaveBeenCalled();
    });

    it('keeps the failure path of every caller on any other failed answer (#935, S6)', async () => {
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

      // Grid layout: its own error message
      notOk(500);
      const gridResult = await saveGridLayout(address, 'scr', {order: [], hidden: []}, {
        title: 'ERROR_TITLE_GRID_LAYOUT', message: 'ERROR_MESSAGE_GRID_LAYOUT'
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));
      expect(gridResult).toBe(false);
      expect(addedOf().find(a => a.type === 'message' && a.parameters?.title === 'ERROR_TITLE_GRID_LAYOUT')).toBeDefined();

      // Window service: its own error message and a null answer
      const windowResult = await launchWindowService('saveWindowAdvancedState', {view: 'base', component: 'WinAdv'}, {}, {
        title: 'ERROR_TITLE_WINDOW_STATE', message: 'ERROR_MESSAGE_WINDOW_STATE'
      })(dispatch, () => ({settings: {token: 'token'}, components: {}}));
      expect(windowResult).toBeNull();
      expect(addedOf().find(a => a.type === 'message' && a.parameters?.title === 'ERROR_TITLE_WINDOW_STATE')).toBeDefined();

      // Log file: logged error, no log update
      notOk(500);
      const setLogText = jest.fn();
      const setOffset = jest.fn();
      fetchLogAction('slice-log', 'TgtLog', 0, setLogText, setOffset)(dispatch, getState);
      await flushPromises();
      expect(consoleError).toHaveBeenCalledWith('Error reading log file:', expect.anything());
      expect(setLogText).not.toHaveBeenCalled();
    });
  });

  describe('serverDownloadAction', () => {
    it('accepts the action once the file is downloaded', async () => {
      global.fetch.mockResolvedValue({
        headers: {get: () => 'file.txt'},
        blob: () => Promise.resolve(new Blob(['content']))
      });
      window.URL.createObjectURL = jest.fn().mockReturnValue('blob:file');
      jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

      serverDownloadAction(action)(dispatch, getState);
      await flushPromises();

      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({type: ACCEPT_ACTION}));
    });

    it('logs the error and does not accept the action when the download fails', async () => {
      const error = new Error('network down');
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
      global.fetch.mockRejectedValue(error);

      serverDownloadAction(action)(dispatch, getState);
      await flushPromises();

      expect(consoleError).toHaveBeenCalledWith('Error downloading file:', error);
      expect(dispatch).not.toHaveBeenCalled();
    });
  });
});
