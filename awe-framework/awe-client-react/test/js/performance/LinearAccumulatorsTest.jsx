import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";
import {getFormValues} from "../../../src/redux/selectors/form";
import {updateSelectedGroup} from "../../../src/redux/reducers/components/gridModel";
import {buildComponentsById} from "../../../src/redux/thunks/screen";
import {getMergedComponent} from "../../../src/redux/reducers/components/stateHelpers";

/**
 * The accumulators that join a value per component keep the order of the components and let the last one win when
 * an identifier repeats. They are built in linear time (#962), so these tests pin the result they always had
 */
describe("awe-react-client/test/js/performance/LinearAccumulatorsTest.jsx", () => {
  afterEach(() => ComponentRegistry.clearAll());

  describe("getFormValues", () => {
    const criterion = (id, value, view = "base") => ({
      address: {view, component: id},
      attributes: {component: "text", id},
      model: {values: [{value, label: value, selected: true}]}
    });
    const stateOf = (components, settings = {token: "tok", tokenKey: "tokenKey"}) => ({settings, components});

    it("collects the token first and then the values of the components in order", () => {
      const values = getFormValues(stateOf({c: criterion("c", "3"), a: criterion("a", "1"), b: criterion("b", "2")}));

      expect(Object.keys(values)).toEqual(["tokenKey", "c", "a", "b"]);
      expect(values).toEqual({tokenKey: "tok", c: "3", a: "1", b: "2"});
    });

    it("only collects the components of the requested view", () => {
      const values = getFormValues(stateOf({a: criterion("a", "1"), b: criterion("b", "2", "other")}), false, undefined, null, "other");

      expect(values).toEqual({tokenKey: "tok", b: "2"});
    });

    it("lets the last component win when an identifier repeats, keeping its first position", () => {
      const warn = jest.spyOn(console, "warn").mockImplementation(() => {});
      const values = getFormValues(stateOf({
        first: criterion("dup", "old"), middle: criterion("mid", "m"), last: criterion("dup", "new")
      }));

      expect(Object.keys(values)).toEqual(["tokenKey", "dup", "mid"]);
      expect(values.dup).toBe("new");
      expect(warn).toHaveBeenCalledTimes(1);
      warn.mockRestore();
    });

    it("sends no token when there is none", () => {
      expect(Object.keys(getFormValues(stateOf({a: criterion("a", "1")}, {})))).toEqual(["a"]);
    });
  });

  describe("updateSelectedGroup", () => {
    const radio = (id, group = "g", view = "base") => ({address: {view, component: id}, attributes: {group}, model: {values: [{value: id}]}});

    it("returns one delta per component of the group, in order", () => {
      ["r1", "r2", "r3"].forEach(id => ComponentRegistry.register(id, radio(id)));
      ComponentRegistry.register("other", radio("other", "h"));

      const update = updateSelectedGroup({}, [], "base", "g", ["r2"]);

      expect(Object.keys(update)).toEqual(["r1", "r2", "r3"]);
      expect(["r1", "r2", "r3"].map(id => getMergedComponent(id, update).model.values[0].selected)).toEqual([false, true, false]);
    });
  });

  describe("buildComponentsById", () => {
    const component = (id, extra = {}) => ({id, controller: {component: "text", ...extra}, model: {values: []}});

    it("builds a component state per identifier, in order", () => {
      const built = buildComponentsById("base", [component("b"), component("a")], {}, {a: ["x"]});

      expect(Object.keys(built)).toEqual(["b", "a"]);
      expect(built.a.address).toEqual({view: "base", component: "a"});
      expect(built.a.context.source).toEqual(["x"]);
      expect(built.b.context.source).toEqual([]);
    });

    it("lets the last component win when an identifier repeats, keeping its first position", () => {
      const built = buildComponentsById("base", [component("d", {label: "old"}), component("m"), component("d", {label: "new"})], {}, {});

      expect(Object.keys(built)).toEqual(["d", "m"]);
      expect(built.d.attributes.label).toBe("new");
    });
  });
});
