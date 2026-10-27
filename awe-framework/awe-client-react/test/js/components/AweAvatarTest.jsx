import React from "react";
import { renderWithProviders } from "../test-utils";
import AweAvatar from "../../../src/components/AweAvatar";
import { fireEvent, waitFor } from "@testing-library/react";
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

  it("shows the description of the value as a second line under the label", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const state = {
      ...preloadedState,
      components: {
        avatar: {
          ...preloadedState.components.avatar,
          model: { values: [{ label: "Pat Vega", description: "Sales operations", value: "pvega", selected: true }] },
        },
      },
    };

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState: state });

    const text = document.querySelector(".avatar-component .avatar-text");
    expect(text.querySelector(".avatar-name")).toHaveTextContent(/^Pat Vega$/);
    expect(text.querySelector(".avatar-description")).toHaveTextContent(/^Sales operations$/);
  });

  it("shows only the label when the value has no description", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    expect(document.querySelector(".avatar-component .avatar-name")).toHaveTextContent(/^test$/);
    expect(document.querySelector(".avatar-component .avatar-description")).toBeNull();
  });

  it("shows no text when the label is hidden", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const state = {
      ...preloadedState,
      components: {
        avatar: {
          ...preloadedState.components.avatar,
          model: { values: [{ label: "Pat Vega", description: "Sales operations", value: "pvega", selected: true }] },
          attributes: { ...preloadedState.components.avatar.attributes, showLabel: false },
        },
      },
    };

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState: state });

    expect(document.querySelector(".avatar-component .avatar-text")).toBeNull();
  });

  it("shows the initials of the first and the last word of the label, as the prototype", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const state = {
      ...preloadedState,
      components: {
        avatar: {
          ...preloadedState.components.avatar,
          model: { values: [{ label: "Pat de la Vega", value: "pvega", image: null, selected: true }] },
          attributes: { label: "avatar" },
        },
      },
    };

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState: state });

    expect(document.querySelector("div#avatar").textContent).toBe("PV");
  });

  it("leaves the words that do not start with a letter out of the initials", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const state = {
      ...preloadedState,
      components: {
        avatar: {
          ...preloadedState.components.avatar,
          model: { values: [{ label: "Manager (test)", value: "test", image: null, selected: true }] },
          attributes: { label: "avatar" },
        },
      },
    };

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState: state });

    expect(document.querySelector("div#avatar").textContent).toBe("M");
  });

  it("shows the name of the value, when it has one, keeping the label as its hint, as the prototype", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const state = {
      ...preloadedState,
      components: {
        avatar: {
          ...preloadedState.components.avatar,
          model: { values: [{ label: "Pat Vega (pvega)", name: "Pat Vega", value: "pvega", image: null, selected: true }] },
          attributes: { label: "avatar" },
        },
      },
    };

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState: state });

    expect(document.querySelector(".avatar-component .avatar-name")).toHaveTextContent(/^Pat Vega$/);
    expect(document.querySelector("div#avatar")).toHaveAttribute("title", "Pat Vega (pvega)");
    expect(document.querySelector("div#avatar").textContent).toBe("PV");
  });

  it("shows a chevron after the text when the avatar opens a dropdown", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const option = { elementType: "Tag", type: "span", id: "option", label: "Option" };

    renderWithProviders(<AweAvatar id="avatar" elementList={[option]} />, { preloadedState });

    expect(document.querySelector(".avatar-component .avatar-chevron")).not.toBeNull();
  });

  it("opens its dropdown as the user menu of the prototype", async () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());
    const option = { elementType: "Tag", type: "span", id: "option", label: "Option" };

    renderWithProviders(<AweAvatar id="avatar" elementList={[option]} />, { preloadedState });
    fireEvent.click(document.querySelector("div#avatar"));

    const dropdown = await waitFor(() => {
      const panel = document.querySelector(".info-dropdown");
      expect(panel).not.toBeNull();
      return panel;
    });
    expect(dropdown).toHaveClass("avatar-dropdown");
  });

  it("shows no chevron when the avatar opens nothing", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, { preloadedState });

    expect(document.querySelector(".avatar-component .avatar-chevron")).toBeNull();
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
  it("is not disabled while actions run, as in the AngularJS engine", () => {
    jest.spyOn(React, "lazy").mockImplementation(importFunc => importFunc());

    renderWithProviders(<AweAvatar id="avatar" elementList={[]} />, {
      preloadedState: { ...preloadedState, actions: { running: true, sync: [[]], async: [] } }
    });

    const avatar = document.querySelector("#avatar");
    expect(avatar.hasAttribute("disabled")).toBe(false);
    expect(avatar.className).not.toContain("p-disabled");
  });

});
