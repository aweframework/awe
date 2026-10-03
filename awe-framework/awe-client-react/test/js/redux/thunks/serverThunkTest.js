import {serverAction, serverDownloadAction} from '../../../../src/redux/thunks/server';
import {ACCEPT_ACTION} from '../../../../src/redux/actions/actions';

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
