import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputText from "../../../src/criteria/AweInputText";
import {cleanup} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweCriterionLeftLabelTest.jsx', () => {

  afterEach(cleanup);

  const render = (attributes = {}) => renderWithProviders(<AweInputText id="text"/>, {
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      components: {
        text: {
          address: {component: 'text', view: 'report'},
          model: {values: []},
          attributes: {label: "Label", readonly: false, visible: true, ...attributes},
          validationRules: {}
        }
      }
    }
  });

  const getField = () => document.querySelector('[criterion-id="text"]');
  const getLabel = () => getField().querySelector('label');

  it('puts the label on top by default', () => {
    render();
    expect(getField().classList.contains('label-left')).toBe(false);
    expect(getLabel().style.width).toBe("");
  });

  it('puts the label on the left with the width of the left-label in characters', () => {
    render({leftLabel: 18});
    expect(getField().classList.contains('label-left')).toBe(true);
    // 18 characters x 7 pixels per character (small size)
    expect(getLabel().style.width).toBe("126px");
  });

  it('accepts the left-label as a text', () => {
    render({leftLabel: "10"});
    expect(getLabel().style.width).toBe("70px");
  });

  it('adds the pixels of the size of the criterion to every character', () => {
    render({leftLabel: 10, size: "md"});
    expect(getLabel().style.width).toBe("80px");
    cleanup();
    render({leftLabel: 10, size: "lg"});
    expect(getLabel().style.width).toBe("100px");
  });

  it('ignores a left-label that is not a number', () => {
    render({leftLabel: "wide"});
    expect(getField().classList.contains('label-left')).toBe(false);
  });
});
