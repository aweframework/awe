import {
  formatDate, getAvailableDates, getDisabledDates, isDateRangeValid, parsePresets, resolvePreset, toDate
} from "../../../src/utilities/dates";

const day = (date) => `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;

describe("awe-react-client/test/js/utilities/datesTest.js", () => {
  describe("getAvailableDates", () => {
    it("sorts the dates chronologically, not alphabetically", () => {
      const available = getAvailableDates([{value: "02/02/2024"}, {value: "30/01/2024"}, {value: "15/03/2023"}]);

      expect(available.map(day)).toEqual(["2023-3-15", "2024-1-30", "2024-2-2"]);
    });
  });

  describe("getDisabledDates", () => {
    it("returns every day between the first and the last available date that is not available", () => {
      const available = [toDate("01/01/2024"), toDate("03/01/2024"), toDate("06/01/2024")];

      const disabled = getDisabledDates(available);

      expect(disabled.map(day)).toEqual(["2024-1-2", "2024-1-4", "2024-1-5"]);
    });

    it("returns a distinct Date instance for each disabled day", () => {
      const available = [toDate("01/01/2024"), toDate("04/01/2024")];

      const disabled = getDisabledDates(available);

      expect(disabled).toHaveLength(2);
      expect(disabled[0]).not.toBe(disabled[1]);
      expect(disabled[0].getTime()).not.toBe(disabled[1].getTime());
    });

    it("does not modify the available dates", () => {
      const available = [toDate("01/01/2024"), toDate("04/01/2024")];

      getDisabledDates(available);

      expect(available.map(day)).toEqual(["2024-1-1", "2024-1-4"]);
    });

    it("compares by day, not by Date instance", () => {
      const available = [toDate("01/01/2024"), toDate("02/01/2024"), toDate("03/01/2024")];

      expect(getDisabledDates(available)).toEqual([]);
    });

    it("returns no disabled dates without available dates or with a single one", () => {
      expect(getDisabledDates([])).toEqual([]);
      expect(getDisabledDates([toDate("05/03/2024")])).toEqual([]);
    });

    it("crosses month boundaries", () => {
      const available = getAvailableDates([{value: "30/01/2024"}, {value: "02/02/2024"}]);

      expect(getDisabledDates(available).map(day)).toEqual(["2024-1-31", "2024-2-1"]);
    });
  });

  describe("formatDate", () => {
    it("formats a date as dd/mm/yyyy with zero padding", () => {
      expect(formatDate(new Date(2026, 0, 5))).toBe("05/01/2026");
    });

    it("returns an empty string for an empty date", () => {
      expect(formatDate(null)).toBe("");
    });
  });

  describe("parsePresets", () => {
    it("splits a list by commas or spaces, upper-casing and dropping the empty ones", () => {
      expect(parsePresets("1m, 3M  ytd,,all")).toEqual(["1M", "3M", "YTD", "ALL"]);
    });

    it("returns an empty list when there are no presets", () => {
      expect([parsePresets(undefined), parsePresets(null), parsePresets("")]).toEqual([[], [], []]);
    });
  });

  describe("resolvePreset", () => {
    const today = new Date(2026, 9, 12);

    it.each([
      ["TODAY", "12/10/2026", "12/10/2026"],
      ["7D", "05/10/2026", "12/10/2026"],
      ["2W", "28/09/2026", "12/10/2026"],
      ["1M", "12/09/2026", "12/10/2026"],
      ["3M", "12/07/2026", "12/10/2026"],
      ["12M", "12/10/2025", "12/10/2026"],
      ["3Y", "12/10/2023", "12/10/2026"],
      ["YTD", "01/01/2026", "12/10/2026"],
      ["all", "", ""],
    ])("resolves %s from the current date", (code, from, to) => {
      expect(resolvePreset(code, today)).toEqual({from, to});
    });

    it("keeps the last day of a shorter month when subtracting months", () => {
      expect(resolvePreset("1M", new Date(2026, 2, 31))).toEqual({from: "28/02/2026", to: "31/03/2026"});
    });

    it("keeps the last day of February when subtracting years from a leap day", () => {
      expect(resolvePreset("1Y", new Date(2024, 1, 29))).toEqual({from: "28/02/2023", to: "29/02/2024"});
    });

    it("uses the system date when no date is given", () => {
      jest.useFakeTimers().setSystemTime(new Date(2026, 4, 20));
      try {
        expect(resolvePreset("TODAY")).toEqual({from: "20/05/2026", to: "20/05/2026"});
      } finally {
        jest.useRealTimers();
      }
    });

    it("returns null for an unknown preset", () => {
      expect(resolvePreset("FOREVER", today)).toBeNull();
    });
  });

  describe("isDateRangeValid", () => {
    it.each([
      ["01/01/2026", "31/01/2026", true],
      ["15/01/2026", "15/01/2026", true],
      ["15/01/2026", "14/01/2026", false],
      ["", "14/01/2026", true],
      ["15/01/2026", "", true],
      ["", "", true],
      ["31/12/2025", "01/01/2026", true],
      ["not a date", "14/01/2026", false],
    ])("checks that %s is not after %s", (from, to, valid) => {
      expect(isDateRangeValid(from, to)).toBe(valid);
    });
  });
});
