import React from "react";
import { renderWithProviders } from "../test-utils";
import AweAvatar from "../../../src/components/AweAvatar";
import { fireEvent } from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

// Test
describe("awe-react-client/test/js/criteria/AweAvatarTest.jsx", () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      avatar: {
        address: { component: "avatar", view: "report" },
        model: { values: [{ label: "test", value: "test", selected: true }] },
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "avatar",
          title: "avatar",
          image: "helpImage",
        },
        validationRules: {
          required: false,
        },
        specificAttributes: { sort: [] },
      },
    },
  };

  it("renders Avatar component", () => {
    spyOn(React, "lazy").and.callFake(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    // check
    expect(document.querySelector("div#avatar")).not.toBeNull();
  });

  it("renders Avatar component and clicks on it", () => {
    spyOn(React, "lazy").and.callFake(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    // check
    expect(document.querySelector("div#avatar")).not.toBeNull();

    // Click on avatar
    fireEvent.click(document.querySelector("div#avatar"));
  });
});
