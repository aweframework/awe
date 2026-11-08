import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

void DefaultSettings;

// The panel of an expired session (#935): one blocking panel, with the title ERROR_TITLE_SESSION_EXPIRED and the
// message ERROR_MESSAGE_SESSION_EXPIRED, and one button (BUTTON_RESTART_APPLICATION) which reloads the application
// and lands on the start screen, inside the context path. No close, no cancel: the session is gone
describe("Session expired panel", () => {
  let $screen, $actionController, $rootScope, $injector, $utilities, $windowMock;

  const action = () => ({accept: jest.fn(), attr: jest.fn()});

  beforeEach(() => {
    $windowMock = {location: {assign: jest.fn(), href: ""}};
    angular.mock.module("aweApplication", {$window: $windowMock});
    inject(["$injector", function (services) {
      $injector = services;
      $screen = services.get("Screen");
      $actionController = services.get("ActionController");
      $rootScope = services.get("$rootScope");
      $utilities = services.get("AweUtilities");
      // The digest of the panel issues the application settings read the client always reads
      services.get("$httpBackend").when("POST", "settings").respond(DefaultSettings);
      jest.spyOn($actionController, "acceptAction").mockImplementation((received) => received.accept());
    }]);
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  it("opens one blocking panel, with no close and no cancel, and finishes the action", () => {
    const panelAction = action();

    $screen.sessionExpired(panelAction);
    $rootScope.$digest();

    const panel = document.querySelector('[data-testid="session-expired-panel"]');
    expect(panel).not.toBeNull();
    expect(panel.classList.contains("modal")).toBe(true);
    // The backdrop is static: the panel is not dismissed by a click outside of it
    expect(panel.getAttribute("data-backdrop")).toBe("static");
    // The session is gone: nothing closes the panel
    expect(panel.querySelectorAll("[data-dismiss]")).toHaveLength(0);
    // Exactly one control: the restart button, whose icon is the refresh icon of the client, not an empty one
    expect(panel.querySelectorAll("button")).toHaveLength(1);
    expect(panel.querySelector('[data-testid="session-expired-restart"]')).not.toBeNull();
    expect(panel.querySelector('[data-testid="session-expired-restart"] use')?.getAttribute("href")).toContain("#refresh-cw");
    // The action which opened the panel is finished
    expect($actionController.acceptAction).toHaveBeenCalledWith(panelAction);
  });

  it("shows the panel through the client modal machinery, which cannot be closed with Escape", () => {
    const show = jest.spyOn($injector.get("AweModal"), "show").mockImplementation();

    $screen.sessionExpired(action());
    $rootScope.$digest();

    // With no dismissable control the keyboard option of the panel is "false": Escape does not close it either
    expect(show).toHaveBeenCalledWith(expect.anything(), {keyboard: false});
  });

  it("restarts the application navigating to its root inside the application context path", () => {
    // A WAR served at /awe restarts the application inside its context path, not at the container root
    jest.spyOn($utilities, "getContextPath").mockReturnValue("/awe");
    $screen.sessionExpired(action());
    $rootScope.$digest();

    document.querySelector('[data-testid="session-expired-restart"]').click();

    expect($windowMock.location.assign).toHaveBeenCalledWith("/awe/");
  });

  it("dispatches the client action of the action registry of the screen controller and opens the panel", () => {
    const sessionScope = $rootScope.$new();
    $injector.get("$controller")("ScreenController", {$scope: sessionScope});
    const sessionAction = action();

    sessionScope.$emit("/action/session-expired", sessionAction);
    $rootScope.$digest();

    expect(document.querySelector('[data-testid="session-expired-panel"]')).not.toBeNull();
    expect($actionController.acceptAction).toHaveBeenCalledWith(sessionAction);
  });

  it("translates the title, the message and the button label", () => {
    // As the sign-in page tests do, the translation boundary is asked with the locale the client carries
    jest.spyOn($injector.get("$translate"), "instant").mockImplementation(key => ({
      ERROR_TITLE_SESSION_EXPIRED: "Session expired",
      ERROR_MESSAGE_SESSION_EXPIRED: "Your connection has expired. You must login again.",
      BUTTON_RESTART_APPLICATION: "Reload and go to start"
    }[key] || key));

    $screen.sessionExpired(action());
    $rootScope.$digest();

    const panel = document.querySelector('[data-testid="session-expired-panel"]');
    expect(panel.querySelector(".modal-title").textContent).toContain("Session expired");
    expect(panel.querySelector(".modal-body").textContent).toContain("Your connection has expired");
    expect(panel.querySelector("button").textContent).toContain("Reload and go to start");
  });
});
