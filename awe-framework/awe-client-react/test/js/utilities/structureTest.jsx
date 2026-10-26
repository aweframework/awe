import React from "react";
import {render} from "@testing-library/react";
import {Components, Criteria, getSource, getSourceChildren, getSourceStyle} from "../../../src/utilities/structure";
import {ComponentList} from "../../../src/components/AweComponents";
import {CriteriaList} from "../../../src/criteria/AweCriteria";

describe("awe-react-client/test/js/utilities/structureTest.jsx", () => {
  describe("getSource", () => {
    it("should find the element of a source", () => {
      const element = {source: "center", elementList: [{id: "a"}]};
      expect(getSource("center", [{source: "left"}, element])).toBe(element);
    });

    it("should give an empty element list when the source is not found", () => {
      expect(getSource("center", [{source: "left"}])).toEqual({elementList: []});
    });
  });

  describe("getSourceStyle", () => {
    it("should give the style of a source that has no type", () => {
      expect(getSourceStyle({style: "p-3"})).toBe("p-3");
    });

    it("should give no style when the source has a type, a style is missing or there is no source", () => {
      expect(getSourceStyle({type: "section", style: "p-3"})).toBe("");
      expect(getSourceStyle({})).toBe("");
      expect(getSourceStyle(null)).toBe("");
      expect(getSourceStyle(undefined)).toBe("");
    });
  });

  describe("Components", () => {
    it("should give nothing for the elements that are ignored", () => {
      expect(Components({elementType: "ButtonAction"}, 0)).toBeNull();
      expect(Components({elementType: "Dependency"}, 1)).toBeNull();
    });

    it("should create the component of an element type with the key of the element", () => {
      const element = Components({elementType: "Button", id: "btn", label: "Save"}, 3);
      expect(element.type).toBe(ComponentList.Button);
      expect(element.key).toBe("btn");
      expect(element.props).toEqual({elementType: "Button", id: "btn", label: "Save"});
    });

    it("should key the component with its index when the element has no identifier", () => {
      expect(Components({elementType: "Button"}, 3).key).toBe("component-3");
    });

    it("should give nothing for an element type that is not a component", () => {
      expect(Components({elementType: "Unknown"}, 0)).toBeUndefined();
    });

    it("should create a criterion for the elements of the criteria type", () => {
      const element = Components({elementType: "Criteria", component: "text", id: "cri"}, 0);
      expect(element.type).toBe(CriteriaList.text);
      expect(element.key).toBe("cri");
    });
  });

  describe("Criteria", () => {
    it("should create the criterion of a component, keyed by index when it has no identifier", () => {
      const element = Criteria({component: "numeric"}, 2);
      expect(element.type).toBe(CriteriaList.numeric);
      expect(element.key).toBe("criterion-2");
      expect(element.props).toEqual({component: "numeric"});
    });

    it("should explain that a criterion has not been created yet", () => {
      const {container} = render(<div>{Criteria({component: "missing"}, 5)}</div>);
      expect(container.textContent).toBe("The criterion missing has not been created yet.");
    });
  });

  describe("getSourceChildren", () => {
    it("should give nothing when there is no source", () => {
      expect(getSourceChildren(null)).toBeNull();
    });

    it("should give an empty list for the default source and for a source without elements", () => {
      expect(getSourceChildren()).toEqual([]);
      expect(getSourceChildren({})).toEqual([]);
    });

    it("should give the children straight when the source has no type", () => {
      const children = getSourceChildren({elementList: [{elementType: "Button", id: "a"}, {elementType: "Dependency"}]});
      expect(children).toHaveLength(2);
      expect(children[0].type).toBe(ComponentList.Button);
      expect(children[1]).toBeNull();
    });

    it("should wrap the children in the element of a source with a type", () => {
      const wrapper = getSourceChildren({type: "section", id: "s", expand: "vertical", style: "p-3", elementList: [{elementType: "Dependency"}]});
      expect(wrapper.type).toBe("section");
      expect(wrapper.props.id).toBe("s");
      expect(wrapper.props.className).toBe("expandible-vertical p-3");
      expect(wrapper.props.children).toEqual([null]);
    });

    it("should leave the class of the wrapper out when it has neither expand nor style", () => {
      const wrapper = getSourceChildren({type: "div", elementList: []});
      expect(wrapper.props.className).toBeUndefined();
    });
  });
});
