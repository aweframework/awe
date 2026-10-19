import {
  formatDate, fromDate, getAvailableDates, getDisabledDates, getViewMode, isDateRangeValid, parsePresets,
  resolvePreset, toCalendarFormat, toDate
} from "../../../src/utilities/dates";

const day = (date) => `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;

describe("awe-react-client/test/js/utilities/datesTest.js", () => {
  describe("getAvailableDates", () => {
    it("ignores the values that are not dates", () => {
      expect(getAvailableDates([{value: "test"}, {value: "15/03/2023"}]).map(day)).toEqual(["2023-3-15"]);
    });

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

  describe("date-format of the contract (bootstrap-datepicker format)", () => {
    it("converts the AWE tokens to the tokens of the calendar", () => {
      expect(toCalendarFormat()).toBe("dd/mm/yy");
      expect(toCalendarFormat("dd/mm/yyyy")).toBe("dd/mm/yy");
      expect(toCalendarFormat("yyyy-mm-dd")).toBe("yy-mm-dd");
      expect(toCalendarFormat("dd/mm/yy")).toBe("dd/mm/y");
      expect(toCalendarFormat("mm/yyyy")).toBe("mm/yy");
      expect(toCalendarFormat("d M, yyyy")).toBe("d M, yy");
    });

    it("quotes the literal letters of the format", () => {
      expect(toCalendarFormat("dd h mm")).toBe("dd 'h' mm");
      expect(toCalendarFormat("dd-mm-yyyy h")).toBe("dd-mm-yy 'h'");
    });

    it("parses a value with the format", () => {
      expect(day(toDate("2024-03-15", "yyyy-mm-dd"))).toBe("2024-3-15");
      expect(day(toDate("5/3/24", "d/m/yy"))).toBe("2024-3-5");
      expect(day(toDate("03/2024", "mm/yyyy"))).toBe("2024-3-1");
      expect(day(toDate("2024", "yyyy"))).toBe("2024-1-1");
    });

    it("keeps the default format when there is none", () => {
      expect(day(toDate("15/03/2024"))).toBe("2024-3-15");
      expect(day(toDate("15/03/2024", null))).toBe("2024-3-15");
      expect(day(toDate("15/03/2024", "dd/mm/yyyy"))).toBe("2024-3-15");
    });

    it("returns no date for a value that does not match the format", () => {
      expect(toDate("15/03/2024", "yyyy-mm-dd")).toBeNull();
      expect(toDate("", "yyyy-mm-dd")).toBeNull();
      expect(toDate("2024-13-45", "yyyy-mm-dd")).toBeNull();
    });

    it("returns no date for a value that is not a date in the default format", () => {
      expect(toDate("abc")).toBeNull();
      expect(toDate("abc", "dd/mm/yyyy")).toBeNull();
      expect(toDate("2024-03-15")).toBeNull();
      expect(toDate("31/02/2024")).toBeNull();
    });

    it("falls back to the default language when the language is not valid", () => {
      expect(fromDate(new Date(2024, 2, 5), "d MM yyyy", "not_a_language!")).toBe("5 March 2024");
      expect(day(toDate("5 March 2024", "d MM yyyy", "xx-INVALID-tag-"))).toBe("2024-3-5");
    });

    it("formats the date with the format, which is the value sent to the server", () => {
      const date = new Date(2024, 2, 5);
      expect(fromDate(date)).toBe("05/03/2024");
      expect(fromDate(date, "dd/mm/yyyy")).toBe("05/03/2024");
      expect(fromDate(date, "yyyy-mm-dd")).toBe("2024-03-05");
      expect(fromDate(date, "d/m/yy")).toBe("5/3/24");
      expect(fromDate(date, "mm/yyyy")).toBe("03/2024");
      expect(fromDate(date, "yyyy")).toBe("2024");
      expect(fromDate(null, "yyyy")).toBeNull();
    });

    it("formats and parses month and day names with the language", () => {
      const date = new Date(2024, 2, 5);
      expect(fromDate(date, "d MM yyyy", "en")).toBe("5 March 2024");
      expect(fromDate(date, "d M yyyy", "es")).toBe("5 Mar 2024");
      expect(fromDate(date, "DD d MM", "en")).toBe("Tuesday 5 March");
      expect(day(toDate("5 March 2024", "d MM yyyy", "en"))).toBe("2024-3-5");
      expect(day(toDate("5 marzo 2024", "d MM yyyy", "es"))).toBe("2024-3-5");
      expect(toDate("5 Smarch 2024", "d MM yyyy", "en")).toBeNull();
    });

    it("maps the view mode of the contract to the view of the calendar", () => {
      expect(getViewMode()).toBe("date");
      expect(getViewMode("days")).toBe("date");
      expect(getViewMode("months")).toBe("month");
      expect(getViewMode("years")).toBe("year");
      expect(getViewMode(1)).toBe("month");
      expect(getViewMode("2")).toBe("year");
      expect(getViewMode("other")).toBe("date");
    });
  });
});
