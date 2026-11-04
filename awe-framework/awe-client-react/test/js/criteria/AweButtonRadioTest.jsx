import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweButtonRadio from "../../../src/criteria/AweButtonRadio";
import {cleanup, fireEvent, screen} from "@testing-library/react";
import i18n from "i18next";

describe('awe-react-client/test/js/criteria/AweButtonRadioTest.jsx', () => {

  afterEach(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      radio: {
        address: {component: 'radio', view: 'report'},
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

  it('renders Awe Button Radio component', async () => {
    renderWithProviders(<AweButtonRadio id="radio"/>, {preloadedState});

    // check
    expect(await screen.findByRole("button")).not.toBeNull();
  });

  describe("with options whose labels are locale keys", () => {
    const stateWith = (values) => ({
      settings: DEFAULT_SETTINGS,
      components: {
        subject: {
          address: {component: 'subject', view: 'report'},
          model: {values},
          attributes: {readonly: false},
          validationRules: {required: false},
          specificAttributes: {sort: []}
        }
      }
    });
    const options = [{label: 'PARAMETER_PROFILE', value: 'profile', selected: true}, {label: 'PARAMETER_USER', value: 'user'}];
    const button = (value) => document.querySelector(`[role="button"][option-id="${value}"]`);
    const selectedOf = (store) => store.getState().components.subject.model.values.filter(item => item.selected).map(item => item.value);

    beforeAll(() => {
      i18n.addResources("en-GB", "translation", {PARAMETER_PROFILE: "Profile", PARAMETER_USER: "User"});
    });

    it('names each button with its translated label, as the text it shows', async () => {
      renderWithProviders(<AweButtonRadio id="subject"/>, {preloadedState: stateWith(options)});

      const user = await screen.findByRole("button", {name: "User"});
      expect(user).toHaveTextContent("User");
      expect(screen.getByRole("button", {name: "Profile"})).toHaveTextContent("Profile");
      expect(screen.queryByRole("button", {name: "PARAMETER_USER"})).toBeNull();
    });

    it('keeps the value of each button and selects one at a time', async () => {
      const {store} = renderWithProviders(<AweButtonRadio id="subject"/>, {preloadedState: stateWith(options)});

      expect(await screen.findAllByRole("button")).toHaveLength(2);
      expect(button("profile")).toHaveAttribute("aria-pressed", "true");

      fireEvent.click(button("user"));

      expect(selectedOf(store)).toEqual(["user"]);
      expect(button("user")).toHaveAttribute("aria-pressed", "true");
      expect(button("profile")).toHaveAttribute("aria-pressed", "false");
    });
  });

  describe("label above the buttons", () => {
    const stateWith = (values, attributes) => ({
      settings: DEFAULT_SETTINGS,
      components: {
        subject: {
          address: {component: 'subject', view: 'report'},
          model: {values},
          attributes: {readonly: false, label: "PARAMETER_SUBJECT", ...attributes},
          validationRules: {required: false},
          specificAttributes: {sort: []}
        }
      }
    });
    const labelRow = () => document.querySelector('[criterion-id="subject"] > label');

    beforeAll(() => {
      i18n.addResources("en-GB", "translation", {PARAMETER_SUBJECT: "Subject", PARAMETER_RADIO_1: "Radio 1"});
    });

    it('shows the translated label of a criterion with options above its buttons, as any other criterion', async () => {
      renderWithProviders(<AweButtonRadio id="subject"/>, {preloadedState: stateWith(
        [{label: 'PARAMETER_PROFILE', value: 'profile', selected: true}, {label: 'PARAMETER_USER', value: 'user'}],
        {initialLoad: "enum"})});

      await screen.findAllByRole("button");
      expect(labelRow()).toHaveTextContent("Subject");
    });

    it('shows the label of a criterion whose options have not arrived yet', async () => {
      renderWithProviders(<AweButtonRadio id="subject"/>, {preloadedState: stateWith([], {initialLoad: "query"})});

      expect(labelRow()).toHaveTextContent("Subject");
    });

    it('keeps a criterion without options as one button with its label as text, without a label above', async () => {
      renderWithProviders(<AweButtonRadio id="subject"/>, {preloadedState: stateWith(
        [{label: 'PARAMETER_RADIO_1', value: '1', selected: true}], {label: "PARAMETER_RADIO_1"})});

      expect(await screen.findByRole("button", {name: "Radio 1"})).toBeInTheDocument();
      expect(labelRow()).toBeNull();
    });
  });

});
