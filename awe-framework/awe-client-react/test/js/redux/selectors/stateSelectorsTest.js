import {selectActionsRunning, selectMessages, selectSettings} from "../../../../src/redux/selectors/stateSelectors";

describe("awe-react-client/test/js/redux/selectors/stateSelectorsTest.js", () => {
  const state = {
    settings: {theme: "dark"},
    messages: {showing: [], defined: {}},
    actions: {running: true, sync: [[]], async: []}
  };

  it("selectSettings returns the settings slice itself", () => {
    expect(selectSettings(state)).toBe(state.settings);
  });

  it("selectMessages returns the messages slice itself", () => {
    expect(selectMessages(state)).toBe(state.messages);
  });

  it("selectActionsRunning returns the running flag", () => {
    expect(selectActionsRunning(state)).toBe(true);
    expect(selectActionsRunning({...state, actions: {...state.actions, running: false}})).toBe(false);
  });
});
