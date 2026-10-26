import {messages} from "../../../../src/redux/reducers/messagesReducer";
import {
  addMessage,
  confirmMessage,
  removeConfirm,
  removeMessage,
  showMessages,
  updateMessages
} from "../../../../src/redux/actions/messages";

describe("awe-react-client/test/js/redux/reducers/messagesReducerBranchesTest.js", () => {
  const first = {id: 1, summary: "Title"};
  const second = {id: 2, summary: "Other"};

  it("starts with the base and report messages defined and nothing showing", () => {
    expect(messages(undefined, {})).toEqual({defined: {base: {}, report: {}}, showing: []});
  });

  it("adds a copy of a message to the showing ones", () => {
    const state = messages(undefined, addMessage(first));
    expect(state.showing).toEqual([first]);
    expect(state.showing[0]).not.toBe(first);
  });

  it("marks as shown the messages it is given", () => {
    const state = messages({showing: [first, second]}, showMessages([second]));
    expect(state.showing).toEqual([first, {...second, show: true}]);
  });

  it("removes a message by id", () => {
    expect(messages({showing: [first, second]}, removeMessage({id: 1})).showing).toEqual([second]);
  });

  it("defines the messages of a view, replacing the previous ones", () => {
    const state = messages({defined: {base: {a: 1}, report: {}}, showing: []}, updateMessages("base", {b: 2}));
    expect(state.defined).toEqual({base: {b: 2}, report: {}});
  });

  it("sets and removes the confirm", () => {
    const withConfirm = messages(undefined, confirmMessage(first));
    expect(withConfirm.confirm).toEqual(first);
    expect(messages(withConfirm, removeConfirm()).confirm).toBeUndefined();
  });

  it("returns the same state for unrelated actions and for a popover that is not showing", () => {
    const state = {showing: []};
    expect(messages(state, {type: "OTHER"})).toBe(state);
    expect(messages(state, {type: "REMOVE_POPOVER", data: {id: 1}})).toBe(state);
  });
});
