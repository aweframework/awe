import * as thunks from '../../../../src/redux/thunks/messages';

describe('awe-react-client/test/js/redux/thunks/messagesThunkTest.js', () => {
  let dispatch;
  let getState;
  let mockState;
  let mockTranslate;

  beforeEach(() => {
    dispatch = jest.fn();
    mockTranslate = jest.fn((key) => `translated_${key}`);

    mockState = {
      settings: {
        messageTimeout: {
          ok: 3000,
          info: 5000,
          error: 0,
          warning: 6000
        }
      },
      messages: {
        defined: {
          baseView: {
            'error-message': {
              title: 'Error_Title',
              message: 'Error_Message'
            },
            'success-message': {
              title: 'Success_Title',
              message: 'Success_Message'
            },
            'warning-message': {
              title: 'Warning_Title',
              message: 'Warning_Message'
            }
          }
        }
      }
    };

    getState = jest.fn(() => mockState);
  });

  describe('messageAction', () => {
    it('debería despachar addMessage con un mensaje de error', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'error',
          title: 'Error',
          message: 'Something_went_wrong'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.type).toBe('ADD_MESSAGE');
      expect(addMessage.data.severity).toBe('error');
      expect(addMessage.data.summary).toBe('translated_Error');
      expect(addMessage.data.detail).toBe('translated_Something_went_wrong');
      expect(addMessage.data.sticky).toBe(true); // error timeout = 0
      expect(addMessage.data.life).toBe(0);
      expect(addMessage.data.closable).toBe(true);
    });

    it('debería despachar addMessage con un mensaje de éxito', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'ok',
          title: 'Success',
          message: 'Operation_completed'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.type).toBe('ADD_MESSAGE');
      expect(addMessage.data.severity).toBe('success');
      expect(addMessage.data.summary).toBe('translated_Success');
      expect(addMessage.data.detail).toBe('translated_Operation_completed');
      expect(addMessage.data.sticky).toBe(false);
      expect(addMessage.data.life).toBe(3000);
    });

    it('debería despachar addMessage con un mensaje de advertencia', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'warning',
          title: 'Warning',
          message: 'Be_careful'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.type).toBe('ADD_MESSAGE');
      expect(addMessage.data.severity).toBe('warn');
      expect(addMessage.data.summary).toBe('translated_Warning');
      expect(addMessage.data.detail).toBe('translated_Be_careful');
      expect(addMessage.data.sticky).toBe(false);
      expect(addMessage.data.life).toBe(6000);
    });

    it('debería despachar addMessage con un mensaje usando target predefinido', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'error',
          target: 'error-message'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.type).toBe('ADD_MESSAGE');
      expect(addMessage.data.severity).toBe('error');
      expect(addMessage.data.summary).toBe('translated_Error_Title');
      expect(addMessage.data.detail).toBe('translated_Error_Message');
    });

    it('debería manejar mensaje con tipo "wrong" como error', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'wrong',
          title: 'Wrong',
          message: 'Invalid input'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.data.severity).toBe('error');
      expect(addMessage.data.sticky).toBe(true);
      expect(addMessage.data.life).toBe(0);
    });

    it('debería usar tipo por defecto para tipos desconocidos', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'custom-type',
          title: 'Custom',
          message: 'Custom message'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.data.severity).toBe('custom-type');
      expect(addMessage.data.life).toBe(5000); // default info timeout
    });

    it('debería incrementar el messageId en cada llamada', () => {
      const action1 = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'ok',
          title: 'Message 1',
          message: 'First message'
        }
      };

      const action2 = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'ok',
          title: 'Message 2',
          message: 'Second message'
        }
      };

      thunks.messageAction(action1, mockTranslate)(dispatch, getState);
      const [[firstMessage]] = dispatch.mock.calls;
      const firstId = firstMessage.data.id;

      dispatch.mockClear();

      thunks.messageAction(action2, mockTranslate)(dispatch, getState);
      const [[secondMessage]] = dispatch.mock.calls;
      const secondId = secondMessage.data.id;

      expect(secondId).toBeGreaterThan(firstId);
    });

    it('debería incluir la acción en el mensaje despachado', () => {
      const action = {
        type: 'message',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          type: 'ok',
          title: 'Test',
          message: 'Test message'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.data.action).toBe(action);
    });

    it('debería usar view desde action.view si no hay address', () => {
      const action = {
        type: 'message',
        view: 'baseView',
        parameters: {
          type: 'error',
          target: 'error-message'
        }
      };

      thunks.messageAction(action, mockTranslate)(dispatch, getState);

      const [[addMessage]] = dispatch.mock.calls;
      expect(addMessage.data.summary).toBe('translated_Error_Title');
      expect(addMessage.data.detail).toBe('translated_Error_Message');
    });
  });

  describe('confirmAction', () => {
    it('debería despachar confirmMessage con título y mensaje', () => {
      const action = {
        type: 'confirm',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          title: 'Confirm_Action',
          message: 'Are_you_sure?'
        }
      };

      thunks.confirmAction(action, mockTranslate)(dispatch, getState);

      expect(dispatch).toHaveBeenCalled();

      const [[confirmMessage]] = dispatch.mock.calls;
      expect(confirmMessage.type).toBe('CONFIRM_MESSAGE');
      expect(confirmMessage.data.title).toBe('translated_Confirm_Action');
      expect(confirmMessage.data.message).toBe('translated_Are_you_sure?');
      expect(confirmMessage.data.action).toBe(action);
    });

    it('debería despachar confirmMessage usando target predefinido', () => {
      const action = {
        type: 'confirm',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {
          target: 'warning-message'
        }
      };

      thunks.confirmAction(action, mockTranslate)(dispatch, getState);

      const [[confirmMessage]] = dispatch.mock.calls;
      expect(confirmMessage.type).toBe('CONFIRM_MESSAGE');
      expect(confirmMessage.data.title).toBe('translated_Warning_Title');
      expect(confirmMessage.data.message).toBe('translated_Warning_Message');
      expect(confirmMessage.data.action).toBe(action);
    });

    it('debería usar view desde action.view si no hay address', () => {
      const action = {
        type: 'confirm',
        view: 'baseView',
        parameters: {
          target: 'success-message'
        }
      };

      thunks.confirmAction(action, mockTranslate)(dispatch, getState);

      const [[confirmMessage]] = dispatch.mock.calls;
      expect(confirmMessage.data.title).toBe('translated_Success_Title');
      expect(confirmMessage.data.message).toBe('translated_Success_Message');
    });

    it('debería manejar parámetros vacíos', () => {
      const action = {
        type: 'confirm',
        address: { view: 'baseView', component: 'comp1' },
        parameters: {}
      };

      thunks.confirmAction(action, mockTranslate)(dispatch, getState);

      const [[confirmMessage]] = dispatch.mock.calls;
      expect(confirmMessage.type).toBe('CONFIRM_MESSAGE');
      expect(confirmMessage.data.title).toBe('');
      expect(confirmMessage.data.message).toBe('');
      expect(confirmMessage.data.action).toBe(action);
    });
  });
});
