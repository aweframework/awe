import React, { useRef } from "react";
import { fireEvent, render } from "@testing-library/react";
import { useFocusTrap } from "../../../src/hooks/useFocusTrap";

function Panel({ active, children }) {
  const ref = useRef(null);
  useFocusTrap(() => ref.current, active);
  return <div ref={ref} data-testid="panel">{children}</div>;
}

describe("awe-react-client/test/js/hooks/useFocusTrapTest.jsx", () => {
  let opener;

  const renderPanel = (active, children = <><button>first</button><input aria-label="middle"/><button>last</button></>) =>
    render(<Panel active={active}>{children}</Panel>);
  const byText = (text) => [...document.querySelectorAll("button")].find(button => button.textContent === text);
  const tab = (shiftKey = false) => fireEvent.keyDown(document.activeElement, { key: "Tab", code: "Tab", shiftKey });

  beforeEach(() => {
    opener = document.createElement("button");
    opener.textContent = "opener";
    document.body.appendChild(opener);
    opener.focus();
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("moves Tab from the last control to the first one", () => {
    renderPanel(true);
    byText("last").focus();

    tab();

    expect(document.activeElement).toBe(byText("first"));
  });

  it("moves Shift+Tab from the first control to the last one", () => {
    renderPanel(true);
    byText("first").focus();

    tab(true);

    expect(document.activeElement).toBe(byText("last"));
  });

  it("lets Tab move between the controls in the middle", () => {
    renderPanel(true);
    byText("first").focus();

    const event = tab();

    // The browser moves the focus: the trap does not cancel the event
    expect(event).toBe(true);
  });

  it.each([
    ["a dialog", "<div class='modal'><button>dialog first</button><button>dialog last</button></div>"],
    ["an aria modal", "<div role='dialog' aria-modal='true'><button>dialog first</button><button>dialog last</button></div>"]
  ])("leaves Tab alone inside %s shown over the panel", (name, html) => {
    renderPanel(true);
    const dialog = document.createElement("div");
    dialog.innerHTML = html;
    document.body.appendChild(dialog);
    byText("dialog last").focus();

    // The browser moves the focus: the trap does not cancel the event
    expect(tab()).toBe(true);
    expect(document.activeElement).toBe(byText("dialog last"));
  });

  it("brings the focus back inside when Tab is pressed outside", () => {
    renderPanel(true);
    opener.focus();

    tab();

    expect(document.activeElement).toBe(byText("first"));
  });

  it("keeps the focus on the panel when it has no focusable control", () => {
    renderPanel(true, <span>text</span>);
    opener.focus();

    tab();

    expect(document.activeElement).toBe(document.querySelector("[data-testid='panel']"));
  });

  it("ignores disabled and hidden controls", () => {
    renderPanel(true, <><button>first</button><button>last</button><button disabled>off</button>
      <button tabIndex={-1}>skipped</button></>);
    byText("last").focus();

    tab();

    expect(document.activeElement).toBe(byText("first"));
  });

  it("does nothing while it is not active", () => {
    renderPanel(false);
    byText("last").focus();

    const event = tab();

    expect(event).toBe(true);
    expect(document.activeElement).toBe(byText("last"));
  });

  it("gives the focus back to the control focused when it was activated", () => {
    const { rerender } = renderPanel(true);
    byText("first").focus();

    rerender(<Panel active={false}><button>first</button></Panel>);

    expect(document.activeElement).toBe(opener);
  });

  it("keeps the focus inside when the panel renders again while it is active", () => {
    const { rerender } = renderPanel(true);
    byText("first").focus();

    rerender(<Panel active={true}><button>first</button><button>last</button></Panel>);

    expect(document.activeElement).toBe(byText("first"));
  });

  it("gives the focus back to the control focused last when the focus was lost before it was activated", () => {
    // The button which opens the panel is disabled for a moment while its action runs, so it loses the focus
    opener.blur();
    const { rerender } = renderPanel(true);
    byText("first").focus();

    rerender(<Panel active={false}><button>first</button></Panel>);

    expect(document.activeElement).toBe(opener);
  });

  it("waits for a disabled opener to be enabled again to give it the focus back", async () => {
    const { rerender } = renderPanel(true);
    byText("first").focus();
    opener.disabled = true;

    rerender(<Panel active={false}><button>first</button></Panel>);
    byText("first").blur();
    expect(document.activeElement).not.toBe(opener);

    opener.disabled = false;
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(document.activeElement).toBe(opener);
  });

  it("does not take the focus from a control the user moved to outside the panel", () => {
    const other = document.createElement("input");
    document.body.appendChild(other);
    const { rerender } = renderPanel(true);
    other.focus();

    rerender(<Panel active={false}><button>first</button></Panel>);

    expect(document.activeElement).toBe(other);
  });

  it("does not move the focus when the opener is no longer in the page", () => {
    const { rerender } = renderPanel(true);
    byText("first").focus();
    opener.remove();

    rerender(<Panel active={false}><button>first</button></Panel>);

    expect(document.activeElement).toBe(byText("first"));
  });
});
