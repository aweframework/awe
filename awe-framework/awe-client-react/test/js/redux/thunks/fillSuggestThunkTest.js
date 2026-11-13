import {createStore} from '../../../../src/redux/store';
import {updateViewComponentsWithDependencies} from '../../../../src/redux/thunks/components';
import {fillAction, fillSuggestAction, selectAction} from '../../../../src/redux/thunks/form';
import {getComponent, getSelectedValues} from '../../../../src/utilities';
import {getAllComponents} from '../../../../src/redux/selectors/componentSelectors';

/**
 * fill-suggest (FillSuggestActionBuilder, deprecated in #804) fills a suggest with its values and selects all of
 * them, like the AngularJS engine does. React runs it as a fill with every row marked as selected.
 */
describe('awe-react-client/test/js/redux/thunks/fillSuggestThunkTest.js', () => {
  const address = {view: 'report', component: 'Sug'};
  const values = [{value: 'aaa', label: 'Label aaa'}, {value: 'bbb', label: 'Label bbb'}];
  const fillSuggest = (parameters) => ({type: 'fill-suggest', address, parameters});

  let store;

  const suggest = (from = store) => getComponent(getAllComponents(from.getState()), address);

  /**
   * Store with a suggest that is loading and has a previous value selected
   */
  const storeWithSuggest = () => {
    const created = createStore();
    created.dispatch(updateViewComponentsWithDependencies('report', {
      Sug: {
        address,
        context: {view: 'report', source: ['report', 'Sug']},
        attributes: {component: 'suggest', serverAction: 'data', targetAction: 'TstSug', loading: true},
        model: {selected: ['old'], values: [{value: 'old', label: 'Old', selected: true}]}
      }
    }));
    return created;
  };

  beforeEach(() => {
    window.fetch = jest.fn();
    store = storeWithSuggest();
  });

  it('replaces the values of the suggest keeping their labels and selects all of them', () => {
    store.dispatch(fillSuggestAction(fillSuggest({values})));

    expect(suggest().model.values).toEqual([
      {value: 'aaa', label: 'Label aaa', selected: true},
      {value: 'bbb', label: 'Label bbb', selected: true}
    ]);
    expect(getSelectedValues(suggest())).toEqual(['aaa', 'bbb']);
  });

  it('ends the load of the suggest without requesting the labels to the server', () => {
    store.dispatch(fillSuggestAction(fillSuggest({values})));

    expect(suggest().attributes.loading).toBe(false);
    expect(window.fetch).not.toHaveBeenCalled();
    expect(store.getState().actions.sync.flat()).toEqual([]);
  });

  it('leaves the same suggest as its documented replacement, a fill followed by a select', () => {
    const replaced = storeWithSuggest();

    store.dispatch(fillSuggestAction(fillSuggest({values})));
    replaced.dispatch(fillAction({type: 'fill', address, parameters: {datalist: {rows: values}}}));
    replaced.dispatch(selectAction({type: 'select', address, parameters: {values: ['aaa', 'bbb']}}));

    expect(suggest(replaced).model.values).toEqual(suggest().model.values);
    expect(getSelectedValues(suggest(replaced))).toEqual(['aaa', 'bbb']);
    expect(window.fetch).not.toHaveBeenCalled();
  });

  it('empties the suggest when the action has no values', () => {
    store.dispatch(fillSuggestAction(fillSuggest({})));

    expect(suggest().model.values).toEqual([]);
    expect(getSelectedValues(suggest())).toEqual([]);
  });
});
