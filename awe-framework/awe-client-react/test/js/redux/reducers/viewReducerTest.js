import {view} from "../../../../src/redux/reducers/viewReducer";
import {CLEAR_ALL_VIEWS, CLEAR_VIEW, SET_VIEW, UPDATE_VIEW} from "../../../../src/redux/actions/view";

describe("awe-react-client/test/js/redux/reducers/viewReducerTest.js", () => {
  it("starts with both views loading", () => {
    expect(view(undefined, {})).toEqual({base: {loading: true}, report: {loading: true}});
  });

  it("sets a view and makes it the current one", () => {
    expect(view({}, {type: SET_VIEW, view: "report", data: {screen: "home"}}))
      .toEqual({view: "report", report: {screen: "home"}});
  });

  it("merges data into an existing view", () => {
    expect(view({base: {screen: "home", loading: true}}, {type: UPDATE_VIEW, view: "base", data: {loading: false}}))
      .toEqual({base: {screen: "home", loading: false}});
  });

  it("resets one view to loading", () => {
    expect(view({base: {screen: "home"}, report: {screen: "list"}}, {type: CLEAR_VIEW, view: "report"}))
      .toEqual({base: {screen: "home"}, report: {loading: true}});
  });

  it("resets every view to loading", () => {
    expect(view({view: "base", base: {screen: "home"}, report: {screen: "list"}}, {type: CLEAR_ALL_VIEWS}))
      .toEqual({view: "base", base: {loading: true}, report: {loading: true}});
  });

  it("returns the same state for unrelated actions", () => {
    const state = {base: {screen: "home"}};
    expect(view(state, {type: "OTHER"})).toBe(state);
  });
});
