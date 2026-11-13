import {messages} from '../../../../src/redux/reducers/messagesReducer';
import {removePopover, showPopover} from '../../../../src/redux/actions/messages';

describe('awe-react-client/test/js/redux/reducers/messagesReducerTest.js', () => {
  const first = {id: 1, severity: 'warn', summary: 'Title', detail: 'Text'};
  const second = {id: 2, severity: 'error', summary: 'Other', detail: 'Other text'};

  it('starts without a popover', () => {
    expect(messages(undefined, {}).popover).toBeUndefined();
  });

  it('shows a popover replacing the previous one', () => {
    const state = messages(messages(undefined, showPopover(first)), showPopover(second));
    expect(state.popover).toEqual(second);
  });

  it('removes the popover it is asked to remove', () => {
    const state = messages(messages(undefined, showPopover(first)), removePopover(first));
    expect(state.popover).toBeUndefined();
  });

  it('keeps a newer popover when an older one is removed', () => {
    const state = messages(messages(undefined, showPopover(second)), removePopover(first));
    expect(state.popover).toEqual(second);
  });
});
