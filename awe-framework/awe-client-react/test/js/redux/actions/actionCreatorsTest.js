import * as actions from "../../../../src/redux/actions/actions";
import * as components from "../../../../src/redux/actions/components";
import * as messages from "../../../../src/redux/actions/messages";
import * as runtime from "../../../../src/redux/actions/runtime";
import * as view from "../../../../src/redux/actions/view";
import {navigationActions} from "../../../../src/redux/actions/navigation";
import {UPDATE_SCREEN, updateScreen} from "../../../../src/redux/actions/screen";
import {UPDATE_SIZE, updateSize} from "../../../../src/redux/actions/size";

describe("awe-react-client/test/js/redux/actions/actionCreatorsTest.js", () => {
  const address = {view: "report", component: "Grd"};
  const data = {value: 1};

  describe("actions", () => {
    it("creators that carry a payload", () => {
      const payload = {id: "a"};
      expect(actions.addAction(payload)).toEqual({type: actions.ADD_ACTION, payload});
      expect(actions.removeAction(payload)).toEqual({type: actions.REMOVE_ACTION, payload});
      expect(actions.addActions([payload])).toEqual({type: actions.ADD_ACTIONS, payload: [payload]});
      expect(actions.startAction(payload)).toEqual({type: actions.START_ACTION, payload});
      expect(actions.runAction(payload)).toEqual({type: actions.RUN_ACTION, payload});
      expect(actions.acceptAction(payload)).toEqual({type: actions.ACCEPT_ACTION, payload});
      expect(actions.rejectAction(payload)).toEqual({type: actions.REJECT_ACTION, payload});
      expect(actions.abortAction(payload)).toEqual({type: actions.ABORT_ACTION, payload});
    });

    it("addActionsTop carries the actions twice, as `actions` and as `payload`", () => {
      const list = [{id: "a"}];
      expect(actions.addActionsTop(list)).toEqual({type: actions.ADD_ACTIONS_TOP, actions: list, payload: list});
    });

    it("creators without payload", () => {
      expect(actions.addStack()).toEqual({type: actions.ADD_STACK});
      expect(actions.removeStack()).toEqual({type: actions.REMOVE_STACK});
      expect(actions.deleteStack()).toEqual({type: actions.DELETE_STACK});
      expect(actions.closeAllActions()).toEqual({type: actions.CLOSE_ALL_ACTIONS});
    });

    it("toggleActionsRunning carries the running flag", () => {
      expect(actions.toggleActionsRunning(true)).toEqual({type: actions.TOGGLE_ACTIONS_RUNNING, running: true});
      expect(actions.toggleActionsRunning(false)).toEqual({type: actions.TOGGLE_ACTIONS_RUNNING, running: false});
    });

    it("keeps the type constants and the statuses", () => {
      expect(actions.ADD_ACTION).toBe("ADD_ACTION");
      expect(actions.TOGGLE_ACTIONS_RUNNING).toBe("TOGGLE_ACTIONS_RUNNING");
      expect(actions.ActionStatus).toEqual({
        STATUS_INITIAL: "STATUS_INITIAL",
        STATUS_RUNNING: "STATUS_RUNNING",
        STATUS_STARTED: "STATUS_STARTED",
        STATUS_ACCEPTED: "STATUS_ACCEPTED",
        STATUS_REJECTED: "STATUS_REJECTED",
        STATUS_ABORTED: "STATUS_ABORTED"
      });
    });

    describe("delayActions", () => {
      beforeEach(() => jest.useFakeTimers());
      afterEach(() => jest.useRealTimers());

      it("runs the function right away without a time", () => {
        const fn = jest.fn();
        actions.delayActions(0, fn);
        expect(fn).toHaveBeenCalledTimes(1);
        actions.delayActions(undefined, fn);
        expect(fn).toHaveBeenCalledTimes(2);
      });

      it("runs the function after the time", () => {
        const fn = jest.fn();
        actions.delayActions(100, fn);
        expect(fn).not.toHaveBeenCalled();
        jest.advanceTimersByTime(100);
        expect(fn).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe("components", () => {
    it("view and registry creators", () => {
      expect(components.clearComponents("report")).toEqual({type: components.CLEAR_COMPONENTS, view: "report"});
      expect(components.clearAllComponents()).toEqual({type: components.CLEAR_ALL_COMPONENTS});
      expect(components.updateViewComponents("report", data))
        .toEqual({type: components.UPDATE_VIEW_COMPONENTS, view: "report", data});
      expect(components.registerComponents(data)).toEqual({type: components.REGISTER_COMPONENTS, data});
      expect(components.unregisterComponents(["a"])).toEqual({type: components.UNREGISTER_COMPONENTS, ids: ["a"]});
    });

    it("creators of an address and its data", () => {
      const byAddress = {
        updateComponent: components.UPDATE_COMPONENT,
        updateAttributes: components.UPDATE_ATTRIBUTES,
        updateSpecificAttributes: components.UPDATE_SPECIFIC_ATTRIBUTES,
        updateModel: components.UPDATE_MODEL,
        updateRowModel: components.UPDATE_ROW_MODEL,
        updateValidation: components.UPDATE_VALIDATION,
        updateColumnAttributes: components.UPDATE_COLUMN_ATTRIBUTES,
        keepValidation: components.KEEP_VALIDATION,
        keepAttribute: components.KEEP_ATTRIBUTE,
        keepModel: components.KEEP_MODEL,
        keepRowModel: components.KEEP_ROW_MODEL,
        restoreValidation: components.RESTORE_VALIDATION,
        restoreAttribute: components.RESTORE_ATTRIBUTE,
        resetModel: components.RESET_MODEL
      };
      Object.entries(byAddress).forEach(([creator, type]) => {
        expect(components[creator](address, data)).toEqual({type, address, data});
      });
      expect(components.keepModel(address)).toEqual({type: components.KEEP_MODEL, address, data: undefined});
    });

    it("creators of a list of components", () => {
      const list = [{address, data}];
      const byList = {
        updateMultipleComponents: components.UPDATE_MULTIPLE_COMPONENTS,
        updateMultipleModels: components.UPDATE_MULTIPLE_MODELS,
        updateMultipleAttributes: components.UPDATE_MULTIPLE_ATTRIBUTES,
        updateMultipleValidation: components.UPDATE_MULTIPLE_VALIDATION,
        updateMultipleColumnAttributes: components.UPDATE_MULTIPLE_COLUMN_ATTRIBUTES,
        restoreMultipleValidation: components.RESTORE_MULTIPLE_VALIDATION,
        restoreMultipleAttributes: components.RESTORE_MULTIPLE_ATTRIBUTES,
        resetMultipleModel: components.RESET_MULTIPLE_MODEL
      };
      Object.entries(byList).forEach(([creator, type]) => {
        expect(components[creator](list)).toEqual({type, componentList: list});
      });
    });

    it("restore creators carry the initial flag, false by default", () => {
      expect(components.restoreModel(address, data)).toEqual({type: components.RESTORE_MODEL, address, data, initial: false});
      expect(components.restoreModel(address, data, true).initial).toBe(true);
      const list = [{address}];
      expect(components.restoreMultipleModel(list))
        .toEqual({type: components.RESTORE_MULTIPLE_MODEL, componentList: list, initial: false});
      expect(components.restoreMultipleModel(list, true).initial).toBe(true);
    });

    it("keeps the statuses and the button types", () => {
      expect(components.ComponentStatus).toEqual({
        STATUS_DEFINED: "defined", STATUS_COMPILED: "compiled", STATUS_INITIALIZED: "initialized"
      });
      expect(components.ButtonTypes).toEqual({BUTTON_NORMAL: "button", BUTTON_RESET: "reset", BUTTON_SUBMIT: "submit"});
      expect(components.VALIDATE_COMPONENTS).toBe("VALIDATE_COMPONENTS");
      expect(components.VALIDATE_ROW).toBe("VALIDATE_ROW");
    });
  });

  describe("messages", () => {
    it("creators", () => {
      expect(messages.addMessage(data)).toEqual({type: messages.ADD_MESSAGE, data});
      expect(messages.showMessages(data)).toEqual({type: messages.SHOW_MESSAGES, data});
      expect(messages.removeMessage(data)).toEqual({type: messages.REMOVE_MESSAGE, data});
      expect(messages.updateMessages("report", data)).toEqual({type: messages.UPDATE_MESSAGES, view: "report", data});
      expect(messages.confirmMessage(data)).toEqual({type: messages.CONFIRM_MESSAGE, data});
      expect(messages.removeConfirm()).toEqual({type: messages.REMOVE_CONFIRM});
      expect(messages.showPopover(data)).toEqual({type: messages.SHOW_POPOVER, data});
      expect(messages.removePopover(data)).toEqual({type: messages.REMOVE_POPOVER, data});
    });

    it("keeps the message types", () => {
      expect(messages.MessageType).toEqual({
        MESSAGE_OK: "ok", MESSAGE_WARNING: "warning", MESSAGE_ERROR: "error", MESSAGE_INFO: "info", MESSAGE_WRONG: "wrong"
      });
    });
  });

  describe("runtime", () => {
    it("setRuntimeEvent stamps the event with the time", () => {
      jest.spyOn(Date, "now").mockReturnValue(1234);
      expect(runtime.setRuntimeEvent(address, "change"))
        .toEqual({type: runtime.SET_RUNTIME_EVENT, address, event: "change", timestamp: 1234});
      jest.restoreAllMocks();
    });

    it("clearRuntimeEvent has no payload", () => {
      expect(runtime.clearRuntimeEvent()).toEqual({type: runtime.CLEAR_RUNTIME_EVENT});
    });
  });

  describe("view, screen and size", () => {
    it("view creators", () => {
      expect(view.setView("report", data)).toEqual({type: view.SET_VIEW, view: "report", data});
      expect(view.updateView("report", data)).toEqual({type: view.UPDATE_VIEW, view: "report", data});
      expect(view.clearView("report")).toEqual({type: view.CLEAR_VIEW, view: "report"});
      expect(view.clearAllViews()).toEqual({type: view.CLEAR_ALL_VIEWS});
      expect(view.SET_VIEW).toBe("SET_SCREEN_VIEW");
      expect(view.UPDATE_VIEW).toBe("UPDATE_SCREEN_VIEW");
      expect(view.CLEAR_VIEW).toBe("CLEAR_SCREEN_VIEW");
    });

    it("screen and size creators", () => {
      expect(updateScreen(data)).toEqual({type: UPDATE_SCREEN, data});
      expect(updateSize(data)).toEqual({type: UPDATE_SIZE, data});
    });
  });

  describe("navigation", () => {
    it("navigateTo navigates with the extra argument and dispatches the target", () => {
      const dispatch = jest.fn();
      const navigate = jest.fn();
      navigationActions.navigateTo("/home", {replace: true})(dispatch, () => ({}), {navigate});
      expect(navigate).toHaveBeenCalledWith("/home", {replace: true});
      expect(dispatch).toHaveBeenCalledWith({type: "NAVIGATE_TO", payload: "/home"});
    });

    it("navigateTo passes empty options by default", () => {
      const navigate = jest.fn();
      navigationActions.navigateTo(-1)(jest.fn(), () => ({}), {navigate});
      expect(navigate).toHaveBeenCalledWith(-1, {});
    });
  });
});
