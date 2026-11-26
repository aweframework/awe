import {
  buildMonthView, countDatesByMonth, getInitialMonth, parseMonthViewDate, readMonthDates, shiftMonth
} from "../../../src/utilities/monthView";

// Rows of a grid of dates: the React grid marks its unsaved rows with $row.operation, the AngularJS one with RowTyp
const rows = [
  { id: 1, Dat: "06/12/2026", Nam: "Día de la Constitución" },
  { id: 2, Dat: "08/12/2026", Nam: "Inmaculada Concepción" },
  { id: 3, Dat: "24/12/2026", Nam: "Nochebuena", $row: { operation: "INSERT", editing: true } },
  { id: 4, Dat: "25/12/2026", Nam: "Navidad", RowTyp: "UPDATE" },
  { id: 5, Dat: "01/01/2026", Nam: "Año Nuevo" },
  { id: 6, Dat: "01/11/2026", Nam: "Todos los Santos", $row: { operation: "DELETE" } },
  { id: 7, Dat: "12/10/2026", Nam: "Fiesta Nacional", RowTyp: "DELETE" },
  { id: 8, Dat: "", Nam: "Sin fecha" }
];
const COLUMNS = { dateColumn: "Dat", nameColumn: "Nam" };
const TODAY = new Date(2026, 10, 24);
// The texts of the month view as a language gives them: the month of the strip goes with its count
const translate = key => ({ MONTH_VIEW_MONTH_DATES: "{0}: {1} fechas" })[key] ?? key;
const view = (shown, dates = readMonthDates(rows, COLUMNS)) =>
  buildMonthView({ dates, shown, today: TODAY, language: "es", translate });
const struck = monthView => monthView.weeks.flatMap(week => week.days).filter(day => day.excluded);

describe("awe-client-react/test/js/utilities/monthViewTest.js", () => {
  it("reads the dates of the grid cells in the formats a grid holds them", () => {
    expect(parseMonthViewDate("6/12/2026")).toEqual({ year: 2026, month: 11, day: 6 });
    expect(parseMonthViewDate("2026-12-06")).toEqual({ year: 2026, month: 11, day: 6 });
    expect(parseMonthViewDate("2026-12-06T00:00:00")).toEqual({ year: 2026, month: 11, day: 6 });
    expect(parseMonthViewDate({ value: "06/12/2026", label: "06/12/2026" })).toEqual({ year: 2026, month: 11, day: 6 });
    expect(parseMonthViewDate(new Date(2026, 11, 6))).toEqual({ year: 2026, month: 11, day: 6 });
    expect(parseMonthViewDate("31/02/2026")).toBeNull();
    expect(parseMonthViewDate("")).toBeNull();
    expect(parseMonthViewDate(null)).toBeNull();
  });

  it("takes every row of the grid, the unsaved ones included, but not the deleted ones nor the rows without a date", () => {
    expect(readMonthDates(rows, COLUMNS).map(date => [date.key, date.name, date.unsaved])).toEqual([
      ["2026-01-01", "Año Nuevo", false],
      ["2026-12-06", "Día de la Constitución", false],
      ["2026-12-08", "Inmaculada Concepción", false],
      ["2026-12-24", "Nochebuena", true],
      ["2026-12-25", "Navidad", true]
    ]);
  });

  it("strikes through the dates of the month, telling their state and their name, not only by colour", () => {
    const december = view({ year: 2026, month: 11 });

    expect(struck(december).map(day => [day.day, day.label, day.title])).toEqual([
      [6, "6, MONTH_VIEW_EXCLUDED: Día de la Constitución", "Día de la Constitución"],
      [8, "8, MONTH_VIEW_EXCLUDED: Inmaculada Concepción", "Inmaculada Concepción"],
      [24, "24, MONTH_VIEW_EXCLUDED: Nochebuena (MONTH_VIEW_UNSAVED)", "Nochebuena (MONTH_VIEW_UNSAVED)"],
      [25, "25, MONTH_VIEW_EXCLUDED: Navidad (MONTH_VIEW_UNSAVED)", "Navidad (MONTH_VIEW_UNSAVED)"]
    ]);
  });

  it("tells the state of a struck day without a name column", () => {
    const december = view({ year: 2026, month: 11 }, readMonthDates(rows, { dateColumn: "Dat" }));

    expect(struck(december)[0]).toEqual(expect.objectContaining({ label: "6, MONTH_VIEW_EXCLUDED", title: "MONTH_VIEW_EXCLUDED" }));
  });

  it("starts the weeks on Monday, with the localized names of the month and of the days", () => {
    const december = view({ year: 2026, month: 11 });

    expect(december.title).toBe("Diciembre 2026");
    expect(december.weekdays.map(day => day.narrow)).toEqual(["L", "M", "X", "J", "V", "S", "D"]);
    expect(december.weekdays[0].long).toBe("lunes");
    // 1 December 2026 is a Tuesday: the first week starts with an empty Monday
    expect(december.weeks[0].days.map(day => day.day ?? null)).toEqual([null, 1, 2, 3, 4, 5, 6]);
    expect(december.weeks[0].days[6].weekend).toBe(true);
    expect(december.weeks).toHaveLength(5);
  });

  it("marks today", () => {
    const today = view({ year: 2026, month: 10 }).weeks.flatMap(week => week.days).filter(day => day.today);

    expect(today.map(day => day.day)).toEqual([24]);
  });

  it("opens on the month of the next date from today, else on the month of today", () => {
    const dates = readMonthDates(rows, COLUMNS);

    expect(getInitialMonth(dates, TODAY)).toEqual({ year: 2026, month: 11 });
    expect(getInitialMonth(dates, new Date(2026, 11, 24))).toEqual({ year: 2026, month: 11 });
    expect(getInitialMonth(dates, new Date(2027, 0, 5))).toEqual({ year: 2027, month: 0 });
    expect(getInitialMonth([], TODAY)).toEqual({ year: 2026, month: 10 });
  });

  it("moves to the previous and to the next month across the years", () => {
    expect(shiftMonth({ year: 2026, month: 11 }, 1)).toEqual({ year: 2027, month: 0 });
    expect(shiftMonth({ year: 2026, month: 0 }, -1)).toEqual({ year: 2025, month: 11 });
    expect(shiftMonth({ year: 2026, month: 5 }, -1)).toEqual({ year: 2026, month: 4 });
  });

  it("counts the dates of every month of the year shown, for the month strip", () => {
    const dates = readMonthDates(rows, COLUMNS);

    expect(countDatesByMonth(dates, 2026)).toEqual([1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4]);
    expect(countDatesByMonth(dates, 2027)).toEqual(Array(12).fill(0));

    const strip = view({ year: 2026, month: 11 }).months;
    expect(strip.map(month => month.short)).toHaveLength(12);
    expect([strip[0].short, strip[11].short]).toEqual(["Ene", "Dic"]);
    expect(strip.map(month => month.count)).toEqual([1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4]);
    expect(strip.filter(month => month.pressed).map(month => month.month)).toEqual([11]);
    expect(strip[11].label).toBe("Diciembre 2026: 4 fechas");
  });
});
