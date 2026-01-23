import * as thunks from '../../../../src/redux/thunks/screen';

describe('awe-react-client/test/js/redux/thunks/screenThunkTest.js', () => {
  let dispatch;

  beforeEach(() => {
    dispatch = jasmine.createSpy('dispatch');
  });

  describe('logoutAction', () => {
    let originalCreateElement;
    let originalBodyAppendChild;
    let mockForm;

    beforeEach(() => {
      mockForm = {
        method: '',
        action: '',
        submit: jasmine.createSpy('submit')
      };
      originalCreateElement = document.createElement;
      originalBodyAppendChild = document.body.appendChild;

      document.createElement = jasmine.createSpy('createElement').and.returnValue(mockForm);
      document.body.appendChild = jasmine.createSpy('appendChild');
    });

    afterEach(() => {
      document.createElement = originalCreateElement;
      document.body.appendChild = originalBodyAppendChild;
    });

    it('should dispatch deleteStack and disconnectWebsocket, and submit a logout form', () => {
      thunks.logoutAction()(dispatch);

      // Verify dispatches
      expect(dispatch).toHaveBeenCalled();
      const calls = dispatch.calls.allArgs();
      
      // First call should be deleteStack
      expect(calls[0][0].type).toBe('DELETE_STACK');
      
      // Second call should be addActionsTop with disconnectWebsocket
      expect(calls[1][0].type).toBe('ADD_ACTIONS_TOP');
      expect(calls[1][0].actions[0].type).toBe('disconnectWebsocket');

      // Verify form creation and submission
      expect(document.createElement).toHaveBeenCalledWith('form');
      expect(mockForm.method).toBe('POST');
      expect(mockForm.action).toContain('/logout');
      expect(document.body.appendChild).toHaveBeenCalledWith(mockForm);
      expect(mockForm.submit).toHaveBeenCalled();
    });
  });
});
