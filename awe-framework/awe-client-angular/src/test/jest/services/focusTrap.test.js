import {createFocusTrap} from "../../../main/resources/js/awe/services/focusTrap";

describe("awe-framework/awe-client-angular/src/test/jest/services/focusTrap.test.js", () => {
  let opener, container, trap;

  const byText = text => [...container.querySelectorAll("button")].find(button => button.textContent === text);
  const tab = (shiftKey = false) => {
    const event = new window.KeyboardEvent("keydown", {key: "Tab", shiftKey, bubbles: true, cancelable: true});
    document.activeElement.dispatchEvent(event);
    return event;
  };

  beforeEach(() => {
    opener = document.createElement("button");
    opener.textContent = "opener";
    container = document.createElement("div");
    container.innerHTML = "<button>first</button><input/><button>last</button><button disabled>off</button>" +
      "<button tabindex='-1'>skipped</button>";
    document.body.append(opener, container);
    opener.focus();
    trap = createFocusTrap(() => container);
  });

  afterEach(() => {
    trap.deactivate();
    document.body.innerHTML = "";
  });

  it("moves the focus to the first control when asked", () => {
    trap.activate();

    trap.focusFirst();

    expect(document.activeElement).toBe(byText("first"));
  });

  it("does not move the focus when it is already inside", () => {
    trap.activate();
    container.querySelector("input").focus();

    trap.focusFirst();

    expect(document.activeElement).toBe(container.querySelector("input"));
  });

  it("moves Tab from the last control to the first one", () => {
    trap.activate();
    byText("last").focus();

    const event = tab();

    expect(event.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(byText("first"));
  });

  it("moves Shift+Tab from the first control to the last one, skipping disabled and untabbable controls", () => {
    trap.activate();
    byText("first").focus();

    tab(true);

    expect(document.activeElement).toBe(byText("last"));
  });

  it("lets Tab move between the controls in the middle", () => {
    trap.activate();
    byText("first").focus();

    expect(tab().defaultPrevented).toBe(false);
  });

  it.each([
    ["a dialog", "<div class='modal'><button>dialog first</button><button>dialog last</button></div>"],
    ["an aria modal", "<div role='dialog' aria-modal='true'><button>dialog first</button><button>dialog last</button></div>"]
  ])("leaves Tab alone inside %s shown over the container", (name, html) => {
    const dialog = document.createElement("div");
    dialog.innerHTML = html;
    document.body.append(dialog);
    trap.activate();
    [...dialog.querySelectorAll("button")].find(button => button.textContent === "dialog last").focus();

    const event = tab();

    expect(event.defaultPrevented).toBe(false);
    expect(document.activeElement.textContent).toBe("dialog last");
  });

  it("brings the focus back inside from a dialog that holds the container", () => {
    const dialog = document.createElement("div");
    dialog.className = "modal";
    dialog.innerHTML = "<button>outer</button>";
    dialog.append(container);
    document.body.append(dialog);
    trap.activate();
    dialog.querySelector("button").focus();

    tab();

    expect(document.activeElement).toBe(byText("first"));
  });

  it("brings the focus back inside when Tab is pressed outside", () => {
    trap.activate();
    opener.focus();

    tab();

    expect(document.activeElement).toBe(byText("first"));
  });

  it("keeps the focus on the container when it has no focusable control", () => {
    container.innerHTML = "<span>text</span>";
    trap.activate();

    tab();

    expect(document.activeElement).toBe(container);
  });

  it("gives the focus back to the control focused when it was activated", () => {
    trap.activate();
    byText("first").focus();

    trap.deactivate();

    expect(document.activeElement).toBe(opener);
  });

  it("stops trapping Tab once it is deactivated", () => {
    trap.activate();
    trap.deactivate();
    byText("last").focus();

    expect(tab().defaultPrevented).toBe(false);
    expect(document.activeElement).toBe(byText("last"));
  });

  it("gives the focus back to the control focused last when the focus was lost before it was activated", () => {
    // The button which opens the panel may lose the focus while its action runs
    opener.blur();
    trap.activate();
    byText("first").focus();

    trap.deactivate();

    expect(document.activeElement).toBe(opener);
  });

  it("waits for a disabled opener to be enabled again to give it the focus back", async () => {
    trap.activate();
    byText("first").focus();
    opener.disabled = true;

    trap.deactivate();
    byText("first").blur();
    expect(document.activeElement).not.toBe(opener);

    opener.disabled = false;
    await new Promise(resolve => setTimeout(resolve, 0));

    expect(document.activeElement).toBe(opener);
  });

  it("does not take the focus from a control the user moved to outside the container", () => {
    const other = document.createElement("input");
    document.body.appendChild(other);
    trap.activate();
    other.focus();

    trap.deactivate();

    expect(document.activeElement).toBe(other);
  });

  it("does not move the focus when the opener is no longer in the page", () => {
    trap.activate();
    byText("first").focus();
    opener.remove();

    trap.deactivate();

    expect(document.activeElement).toBe(byText("first"));
  });
});
