import React from 'react';
import fs from "fs";
import path from "path";
import {MemoryRouter} from "react-router";
import {renderWithProviders} from "../test-utils";
import {ClientActions} from "../../../../awe-client-angular/src/main/resources/js/awe/data/actions";
import useClientActions from "../../../src/services/ClientActionsService";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

const GENERAL_XSD = path.join(__dirname, "../../../../awe-generic-screens/src/main/resources/schemas/awe/general.xsd");
const XSD_NS = "http://www.w3.org/2001/XMLSchema";
const DEPRECATION = "Deprecated in AWE 5.0";
const NOT_IN_REACT = "Not carried over on purpose (odd/ROADMAP.md, user decision; see actionParityTest)";

/**
 * Contract button actions that an engine does not register by name, and why (engine -> action -> reason)
 */
const NOT_REGISTERED = {
  angular: {
    "resize": "Handled by a listener of /action/resize in controllers/application.js",
    "reload-language": "Handled by a listener of /action/reload-language in services/locals.js"
  },
  react: {
    "copy-criterion-value-clipboard": NOT_IN_REACT,
    "redirect-screen": NOT_IN_REACT,
    "update-theme": NOT_IN_REACT,
    "disable-dependencies": NOT_IN_REACT,
    "enable-dependencies": NOT_IN_REACT
  }
};

/**
 * Values of an enumeration of general.xsd, split into current and deprecated ones
 */
const contractValues = (typeName) => {
  const schema = new DOMParser().parseFromString(fs.readFileSync(GENERAL_XSD, "utf8"), "application/xml");
  const type = [...schema.getElementsByTagNameNS(XSD_NS, "simpleType")].find((node) => node.getAttribute("name") === typeName);
  const values = [...type.getElementsByTagNameNS(XSD_NS, "enumeration")];
  const isDeprecated = (node) => node.textContent.includes(DEPRECATION);
  return {
    current: values.filter((node) => !isDeprecated(node)).map((node) => node.getAttribute("value")),
    deprecated: values.filter(isDeprecated).map((node) => node.getAttribute("value"))
  };
};

/**
 * Flatten the AngularJS action groups into the list of action names
 */
const collectNames = (group) => Object.entries(group).flatMap(([name, value]) => {
  if (value && typeof value.method === "string") {
    return [name];
  }
  return value && typeof value === "object" && !Array.isArray(value) ? collectNames(value) : [];
});

const unhandled = (values, registered, exceptions) => values.filter((name) => !registered.has(name) && !(name in exceptions));

describe('awe-react-client/test/js/services/actionContractTest.jsx', () => {
  const buttonActions = contractValues("buttonActionEnum");
  const angularNames = new Set(collectNames(ClientActions));
  let reactNames;
  let registry;

  function TestHarness() {
    registry = useClientActions();
    return null;
  }

  beforeAll(() => {
    renderWithProviders(<MemoryRouter><TestHarness/></MemoryRouter>, {preloadedState: {settings: DEFAULT_SETTINGS}});
    reactNames = new Set(Object.keys(registry));
  });

  it('reads the button actions of the contract', () => {
    expect(buttonActions.current.length).toBeGreaterThan(50);
    expect(buttonActions.deprecated).toEqual(["forward", "check-leaf-selected", "check-parent-expanded", "get-locals"]);
  });

  it('handles every current button action of the contract in AngularJS', () => {
    expect(unhandled(buttonActions.current, angularNames, NOT_REGISTERED.angular)).toEqual([]);
  });

  it('handles every current button action of the contract in React', () => {
    expect(unhandled(buttonActions.current, reactNames, NOT_REGISTERED.react)).toEqual([]);
  });

  it('handles no deprecated button action in either engine', () => {
    expect(buttonActions.deprecated.filter((name) => angularNames.has(name) || reactNames.has(name))).toEqual([]);
  });

  it('only lists exceptions that are current contract values not registered by the engine', () => {
    const stale = Object.entries({angular: angularNames, react: reactNames}).flatMap(([engine, names]) =>
      Object.keys(NOT_REGISTERED[engine])
        .filter((name) => names.has(name) || !buttonActions.current.includes(name))
        .map((name) => `${engine}:${name}`));

    expect(stale).toEqual([]);
  });

  it('dispatches the server message actions by their contract name', () => {
    expect(reactNames.has("target-message")).toBe(true);
    expect(reactNames.has("targetMessage")).toBe(false);
    expect(registry["target-message"]).toBe(registry.message);
  });

  it('registers toggle-class in both engines', () => {
    expect(angularNames.has("toggle-class")).toBe(true);
    expect(reactNames.has("toggle-class")).toBe(true);
  });
});
