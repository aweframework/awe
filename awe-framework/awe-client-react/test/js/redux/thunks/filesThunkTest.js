import {fetchLogAction, fetchPdfAction} from '../../../../src/redux/thunks/files';
import {ADD_ACTIONS_TOP} from '../../../../src/redux/actions/actions';

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

describe('awe-react-client/test/js/redux/thunks/filesThunkTest.js', () => {
  let dispatch;
  let getState;

  beforeEach(() => {
    dispatch = jest.fn();
    getState = jest.fn().mockReturnValue({settings: {token: 'token'}, components: {}});
    global.fetch = jest.fn();
    window.fetch = global.fetch;
    window.URL.createObjectURL = jest.fn().mockReturnValue('blob:pdf');
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe('fetchLogAction', () => {
    it('prints the lines of the answer and runs its other actions on the viewer, as AngularJS does', async () => {
      const stop = {type: 'update-controller', silent: true, async: true, parameters: {attribute: 'autorefresh', value: 0}};
      global.fetch.mockResolvedValue({
        headers: {get: () => 'application/json;charset=UTF-8'},
        status: 200,
        ok: true,
        json: () => Promise.resolve([{type: 'log-delta', parameters: {log: ['a', 'b']}}, stop])
      });
      const viewer = {view: 'report', component: 'executionLogViewer'};
      const setLogText = jest.fn();
      const setOffset = jest.fn();

      await fetchLogAction('get-execution-log', '', 3, setLogText, setOffset, viewer)(dispatch, getState);

      expect(setLogText.mock.calls[0][0]('')).toBe('a\nb');
      expect(setOffset.mock.calls[0][0](3)).toBe(5);
      const queued = dispatch.mock.calls.map(([item]) => item).filter(item => item?.type === ADD_ACTIONS_TOP);
      expect(queued).toHaveLength(1);
      expect(queued[0].actions).toEqual([{...stop, address: viewer}]);
    });

    it('queues nothing when the answer only carries the lines', async () => {
      global.fetch.mockResolvedValue({
        headers: {get: () => 'application/json;charset=UTF-8'},
        status: 200,
        ok: true,
        json: () => Promise.resolve([{type: 'log-delta', parameters: {log: ['a']}}])
      });

      await fetchLogAction('get-execution-log', '', 0, jest.fn(), jest.fn(), {view: 'report', component: 'log'})(dispatch, getState);

      expect(dispatch.mock.calls.map(([item]) => item).filter(item => item?.type === ADD_ACTIONS_TOP)).toHaveLength(0);
    });
  });

  describe('fetchPdfAction', () => {
    it('hands the object url of the retrieved pdf to the caller', async () => {
      const blob = new Blob(['pdf']);
      global.fetch.mockResolvedValue({blob: () => Promise.resolve(blob)});
      const setPdf = jest.fn();

      fetchPdfAction('target', setPdf)(dispatch, getState);
      await flushPromises();

      expect(window.URL.createObjectURL).toHaveBeenCalledWith(blob);
      expect(setPdf).toHaveBeenCalledWith('blob:pdf');
    });

    it('logs the error and does not hand any pdf to the caller when the request fails', async () => {
      const error = new Error('network down');
      const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});
      global.fetch.mockRejectedValue(error);
      const setPdf = jest.fn();

      fetchPdfAction('target', setPdf)(dispatch, getState);
      await flushPromises();

      expect(consoleError).toHaveBeenCalledWith('Error reading pdf file:', error);
      expect(setPdf).not.toHaveBeenCalled();
    });
  });
});
