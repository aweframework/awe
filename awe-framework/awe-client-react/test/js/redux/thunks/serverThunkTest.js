import {serverDownloadAction} from '../../../../src/redux/thunks/server';
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
