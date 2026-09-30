import {getUID} from "../../../../src/redux/actions/settings";

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe("awe-react-client/test/js/redux/actions/settingsTest.js", () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  describe("getUID", () => {
    it("returns a version 4 UUID", () => {
      expect(getUID()).toMatch(UUID_V4);
    });

    it("returns a different value on each call", () => {
      expect(getUID()).not.toBe(getUID());
    });

    it("does not use Math.random", () => {
      const random = jest.spyOn(Math, "random");

      getUID();

      expect(random).not.toHaveBeenCalled();
    });

    it("falls back to crypto.getRandomValues when crypto.randomUUID is not available", () => {
      // crypto.randomUUID only exists in secure contexts (https, localhost)
      const original = Object.getOwnPropertyDescriptor(globalThis.crypto, "randomUUID")
        || Object.getOwnPropertyDescriptor(Object.getPrototypeOf(globalThis.crypto), "randomUUID");
      Object.defineProperty(globalThis.crypto, "randomUUID", {value: undefined, configurable: true});
      const getRandomValues = jest.spyOn(globalThis.crypto, "getRandomValues");
      try {
        const uid = getUID();

        expect(getRandomValues).toHaveBeenCalledTimes(1);
        expect(uid).toMatch(UUID_V4);
        expect(getUID()).not.toBe(uid);
      } finally {
        if (original) {
          Object.defineProperty(globalThis.crypto, "randomUUID", original);
        } else {
          delete globalThis.crypto.randomUUID;
        }
      }
    });
  });
});
