import React from "react";
import {fireEvent, render, screen} from "@testing-library/react";
import i18n from "i18next";
import ScreenElementPanel from "../../../src/components/ScreenElementPanel";
import {resolveValue} from "../../../src/utilities/screenPersonalization";

const attributes = [
  {attribute: "visible", valueType: "BOOLEAN", allowedValues: [], value: "true"},
  {attribute: "max", valueType: "INTEGER", allowedValues: [], value: "10"},
  {attribute: "align", valueType: "ENUM", allowedValues: ["left", "right"], value: "left"},
  {attribute: "label", valueType: "TEXT", allowedValues: [], value: "Save"},
  {attribute: "unit", valueType: "TEXT", allowedValues: [], value: ""}
];
const rule = (id, attribute, value, profile = null) => ({id, user: null, profile, component: "Btn", attribute, value, active: 1});

describe("awe-react-client/test/js/components/ScreenPersonalizationTest.jsx", () => {
  const renderPanel = (props = {}) => {
    const handlers = {onChange: jest.fn(), onResetElement: jest.fn(), onClose: jest.fn()};
    render(<ScreenElementPanel element={{id: "Btn", label: "Save", elementType: "Button"}} scopeLabel="Everyone" attributes={attributes}
      rules={[]} scope={{type: "everyone", id: null}} pending={{}} {...handlers} {...props}/>);
    return handlers;
  };

  it("shows the editor of each value type", () => {
    renderPanel();
    expect(screen.getByRole("switch")).toBeInTheDocument();                       // BOOLEAN
    expect(screen.getByDisplayValue("10")).toBeInTheDocument();                   // INTEGER
    expect(screen.getByRole("combobox")).toBeInTheDocument();                     // ENUM
    expect(screen.getByDisplayValue("Save")).toHaveAttribute("maxlength", "60");  // TEXT
  });

  it("reports a change and shows an inline error for an invalid value", () => {
    const {onChange} = renderPanel({pending: {"Btn|max": "abc"}});
    expect(screen.getByRole("alert")).toHaveTextContent("ERROR_MESSAGE_SCREEN_CONFIGURATION_INVALID_INTEGER");
    fireEvent.change(screen.getByDisplayValue("abc"), {target: {value: "12"}});
    expect(onChange).toHaveBeenCalledWith("max", "12");
  });

  it("puts the advanced attributes in a collapsed group", () => {
    renderPanel();
    expect(screen.getByText("SCR_PER_ADVANCED").closest("details")).not.toHaveAttribute("open");
  });

  it("offers Reset only for an attribute that the scope overrides, and resets it", () => {
    const {onChange} = renderPanel({rules: [rule(1, "label", "X")]});
    fireEvent.click(screen.getByRole("button", {name: /SCR_PER_RESET_ATTRIBUTE/}));
    expect(onChange).toHaveBeenCalledWith("label", null);
    expect(screen.getAllByRole("button", {name: /SCR_PER_RESET_ATTRIBUTE/})).toHaveLength(1);
  });

  it("closes and resets the whole element from the footer", () => {
    const {onClose, onResetElement} = renderPanel();
    fireEvent.click(screen.getByText("SCR_PER_RESET_ELEMENT"));
    fireEvent.click(screen.getByText("BUTTON_DONE"));
    expect(onResetElement).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });

  describe("resolveValue", () => {
    const scope = {type: "profile", id: 7};
    it("tells where the value comes from, including the pending edits", () => {
      const rules = [rule(1, "label", "E"), rule(2, "label", "P", 7)];
      expect(resolveValue(rules, {}, "Btn", "label", scope, "xml")).toMatchObject({value: "P", origin: "profile"});
      expect(resolveValue(rules, {"Btn|label": "N"}, "Btn", "label", scope, "xml")).toMatchObject({value: "N", origin: "profile"});
      expect(resolveValue(rules, {"Btn|label": null}, "Btn", "label", scope, "xml")).toMatchObject({value: "E", origin: "everyone"});
      expect(resolveValue([], {}, "Btn", "label", scope, "xml")).toMatchObject({value: "xml", origin: "default"});
    });
  });

  it("shows the translated text of a label key, keeps the key as helper text and reports what the admin types", () => {
    i18n.addResource(i18n.language, "translation", "BUTTON_NEW_USER", "New user");
    const {onChange} = renderPanel({attributes: [{attribute: "label", valueType: "TEXT", allowedValues: [], value: "BUTTON_NEW_USER"}]});
    expect(screen.getByDisplayValue("New user")).toBeInTheDocument();
    expect(screen.getByText("BUTTON_NEW_USER")).toBeInTheDocument();
    fireEvent.change(screen.getByDisplayValue("New user"), {target: {value: "Add user"}});
    expect(onChange).toHaveBeenCalledWith("label", "Add user");
  });
});
