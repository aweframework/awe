import "../../../main/resources/js/awe/app";
import "../../../main/resources/webpack/locals-en-GB.config";
import "../../../main/resources/webpack/locals-es-ES.config";

describe('awe-framework/awe-client-angular/src/test/jest/services/utilities.js', function() {
  let $injector, $utilities;
  // Mock module
  beforeEach(function() {
    angular.mock.module('aweApplication');

    inject(["$injector", function(__$injector__) {
      $injector = __$injector__;
      $utilities = $injector.get('AweUtilities');
    }]);

    jest.setTimeout(10000);
  });

  afterEach(function() {
  });

  // Compare equal values
  it('should compare equal values', function() {
    // Assert
    expect($utilities.compareEqualValues(null, "lala")).toBe(false);
    expect($utilities.compareEqualValues(null, null)).toBe(true);
    expect($utilities.compareEqualValues(123123, 23)).toBe(false);
    expect($utilities.compareEqualValues(1231, 1231)).toBe(true);
    expect($utilities.compareEqualValues("tutu lala", "tutu lala")).toBe(true);
    expect($utilities.compareEqualValues("tutu lala asa", "tutu lala")).toBe(false);
  });

  // Compare contain values
  it('should compare contain values', function() {
    // Assert
    expect($utilities.compareContainValues(null, "lala")).toBe(false);
    expect($utilities.compareContainValues(null, null)).toBe(true);
    expect($utilities.compareContainValues(123123, 23)).toBe(true);
    expect($utilities.compareContainValues("tutu lala ellele", "elle")).toBe(true);
    expect($utilities.compareContainValues("tutu lala ellele", "jander")).toBe(false);
    expect($utilities.compareContainValues("jander", "jander clander")).toBe(false);
  });

  it('should get state', function() {
    // Assert
    expect($utilities.getState("/screen/public/home/tutu", false)).toEqual({to: 'public.screen', parameters: {screenId: "home", subScreenId: "tutu", r: undefined}});
    expect($utilities.getState("/screen/private/home/tutu", false)).toEqual({to: 'private.screen', parameters: {screenId: "home", subScreenId: "tutu", r: undefined}});
    expect($utilities.getState("/screen/tutu", false)).toEqual({to: 'global', parameters: {screenId: "tutu", r: undefined}});
    expect($utilities.getState("/", false)).toEqual({to: 'index', parameters: {r: undefined}});
    expect($utilities.getState("/ascasc/Asvasv", false)).toEqual({to: 'index', parameters: {r: undefined}});
    expect($utilities.getState("/screen/public/home/tutu", true).to).toEqual('public.screen');
    expect($utilities.getState("/screen/public/home/tutu", true).parameters.screenId).toEqual('home');
    expect($utilities.getState("/screen/public/home/tutu", true).parameters.subScreenId).toEqual('tutu');
    expect($utilities.getState("/screen/public/home/tutu", true).parameters.r).not.toBe(undefined);
    expect($utilities.getState("/screen/public/home/tutu", false).parameters.r).toBe(undefined);
  });

  it('should get row index', function() {
    // Generate model
    let values = [
      {id: 1, _ID: "1"},
      {id: 2, _ID: "3"},
      {id: 3, _ID: "5"},
      {id: 4, _ID: "6"},
      {id: 5, _ID: "7"}
    ];

    // Assert
    expect($utilities.getRowIndex(values, 2, "_ID")).toEqual(-1);
    expect($utilities.getRowIndex(values, 3, "_ID")).toEqual(1);
    expect($utilities.getRowIndex(values, 7, "_ID")).toEqual(4);
    expect($utilities.getRowIndex(values, "1", "_ID")).toEqual(0);
    expect($utilities.getRowIndex(values, 2, "id")).toEqual(1);
    expect($utilities.getRowIndex(values, "a", "id")).toEqual(-1);
    expect($utilities.getRowIndex(values, "3", "id")).toEqual(2);
    expect($utilities.getRowIndex(values, 1, "id")).toEqual(0);
  });

  it('should decode data', function () {
    let data = '{"test": 1}';

    // Assert
    expect($utilities.decodeData($utilities.encodeSymetric(data), true)).toEqual({"test": 1});
    expect($utilities.decodeData(data, false)).toEqual({"test": 1});
  });

  it('should call stick to bottom', function () {
    const element = {animate: (a, o) => o.complete(), trigger: jest.fn().mockName("trigger")};
    $utilities.timeout = (fn) => fn();

    $utilities.stickToBottom(true, 0, false, element);

    // Assert
    expect(element.trigger).toHaveBeenCalledWith("scroll");

    element.trigger = jest.fn().mockName("trigger2");
    $utilities.stickToBottom(false, 0, false, element);

    // Assert
    expect(element.trigger).not.toHaveBeenCalled();

    element.trigger = jest.fn().mockName("trigger3");
    $utilities.stickToBottom(false, 0, true, element);

    // Assert
    expect(element.trigger).toHaveBeenCalledWith("scroll");
  });

  it('should manage a rest error', function () {
    const actions1 = $utilities.manageRestError({status: 401, title: "", message: ""});
    expect(actions1.length).toEqual(2);

    const actions2 = $utilities.manageRestError({status: 403, title: "", message: ""}, "tutu");
    expect(actions2.length).toEqual(2);

    const actions3 = $utilities.manageRestError({status: -1, title: "", message: ""}, "tutu");
    expect(actions3.length).toEqual(2);

    const actions4 = $utilities.manageRestError({status: 401, title: "", message: ""}, "tutu");
    expect(actions4.length).toEqual(3);

    const actions5 = $utilities.manageRestError({status: 413, title: "", message: ""});
    expect(actions5.length).toEqual(1);

    const actions6 = $utilities.manageRestError({status: 504, title: "", message: ""}, "tutu");
    expect(actions6.length).toEqual(2);
  });

  describe('copyToClipboard', function () {
    let $log, writeText;
    beforeEach(function () {
      $log = $injector.get('$log');
      jest.spyOn($log, 'error').mockImplementation(() => {});
      writeText = jest.fn();
      Object.defineProperty(navigator, 'clipboard', {value: {writeText}, configurable: true});
    });

    it('should copy the text and log nothing when the clipboard accepts it', async function () {
      writeText.mockResolvedValue(undefined);
      await $utilities.copyToClipboard("some text", "Copying test value");
      expect(writeText).toHaveBeenCalledWith("some text");
      expect($log.error).not.toHaveBeenCalled();
    });

    it('should log the failure with its context instead of leaving the promise rejected', async function () {
      const error = new Error("denied");
      writeText.mockRejectedValue(error);
      await expect($utilities.copyToClipboard("some text", "Copying test value")).resolves.toBeUndefined();
      expect($log.error).toHaveBeenCalledWith("[ERROR] Copying test value to the clipboard", error);
    });

    it('should log and resolve when the clipboard API is not available (non-secure context)', async function () {
      Object.defineProperty(navigator, 'clipboard', {value: undefined, configurable: true});
      await expect($utilities.copyToClipboard("some text", "Copying test value")).resolves.toBeUndefined();
      expect($log.error).toHaveBeenCalledWith("[ERROR] Copying test value to the clipboard", expect.any(Error));
    });
  });

  describe('behaviour pinned before the lint cleanup', function () {
    it('should remove every child node of a node', function () {
      const node = document.createElement("div");
      node.appendChild(document.createElement("span"));
      node.appendChild(document.createTextNode("text"));
      node.appendChild(document.createComment("comment"));
      node.appendChild(document.createElement("b"));

      $utilities.removeChildren(node);

      expect(node.childNodes.length).toBe(0);
      expect(node.firstChild).toBeNull();
    });

    it('should leave an empty node empty', function () {
      const node = document.createElement("div");
      $utilities.removeChildren(node);
      expect(node.childNodes.length).toBe(0);
    });

    it('should url encode spaces, percent signs, dashes, slashes and reserved characters', function () {
      expect($utilities.urlEncode("a b")).toBe("a+b");
      expect($utilities.urlEncode("100%")).toBe("100%25");
      expect($utilities.urlEncode("a-b_c.d")).toBe("a-b_c.d");
      expect($utilities.urlEncode("a/b")).toBe("a%2Fb");
      expect($utilities.urlEncode("it's (1)*~!")).toBe("it%27s+%281%29%2a%7e%21");
    });

    it('should url decode what it encodes and the plus sign as a space', function () {
      expect($utilities.urlDecode("a+b")).toBe("a b");
      expect($utilities.urlDecode("100%25")).toBe("100%");
      expect($utilities.urlDecode("a%2Fb")).toBe("a/b");
      expect($utilities.urlDecode("it%27s+%281%29%2A%7E%21")).toBe("it's (1)*~!");
      ["a b", "100%", "a-b/c d", "it's (1)*~!", "ñ é-/%"].forEach(function (value) {
        expect($utilities.urlDecode($utilities.urlEncode(value))).toBe(value);
      });
    });

    it('should tell numbers from other types', function () {
      expect($utilities.isNumber(12)).toBe(true);
      expect($utilities.isNumber(-3.5)).toBe(true);
      expect($utilities.isNumber("12")).toBe(true);
      expect($utilities.isNumber("-12")).toBe(true);
      expect($utilities.isNumber("1.5")).toBe(true);
      // The string pattern is not anchored, so any string matches it (empty matches too)
      expect($utilities.isNumber("abc")).toBe(true);
      expect($utilities.isNumber("")).toBe(true);
      expect($utilities.isNumber(true)).toBe(false);
      expect($utilities.isNumber(null)).toBe(false);
      expect($utilities.isNumber(undefined)).toBe(false);
      expect($utilities.isNumber({})).toBe(false);
    });
  });
});
