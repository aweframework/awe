import React from "react";
import { renderWithProviders } from "../test-utils";
import AweAvatar from "../../../src/components/AweAvatar";
import { fireEvent } from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

// Test
describe("awe-react-client/test/js/criteria/AweAvatarTest.jsx", () => {

  afterEach(() => jest.restoreAllMocks());

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
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    // check
    expect(document.querySelector("div#avatar")).not.toBeNull();
  });

  it("renders Avatar component with empty state", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { components: {avatar: {}}, settings: {} });

    // check
    expect(document.querySelector("div#avatar")).not.toBeNull();
  });

  it("renders the initial of the label and requests no image when the user has no image", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const state = {
      ...preloadedState,
      components: {
        avatar: {
          ...preloadedState.components.avatar,
          model: { values: [{ label: "manager", value: "test", image: null, selected: true }] },
          attributes: { label: "avatar" },
        },
      },
    };

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState: state });

    const avatar = document.querySelector("div#avatar");
    expect(avatar.querySelector("img")).toBeNull();
    expect(avatar.textContent).toBe("M");
  });

  it("renders the image of the user under the context path", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    expect(document.querySelector("div#avatar img").getAttribute("src")).toMatch(/helpImage$/);
  });

  it("renders Avatar component and clicks on it", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    // check
    expect(document.querySelector("div#avatar")).not.toBeNull();

    // Click on avatar
    fireEvent.click(document.querySelector("div#avatar"));
  });
});
