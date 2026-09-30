import {size} from "../../../../src/redux/reducers/sizeReducer";
import {UPDATE_SIZE} from "../../../../src/redux/actions/size";

describe("awe-react-client/test/js/redux/reducers/sizeReducerTest.js", () => {
  it("starts empty", () => {
    expect(size(undefined, {})).toEqual({});
  });

  it("merges the new size into the current one", () => {
    expect(size({width: 100, height: 50}, {type: UPDATE_SIZE, data: {width: 200}})).toEqual({width: 200, height: 50});
  });

  it("returns the same state for unrelated actions", () => {
    const state = {width: 100};
    expect(size(state, {type: "OTHER"})).toBe(state);
  });
});
