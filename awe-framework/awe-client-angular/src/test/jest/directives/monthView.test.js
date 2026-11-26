import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {getMonthViewNames} from "awe-client-react/utilities/monthView";

const pad = number => String(number).padStart(2, "0");
const text = date => `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
const daysFromToday = days => {
  const today = new Date();
  return new Date(today.getFullYear(), today.getMonth(), today.getDate() + days);
};
// A saved date in the past and the next date from today, added to the grid and not saved yet (RowTyp)
const PAST = daysFromToday(-400);
const NEXT = daysFromToday(40);
const ROWS = [
  {id: 1, Dat: text(PAST), Nam: "Saved holiday"},
  {id: 2, Dat: text(NEXT), Nam: "New holiday", RowTyp: "INSERT"},
  {id: 3, Dat: text(daysFromToday(41)), Nam: "Removed holiday", RowTyp: "DELETE"}
];

describe("awe-client-angular/src/test/jest/directives/monthView.test.js", () => {
  let $compile;
  let $rootScope;
  let gridModel;
  let control;
  let names;

  beforeEach(() => {
    gridModel = {values: ROWS.map(row => ({...row})), selected: null};
    control = {
      getAddressModel: jest.fn(address => (address.component === "GrdDat" ? gridModel : {selected: null, values: []}))
    };
    const controller = {visible: true, parameters: {grid: "GrdDat", dateColumn: "Dat", nameColumn: "Nam"}};

    function ComponentMock(scope, id) {
      const component = {
        address: {view: "report", component: id}, api: {}, controller, id, listeners: {}, model: {values: []}, scope,
        asComponent: jest.fn(() => true)
      };
      scope.component = component;
      scope.controller = controller;
      return component;
    }

    angular.mock.module("aweApplication", {Component: ComponentMock, Control: control});
    inject(["$compile", "$httpBackend", "$rootScope", "AweSettings", (_$compile_, $httpBackend, _$rootScope_, $settings) => {
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $httpBackend.when("POST", "settings").respond(DefaultSettings);
      names = getMonthViewNames($settings.getLanguage());
    }]);
  });

  /**
   * Compile the directive as a screen does (the server renders the month-view tag)
   * @returns {Element} Rendered month view
   */
  function compileMonthView() {
    const element = $compile('<awe-month-view month-view-id="CalMth"></awe-month-view>')($rootScope.$new());
    $rootScope.$digest();
    return element[0];
  }

  const title = date => `${names.months[date.getMonth()].long} ${date.getFullYear()}`;
  const shownTitle = root => root.querySelector(`#${root.getAttribute("aria-labelledby")}`).textContent;
  const struckDays = root => [...root.querySelectorAll("td[aria-label]")].map(cell => cell.getAttribute("aria-label"));
  const button = (root, label) => root.querySelector(`button[aria-label="${label}"]`);

  it("opens on the month of the next date and strikes it through, also when the row is not saved yet", () => {
    const root = compileMonthView();

    expect(shownTitle(root)).toBe(title(NEXT));
    expect(struckDays(root)).toEqual([`${NEXT.getDate()}, MONTH_VIEW_EXCLUDED: New holiday (MONTH_VIEW_UNSAVED)`]);
    expect(root.querySelector("td[aria-label]").getAttribute("title")).toBe("New holiday (MONTH_VIEW_UNSAVED)");
    expect(root.textContent).toContain("MONTH_VIEW_LEGEND");
  });

  it("opens on the month of today when no date comes after today", () => {
    gridModel.values = [ROWS[0]];
    const root = compileMonthView();

    expect(shownTitle(root)).toBe(title(new Date()));
  });

  it("moves to the previous and to the next month with its buttons", () => {
    const root = compileMonthView();
    const shown = new Date(NEXT.getFullYear(), NEXT.getMonth(), 1);

    button(root, "MONTH_VIEW_NEXT").click();
    $rootScope.$digest();
    expect(shownTitle(root)).toBe(title(new Date(shown.getFullYear(), shown.getMonth() + 1, 1)));

    button(root, "MONTH_VIEW_PREVIOUS").click();
    button(root, "MONTH_VIEW_PREVIOUS").click();
    $rootScope.$digest();
    expect(shownTitle(root)).toBe(title(new Date(shown.getFullYear(), shown.getMonth() - 1, 1)));
  });

  it("shows the month clicked in the strip, which counts the dates of every month of the year", () => {
    const root = compileMonthView();
    const buttons = () => [...root.querySelectorAll('[aria-label="MONTH_VIEW_STRIP"] button')];

    expect(buttons()).toHaveLength(12);
    expect(buttons()[NEXT.getMonth()].getAttribute("aria-pressed")).toBe("true");
    expect(buttons()[NEXT.getMonth()].textContent.replace(/\s/g, "")).toBe(`${names.months[NEXT.getMonth()].short}1`);

    const other = (NEXT.getMonth() + 6) % 12;
    buttons()[other].click();
    $rootScope.$digest();

    expect(shownTitle(root)).toBe(`${names.months[other].long} ${NEXT.getFullYear()}`);
    expect(buttons()[other].getAttribute("aria-pressed")).toBe("true");
  });

  it("draws again when the rows of the grid change, as a cell edited in the grid does", () => {
    gridModel.values = [];
    const root = compileMonthView();
    expect(struckDays(root)).toEqual([]);

    gridModel.values.push({id: 4, Dat: "", Nam: "Edited holiday", RowTyp: "INSERT"});
    gridModel.values[0].Dat = text(NEXT);
    $rootScope.$digest();

    expect(shownTitle(root)).toBe(title(NEXT));
    expect(struckDays(root)).toEqual([`${NEXT.getDate()}, MONTH_VIEW_EXCLUDED: Edited holiday (MONTH_VIEW_UNSAVED)`]);
  });
});
