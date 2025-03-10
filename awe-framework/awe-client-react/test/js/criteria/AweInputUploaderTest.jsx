import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {cleanup, screen} from "@testing-library/react";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputUploader from "../../../src/criteria/AweInputUploader";

describe('awe-react-client/test/js/criteria/AweInputUploaderTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      uploader: {
        address: {component: 'uploader', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          size: "sm"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Input Uploader component', () => {
    const resultPromise = new Promise((resolve) => resolve({
      structure: [], components: [{
        uploader: {
          address: {component: 'uploader', view: 'report'},
          model: {values: [{label: 'test', value: 'test', selected: true}]},
          attributes: {
            placeholder: "placeholder",
            readonly: false,
          },
          validationRules: {
            required: false
          },
          specificAttributes: {sort: []}
        }
      }]
    }));
    const promise = new Promise((resolve) => {
      resolve({
        headers: {
          get: () => 'application/json;charset=UTF-8'
        },
        status: 200,
        ok: true,
        json: () => resultPromise
      });
    });
    spyOn(window, "fetch").and.returnValue(promise);
    promise.catch((e) => console.info(e));

    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState});

    // check
    expect(screen.getByText("BUTTON_CHOOSE")).toBeDefined();
  });

  it('renders Awe Input Uploader component with size', () => {
    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState});

    // check
    expect(screen.getByText("BUTTON_CHOOSE")).toBeDefined();
  });

  it('renders Awe Input Uploader component and deletes a file', () => {
    renderWithProviders(<AweInputUploader id="uploader"/>, {preloadedState});

    // check
    expect(screen.getByText("BUTTON_CLEAR")).toBeDefined();

    // click on delete file
    screen.getByText("BUTTON_CLEAR").click();

    // check button clear
    expect(screen.getByText("BUTTON_CLEAR").parentElement).toHaveClass("hidden");
  });

});
