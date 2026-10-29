import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import AweSuggest from "../../../src/criteria/AweSuggest";
import AweSuggestMultiple from "../../../src/criteria/AweSuggestMultiple";

// The props the suggests give to the PrimeReact autocomplete
const autocompleteProps = [];
jest.mock("primereact/autocomplete", () => {
  const ReactModule = jest.requireActual("react");
  return {
    AutoComplete: ReactModule.forwardRef((props, ref) => {
      autocompleteProps.push(props);
      return <input ref={ref} id={props.id} readOnly/>;
    })
  };
});

describe("awe-react-client/test/js/criteria/AweSuggestHighlightTest.jsx", () => {
  const state = id => ({
    settings: DEFAULT_SETTINGS,
    components: {
      [id]: {
        address: {component: id, view: "report"},
        model: {values: []},
        attributes: {},
        validationRules: {},
        specificAttributes: {}
      }
    }
  });

  beforeEach(() => {
    autocompleteProps.length = 0;
  });

  // The first suggestion is highlighted, so Enter or Tab choose it at once, as in the AngularJS engine (#793)
  it.each([["suggest", AweSuggest], ["suggestMultiple", AweSuggestMultiple]])(
    "highlights the first suggestion of a %s", async (id, Component) => {
      await act(async () => {
        renderWithProviders(<Component id={id}/>, {preloadedState: state(id)});
      });

      expect(autocompleteProps.at(-1)).toMatchObject({autoHighlight: true, forceSelection: true});
    });
});
