import {bestPairing, orderScreens, parseRetries, resolveMode, summarize, unknownScreens} from "../../../../tools/pixel-diff/summary";

const capture = (overrides = {}) => ({browser: "chromium", viewport: "1440x900", theme: "light", screen: "home", differing: 0, sizeMismatch: false,
  routeHits: {before: 3, after: 3}, ...overrides});

describe("tools/pixel-diff/summary", () => {
  describe("summarize", () => {
    it("passes when every capture is identical and the stylesheet was served", () => {
      expect(summarize([capture(), capture({screen: "users"})])).toMatchObject({captures: 2, identical: 2, different: 0, errors: 0, exitCode: 0});
    });

    it("fails with no capture at all", () => {
      expect(summarize([])).toMatchObject({captures: 0, exitCode: 1});
    });

    it("fails on a capture with differing pixels", () => {
      expect(summarize([capture(), capture({screen: "users", differing: 12})])).toMatchObject({identical: 1, different: 1, exitCode: 1});
    });

    it("fails on a size mismatch, which has no pixel count", () => {
      expect(summarize([capture({differing: null, sizeMismatch: true})])).toMatchObject({different: 1, exitCode: 1});
    });

    it("fails on a screen that could not be captured, and does not count it as identical", () => {
      const summary = summarize([capture(), capture({screen: "modal", differing: null, error: "click timeout"})]);
      expect(summary).toMatchObject({captures: 1, identical: 1, errors: 1, exitCode: 1});
    });

    it("fails when only errors were recorded", () => {
      expect(summarize([capture({error: "unknown screen"})])).toMatchObject({captures: 0, errors: 1, exitCode: 1});
    });

    it("fails when a stylesheet was never requested (a false pass: both captures used the same CSS)", () => {
      expect(summarize([capture({routeHits: {before: 0, after: 4}})])).toMatchObject({identical: 0, errors: 1, exitCode: 1});
    });
  });

  describe("two applications (--url-before and --url-after)", () => {
    it("does not ask for a stylesheet swap: the two sides are two applications", () => {
      expect(summarize([capture({routeHits: {before: 0, after: 0}, twoApps: true})])).toMatchObject({identical: 1, errors: 0, exitCode: 0});
    });

    it("still fails on differing pixels", () => {
      expect(summarize([capture({twoApps: true, routeHits: {before: 0, after: 0}, differing: 3})])).toMatchObject({different: 1, exitCode: 1});
    });

    it("still fails on a screen that could not be captured", () => {
      expect(summarize([capture({twoApps: true, routeHits: {before: 0, after: 0}, differing: null, error: "timeout"})])).toMatchObject({errors: 1, exitCode: 1});
    });
  });

  describe("resolveMode", () => {
    it("is a stylesheet swap with an application and two stylesheets", () => {
      expect(resolveMode({url: "http://localhost:1", before: "a.css", after: "b.css"})).toEqual({twoApps: false, urlBefore: "http://localhost:1", urlAfter: "http://localhost:1"});
    });

    it("is two applications with two different addresses", () => {
      expect(resolveMode({urlBefore: "http://localhost:1", urlAfter: "http://localhost:2"}))
        .toEqual({twoApps: true, urlBefore: "http://localhost:1", urlAfter: "http://localhost:2"});
    });

    it("refuses the same application on both sides (nothing would differ)", () => {
      expect(() => resolveMode({urlBefore: "http://localhost:1", urlAfter: "http://localhost:1"})).toThrow(/same/);
    });

    it("sees through a trailing slash and the case of the host", () => {
      expect(() => resolveMode({urlBefore: "http://localhost:1/", urlAfter: "HTTP://LOCALHOST:1"})).toThrow(/same/);
    });

    it("refuses to mix the modes", () => {
      expect(() => resolveMode({url: "http://localhost:1", before: "a.css", after: "b.css", urlBefore: "http://localhost:2", urlAfter: "http://localhost:3"})).toThrow(/mix/);
      expect(() => resolveMode({urlBefore: "http://localhost:2", urlAfter: "http://localhost:3", before: "a.css"})).toThrow(/mix/);
      expect(() => resolveMode({urlBefore: "http://localhost:2", urlAfter: "http://localhost:3", url: "http://localhost:1"})).toThrow(/mix/);
    });

    it("needs both addresses in the two applications mode", () => {
      expect(() => resolveMode({urlBefore: "http://localhost:1"})).toThrow(/both/);
      expect(() => resolveMode({urlAfter: "http://localhost:1"})).toThrow(/both/);
    });

    it("needs the application and both stylesheets in the swap mode", () => {
      expect(() => resolveMode({url: "http://localhost:1", before: "a.css"})).toThrow(/--after/);
      expect(() => resolveMode({before: "a.css", after: "b.css"})).toThrow(/--url/);
      expect(() => resolveMode({})).toThrow(/--url/);
    });

    it("refuses the same stylesheet on both sides", () => {
      expect(() => resolveMode({url: "http://localhost:1", before: "a.css", after: "a.css"})).toThrow(/same/);
    });
  });

  describe("bestPairing", () => {
    it("picks the pairing that differs least, keeping which captures it is made of", () => {
      expect(bestPairing([{before: 0, after: 0, differing: 30}, {before: 1, after: 0, differing: 0}, {before: 1, after: 1, differing: 5}]))
        .toEqual({before: 1, after: 0, differing: 0});
    });

    it("never prefers a size mismatch to a pixel count", () => {
      expect(bestPairing([{before: 0, after: 0, differing: null, sizeMismatch: true}, {before: 0, after: 1, differing: 9}]))
        .toMatchObject({after: 1, differing: 9});
    });

    it("reports the mismatch when every pairing has one", () => {
      expect(bestPairing([{before: 0, after: 0, differing: null, sizeMismatch: true}])).toMatchObject({sizeMismatch: true});
    });
  });

  describe("options", () => {
    it("accepts a non-negative integer of retries", () => {
      expect(parseRetries("0")).toBe(0);
      expect(parseRetries("4")).toBe(4);
    });

    it.each(["-1", "2.5", "abc", "", undefined])("rejects %p retries", value => {
      expect(() => parseRetries(value)).toThrow(/retries/);
    });

    it("lists the screens that do not exist", () => {
      expect(unknownScreens(["home", "nope", "users"], ["home", "users"])).toEqual(["nope"]);
    });
  });

  describe("orderScreens", () => {
    const screens = [{name: "a"}, {name: "m1", module: "Test"}, {name: "login", public: true}, {name: "b"}, {name: "m2", module: "Test"}, {name: "o", module: "Other"}];

    it("runs the public screens, then the screens of the default module, then each module's screens together", () => {
      expect(orderScreens(screens).map(screen => screen.name)).toEqual(["login", "a", "b", "m1", "m2", "o"]);
    });

    it("keeps the order inside each group and does not change its argument", () => {
      const copy = JSON.stringify(screens);
      orderScreens(screens);
      expect(JSON.stringify(screens)).toBe(copy);
    });

    it("selects a module once per run of consecutive screens of it", () => {
      const ordered = orderScreens(screens);
      const changes = ordered.filter((screen, index) => screen.module && screen.module !== (ordered[index - 1] || {}).module);
      expect(changes.map(screen => screen.name)).toEqual(["m1", "o"]);
    });
  });
});
