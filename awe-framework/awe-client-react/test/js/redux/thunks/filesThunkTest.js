import {fetchPdfAction} from '../../../../src/redux/thunks/files';

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
