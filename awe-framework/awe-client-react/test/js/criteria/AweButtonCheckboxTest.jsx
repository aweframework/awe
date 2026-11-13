import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweButtonCheckbox from "../../../src/criteria/AweButtonCheckbox";
import {cleanup, fireEvent, screen} from "@testing-library/react";
import i18n from "i18next";

describe('awe-react-client/test/js/criteria/AweButtonCheckboxTest.jsx', () => {

  afterEach(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      checkbox: {
        address: {component: 'checkbox', view: 'report'},
        model: {values: [{label: 'test', value: '1', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "test"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Button Checkbox component', async () => {
    renderWithProviders(<AweButtonCheckbox id="checkbox"/>, {preloadedState});

    // check
    expect(await screen.findByRole("button")).not.toBeNull();
  });

  describe("with options whose labels are locale keys", () => {
    const stateWith = (values) => ({
      settings: DEFAULT_SETTINGS,
      components: {
        days: {
          address: {component: 'days', view: 'report'},
          model: {values},
          attributes: {readonly: false},
          validationRules: {required: false},
          specificAttributes: {sort: []}
        }
      }
    });
    const options = [{label: 'WEEKDAY_MONDAY', value: '1', selected: true}, {label: 'WEEKDAY_TUESDAY', value: '2'}];
    const button = (value) => document.querySelector(`[role="button"][option-id="${value}"]`);
    const selectedOf = (store) => store.getState().components.days.model.values.filter(item => item.selected).map(item => item.value);

    beforeAll(() => {
      i18n.addResources("en-GB", "translation", {WEEKDAY_MONDAY: "Monday", WEEKDAY_TUESDAY: "Tuesday"});
    });

    it('names each button with its translated label, as the text it shows', async () => {
      renderWithProviders(<AweButtonCheckbox id="days"/>, {preloadedState: stateWith(options)});

      const tuesday = await screen.findByRole("button", {name: "Tuesday"});
      expect(tuesday).toHaveTextContent("Tuesday");
      expect(screen.getByRole("button", {name: "Monday"})).toHaveTextContent("Monday");
      expect(screen.queryByRole("button", {name: "WEEKDAY_TUESDAY"})).toBeNull();
    });

    it('keeps the value of each button and adds the clicked one to the selection', async () => {
      const {store} = renderWithProviders(<AweButtonCheckbox id="days"/>, {preloadedState: stateWith(options)});

      expect(await screen.findAllByRole("button")).toHaveLength(2);

      fireEvent.click(button("2"));

      expect(selectedOf(store)).toEqual(["1", "2"]);
      expect(button("2")).toHaveAttribute("aria-pressed", "true");
      expect(button("1")).toHaveAttribute("aria-pressed", "true");
    });
  });

});
