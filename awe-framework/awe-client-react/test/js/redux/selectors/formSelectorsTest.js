import { getFormValues } from '../../../../src/redux/selectors/form';
import { getModelValidation } from '../../../../src/redux/selectors/modelValidation';

describe('awe-react-client/test/js/redux/selectors/formSelectorsTest.js', () => {
  let warnSpy;

  beforeEach(() => {
    warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('skips malformed components without blocking form value collection', () => {
    const state = {
      settings: {},
      components: {
        valid: {
          uid: 'valid-uid',
          address: { view: 'base', component: 'valid' },
          attributes: { id: 'valid', component: 'text' },
          model: { values: [{ value: 'ok', label: 'ok', selected: true }] },
          storedModel: { values: [{ value: 'ok', label: 'ok', selected: true }] }
        },
        malformed: {
          uid: 'broken-uid',
          attributes: { id: 'broken', component: 'text' },
          model: { values: [{ value: 'bad', selected: true }] },
          storedModel: { values: [{ value: 'bad', selected: true }] },
          context: { view: 'broken-view' }
        }
      }
    };

    expect(getFormValues(state)).toEqual({ valid: 'ok' });
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('[AWE] Malformed component detected in selector:collectFormValues'),
      expect.objectContaining({
        componentKey: 'malformed',
        componentUid: 'broken-uid',
        attributesId: 'broken',
        view: 'broken-view'
      })
    );
  });

  it('skips malformed components in model validation checks', () => {
    const state = {
      settings: {},
      components: {
        valid: {
          address: { view: 'base', component: 'valid' },
          attributes: { id: 'valid', component: 'text', checkEmpty: true },
          model: { values: [{ value: 'changed', label: 'changed', selected: true }] },
          storedModel: { values: [{ value: 'initial', label: 'initial', selected: true }] }
        },
        malformed: {
          uid: 'broken-validation',
          attributes: { id: 'broken', component: 'text', checkEmpty: true },
          model: { values: [{ value: 'ignored', selected: true }] },
          storedModel: { values: [{ value: 'ignored', selected: true }] },
          context: { view: 'validation-view' }
        }
      }
    };

    expect(getModelValidation(state)).toEqual({
      isEmptyModel: false,
      isUpdatedModel: true,
      isUnchangedModel: false
    });
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('[AWE] Malformed component detected in selector:getModelValidation'),
      expect.objectContaining({
        componentKey: 'malformed',
        componentUid: 'broken-validation',
        attributesId: 'broken',
        view: 'validation-view'
      })
    );
  });
});
