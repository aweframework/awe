import React from "react";
import { act } from "@testing-library/react";
import { renderWithProviders } from "../test-utils";
import AweSidePanel from "../../../src/components/AweSidePanel";
import AweDialog from "../../../src/components/AweDialog";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { updateAttributes } from "../../../src/redux/actions/components";
import { closeDialogAction, openDialogAction } from "../../../src/redux/thunks/components";

describe("awe-react-client/test/js/criteria/CriterionViewModeTest.jsx", () => {
  const view = "report";
  const panelAddress = { component: "panel", view };
  const criterionAddress = { component: "name", view };
  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      panel: {
        address: panelAddress,
        model: { values: [] },
        attributes: { isShowing: false, component: "dialog" },
        specificAttributes: {},
        actions: []
      },
      name: {
        address: criterionAddress,
        model: { values: [] },
        attributes: { component: "text", visible: true, readonly: false },
        validationRules: {},
        specificAttributes: {},
        actions: []
      }
    }
  };
  const elementList = [{ elementType: "Criteria", component: "text", id: "name" }];
  const renderIn = (Panel) => renderWithProviders(<Panel id="panel" elementList={elementList}/>, { preloadedState });
  const openAction = (viewMode) => ({
    type: "dialog", target: "panel", address: { component: "ButEdit", view }, ...(viewMode ? { viewMode } : {})
  });
  const open = (store, viewMode) => act(() => {
    store.dispatch(openDialogAction(openAction(viewMode)));
  });
  const close = (store) => act(() => {
    store.dispatch(closeDialogAction({ type: "close", address: panelAddress }));
  });
  const setReadonly = (store, readonly) => act(() => {
    store.dispatch(updateAttributes(criterionAddress, { readonly }));
  });
  const field = () => document.querySelector("[criterion-id='name']");
  const input = () => document.getElementById("name");
  const isReadOnly = () => field().classList.contains("p-disabled") && input().disabled;
  const isEditable = () => !field().classList.contains("p-disabled") && !input().disabled;

  it("makes the criteria of a side panel opened in view mode read-only", () => {
    const { store } = renderIn(AweSidePanel);

    open(store, true);

    expect(isReadOnly()).toBe(true);
  });

  it("makes the criteria of a dialog opened in view mode read-only", () => {
    const { store } = renderIn(AweDialog);

    open(store, true);

    expect(isReadOnly()).toBe(true);
  });

  it("keeps the criteria read-only when a dependency sets them editable while the panel is in view mode", () => {
    const { store } = renderIn(AweSidePanel);
    open(store, true);

    setReadonly(store, false);

    expect(isReadOnly()).toBe(true);
  });

  it("opens the panel editable again when an action without view mode opens it after a view mode opening", () => {
    const { store } = renderIn(AweSidePanel);
    open(store, true);
    close(store);

    open(store, false);

    expect(isEditable()).toBe(true);
  });

  it("clears the view mode when the panel is closed", () => {
    const { store } = renderIn(AweSidePanel);
    open(store, true);

    close(store);

    expect(store.getState().components.panel.attributes.action).toBeNull();
    expect(isEditable()).toBe(true);
  });

  it("keeps the readonly of the dependencies in a panel opened without view mode", () => {
    const { store } = renderIn(AweSidePanel);
    open(store, false);
    expect(isEditable()).toBe(true);

    setReadonly(store, true);
    expect(isReadOnly()).toBe(true);

    setReadonly(store, false);
    expect(isEditable()).toBe(true);
  });
});
