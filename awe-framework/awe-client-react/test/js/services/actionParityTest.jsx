import React from 'react';
import {MemoryRouter} from "react-router";
import {renderWithProviders} from "../test-utils";
import {ClientActions} from "../../../../awe-client-angular/src/main/resources/js/awe/data/actions";
import useClientActions from "../../../src/services/ClientActionsService";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

const NOT_IN_REACT = "Not carried over on purpose (odd/ROADMAP.md, user decision)";

/**
 * AngularJS client actions that React registers under a different name (AngularJS name -> React name)
 */
const ALIASES = {
  "validate-selected-row": "validate-row"
};

/**
 * AngularJS client actions that React does not register (AngularJS name -> reason)
 */
const NOT_PORTED = {
  "taglist-data": NOT_IN_REACT,
  "file-status": NOT_IN_REACT,
  "redirect-screen": NOT_IN_REACT,
  "update-theme": NOT_IN_REACT,
  "enable-dependencies": NOT_IN_REACT,
  "disable-dependencies": NOT_IN_REACT,
  "end-dependency": NOT_IN_REACT,
  "copy-criterion-value-clipboard": NOT_IN_REACT,
  "screen-data": "Not a client action in React: the screen data is requested by the screen thunks",
  "target-message": "Open question: React registers it as targetMessage, so the server name is never dispatched",
  "fill-suggest": "Planned in #804: React will support it as fill with the selected rows"
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

/**
 * Status of every AngularJS action in React
 */
const classify = (angularNames, reactNames) => angularNames.map((name) => {
  if (name in NOT_PORTED) {
    return {name, status: "not-ported", reason: NOT_PORTED[name]};
  }
  if (name in ALIASES && reactNames.has(ALIASES[name])) {
    return {name, status: "renamed", reactName: ALIASES[name]};
  }
  return {name, status: reactNames.has(name) ? "implemented" : "undecided"};
});

describe('awe-react-client/test/js/services/actionParityTest.jsx', () => {
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

  it('knows the status in React of every AngularJS client action', () => {
    const angularNames = [...new Set(collectNames(ClientActions))];

    const undecided = classify(angularNames, reactNames).filter(({status}) => status === "undecided");

    expect(undecided.map(({name}) => name)).toEqual([]);
  });

  it('fails when a new AngularJS action has no React status', () => {
    const result = classify(["a-brand-new-action"], reactNames);

    expect(result).toEqual([{name: "a-brand-new-action", status: "undecided"}]);
  });

  it('does not list as not ported an action that React already registers', () => {
    const stale = Object.keys(NOT_PORTED).filter((name) => reactNames.has(name));

    expect(stale).toEqual([]);
  });

  it('only lists aliases and not ported actions that still exist in AngularJS', () => {
    const angularNames = new Set(collectNames(ClientActions));

    const unknown = [...Object.keys(ALIASES), ...Object.keys(NOT_PORTED)].filter((name) => !angularNames.has(name));

    expect(unknown).toEqual([]);
  });

  it('implements print and resize, the contract actions of #817', () => {
    expect(reactNames.has("print")).toBe(true);
    expect(reactNames.has("resize")).toBe(true);
    expect(reactNames.has("screen-print")).toBe(false);
    expect(classify(["print"], reactNames)).toEqual([{name: "print", status: "implemented"}]);
  });

  it('implements the after-* grid notifications of #802', () => {
    ["after-add-row", "after-delete-row", "after-save-row", "after-cancel-row"].forEach((name) => {
      expect(classify([name], reactNames)).toEqual([{name, status: "implemented"}]);
    });
  });

  it('points every alias to an action that React registers', () => {
    const missing = Object.entries(ALIASES).filter(([, reactName]) => !reactNames.has(reactName));

    expect(missing).toEqual([]);
  });
});
