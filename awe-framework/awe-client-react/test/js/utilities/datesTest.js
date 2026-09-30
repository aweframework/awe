import {getAvailableDates, getDisabledDates, toDate} from "../../../src/utilities/dates";

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
});
