import {getModelValidation} from "../../../../src/redux/selectors/modelValidation";

describe("awe-react-client/test/js/redux/selectors/modelValidationSelectorsTest.js", () => {
  const criterion = (id, value, stored, attributes = {}) => ({
    address: {view: "base", component: id},
    attributes: {id, component: "text", ...attributes},
    model: {values: [{value, label: value, selected: true}]},
    storedModel: {values: [{value: stored, label: stored, selected: true}]}
  });
  const stateWith = (...components) => ({
    settings: {},
    components: Object.fromEntries(components.map(component => [component.address.component, component]))
  });

  it("has an empty, unchanged model when there are no components", () => {
    expect(getModelValidation(stateWith())).toEqual({isEmptyModel: true, isUpdatedModel: false, isUnchangedModel: true});
  });

  it("detects an updated model when a value differs from the stored one", () => {
    expect(getModelValidation(stateWith(criterion("a", "new", "old")))).toEqual({
      isEmptyModel: true, isUpdatedModel: true, isUnchangedModel: false
    });
  });

  it("keeps the model unchanged when the values equal the stored ones", () => {
    expect(getModelValidation(stateWith(criterion("a", "same", "same")))).toEqual({
      isEmptyModel: true, isUpdatedModel: false, isUnchangedModel: true
    });
  });

  it("only components that check emptiness make the model not empty", () => {
    expect(getModelValidation(stateWith(criterion("a", "x", "x", {checkEmpty: true}))).isEmptyModel).toBe(false);
    expect(getModelValidation(stateWith(criterion("a", "x", "x"))).isEmptyModel).toBe(true);
    expect(getModelValidation(stateWith(criterion("a", null, null, {checkEmpty: true}))).isEmptyModel).toBe(true);
  });

  it("the model is not empty when any component that checks it has a value", () => {
    const state = stateWith(criterion("a", null, null, {checkEmpty: true}), criterion("b", "y", "y", {checkEmpty: true}));
    expect(getModelValidation(state).isEmptyModel).toBe(false);
  });

  it("the model is updated when any component changed", () => {
    const state = stateWith(criterion("a", "x", "x"), criterion("b", "new", "old"));
    expect(getModelValidation(state).isUpdatedModel).toBe(true);
  });

  it("ignores the components of a grid row", () => {
    const row = {...criterion("cell", "new", "old"), address: {view: "base", component: "Grd", row: "1", column: "cell"}};
    const state = {settings: {}, components: {cell: row}};
    expect(getModelValidation(state)).toEqual({isEmptyModel: true, isUpdatedModel: false, isUnchangedModel: true});
  });
});
