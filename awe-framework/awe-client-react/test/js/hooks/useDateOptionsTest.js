import {renderHook} from "@testing-library/react";
import useDateOptions from "../../../src/hooks/useDateOptions";

describe("awe-react-client/test/js/hooks/useDateOptions.js", () => {
  const limit = (value, options = {}) => renderHook(() =>
    useDateOptions({showFutureDates: false, ...options}, value, "en")).result.current.calendarProps.maxDate;

  it("limits the dates to the value of the criterion when the future dates are hidden", () => {
    expect(limit("15/03/2024")).toEqual(new Date(2024, 2, 15));
  });

  it("sets no limit when the value is empty", () => {
    expect(limit("")).toBeUndefined();
  });

  it("sets no limit when the value is not a date, never an invalid date", () => {
    expect(limit("abc")).toBeUndefined();
    expect(limit("2024-03-15")).toBeUndefined();
    expect(limit("2024-03-15", {dateFormat: "dd/mm/yyyy"})).toBeUndefined();
  });

  it("sets no limit when the future dates are shown", () => {
    expect(limit("15/03/2024", {showFutureDates: true})).toBeUndefined();
  });
});
