import React from "react";
import { act, fireEvent, screen } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { updateModel } from "../../../src/redux/actions/components";
import { getMonthViewNames } from "../../../src/utilities/monthView";
import { renderWithProviders } from "../test-utils";
import AweMonthView from "../../../src/widgets/AweMonthView";

const NAMES = getMonthViewNames("es");
const pad = number => String(number).padStart(2, "0");
const text = date => `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
const daysFromToday = days => {
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), today.getDate() + days);
};
const title = date => `${NAMES.months[date.getMonth()].long} ${date.getFullYear()}`;
// A saved date in the past and the next date from today, which is not saved yet
const PAST = daysFromToday(-400);
const NEXT = daysFromToday(40);
const GRID_ROWS = [
  { id: 1, Dat: text(PAST), Nam: "Saved holiday" },
  { id: 2, Dat: text(NEXT), Nam: "New holiday", $row: { operation: "INSERT", editing: true } }
];
const GRID = { address: { component: "GrdDat", view: "report" }, attributes: { id: "GrdDat", component: "grid" } };

const renderMonthView = (values = GRID_ROWS) => renderWithProviders(<AweMonthView id="CalMth" />, {
  preloadedState: {
    settings: { ...DEFAULT_SETTINGS, language: "es" },
    components: {
      GrdDat: { ...GRID, model: { values, selected: [] } },
      CalMth: {
        address: { component: "CalMth", view: "report" },
        model: { values: [] },
        attributes: { id: "CalMth", type: "month-view", parameters: { grid: "GrdDat", dateColumn: "Dat", nameColumn: "Nam" } }
      }
    }
  }
});
// The month is a group named by its title
const shownTitle = () => {
  const month = screen.getByRole("group", { name: name => name !== "MONTH_VIEW_STRIP" });
  return document.getElementById(month.getAttribute("aria-labelledby")).textContent;
};
const struckDays = () => screen.queryAllByRole("cell").filter(cell => cell.getAttribute("aria-label"))
  .map(cell => cell.getAttribute("aria-label"));

describe("awe-client-react/test/js/widgets/AweMonthViewTest.jsx", () => {
  it("opens on the month of the next date and strikes it through, also when the row is not saved yet", () => {
    renderMonthView();

    expect(shownTitle()).toBe(title(NEXT));
    expect(struckDays()).toEqual([`${NEXT.getDate()}, MONTH_VIEW_EXCLUDED: New holiday (MONTH_VIEW_UNSAVED)`]);
    expect(screen.getByText(String(NEXT.getDate()), { selector: "td" }).getAttribute("title"))
      .toBe("New holiday (MONTH_VIEW_UNSAVED)");
    expect(screen.getByText("MONTH_VIEW_LEGEND")).toBeDefined();
  });

  it("opens on the month of today when no date comes after today", () => {
    renderMonthView([GRID_ROWS[0]]);

    expect(shownTitle()).toBe(title(new Date()));
  });

  it("moves to the previous and to the next month with its buttons", () => {
    renderMonthView();
    const shown = new Date(NEXT.getFullYear(), NEXT.getMonth(), 1);

    fireEvent.click(screen.getByRole("button", { name: "MONTH_VIEW_NEXT" }));
    expect(shownTitle()).toBe(title(new Date(shown.getFullYear(), shown.getMonth() + 1, 1)));

    fireEvent.click(screen.getByRole("button", { name: "MONTH_VIEW_PREVIOUS" }));
    fireEvent.click(screen.getByRole("button", { name: "MONTH_VIEW_PREVIOUS" }));
    expect(shownTitle()).toBe(title(new Date(shown.getFullYear(), shown.getMonth() - 1, 1)));
  });

  it("shows the month clicked in the strip, which counts the dates of every month of the year", () => {
    renderMonthView();
    const strip = screen.getByRole("group", { name: "MONTH_VIEW_STRIP" });
    const buttons = Array.from(strip.querySelectorAll("button"));

    expect(buttons).toHaveLength(12);
    expect(buttons[NEXT.getMonth()].getAttribute("aria-pressed")).toBe("true");
    expect(buttons[NEXT.getMonth()].textContent).toBe(`${NAMES.months[NEXT.getMonth()].short}1`);

    const other = (NEXT.getMonth() + 6) % 12;
    fireEvent.click(buttons[other]);

    expect(shownTitle()).toBe(`${NAMES.months[other].long} ${NEXT.getFullYear()}`);
    expect(buttons[other].getAttribute("aria-pressed")).toBe("true");
  });

  it("draws again when the rows of the grid change", () => {
    const { store } = renderMonthView([]);
    expect(struckDays()).toEqual([]);

    act(() => {
      store.dispatch(updateModel(GRID.address, { values: GRID_ROWS }));
    });

    expect(shownTitle()).toBe(title(NEXT));
    expect(struckDays()).toHaveLength(1);
  });
});
