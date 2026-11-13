import {DATE_FORMAT, formatDate, parseDate} from "../../../main/resources/js/awe/services/dateUtils";

describe("awe-framework/awe-client-angular/src/test/jest/services/dateUtils.js", () => {
  describe("parseDate", () => {
    it.each([
      ["01/02/2020", 2020, 1, 1],
      ["29/02/2020", 2020, 1, 29],
      ["31/12/1999", 1999, 11, 31]
    ])("parses the valid date %s", (value, year, month, day) => {
      const date = parseDate(value, true);
      expect(date.isValid()).toBe(true);
      expect([date.year(), date.month(), date.date()]).toEqual([year, month, day]);
    });

    it.each(["31/02/2020", "29/02/2021", "00/01/2020", "15/13/2020", "32/01/2020", "ab/cd/efgh", "", "1/2/2020"])(
      "rejects %p in strict mode", (value) => {
        expect(parseDate(value, true).isValid()).toBe(false);
      });

    it("accepts unpadded days and months only when it is not strict", () => {
      expect(parseDate("1/2/2020").isValid()).toBe(true);
      expect(parseDate("1/2/2020").format(DATE_FORMAT)).toBe("01/02/2020");
    });

    it.each([
      ["1/2/20", 2020], ["1/2/68", 2068], ["1/2/69", 1969], ["1/2/99", 1999], ["1/2/5", 5], ["1/2/123", 123]
    ])("keeps the lenient years of the former moment parsing: %s", (value, year) => {
      const date = parseDate(value);
      expect(date.isValid()).toBe(true);
      expect(date.year()).toBe(year);
    });

    it.each(["0000", "0001", "0050", "0099"])("keeps the year %s instead of mapping it to the 1900s", (year) => {
      const date = parseDate(`15/06/${year}`, true);
      expect(date.isValid()).toBe(true);
      expect(date.year()).toBe(Number(year));
    });

    it("rejects dates that overflow their month when it is not strict", () => {
      expect(parseDate("31/02/2020").isValid()).toBe(false);
    });

    it("rejects empty values", () => {
      expect([parseDate(null).isValid(), parseDate(undefined).isValid()]).toEqual([false, false]);
    });

    it("returns dates comparable with the relational operators", () => {
      expect(parseDate("01/01/2020") < parseDate("02/01/2020")).toBe(true);
      expect(parseDate("02/01/2020") > parseDate("01/01/2020")).toBe(true);
    });
  });

  describe("formatDate", () => {
    it("formats a javascript date with the given format", () => {
      const date = new Date(2018, 9, 23);
      expect(formatDate(date, "DD/MM/YYYY")).toBe("23/10/2018");
      expect(formatDate(date, "MMYYYY")).toBe("102018");
      expect(formatDate(date, "YYYY")).toBe("2018");
    });
  });
});
