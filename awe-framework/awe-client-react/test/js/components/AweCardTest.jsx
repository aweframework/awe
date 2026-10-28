import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
// The list of components first, as the application loads it (the card imports the structure that imports the list)
import {ComponentList} from "../../../src/components/AweComponents";
import AweCard from "../../../src/components/AweCard";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {updateModel} from "../../../src/redux/actions/components";

const state = (attributes = {}, values = []) => ({
  settings: DEFAULT_SETTINGS,
  components: {
    CrdCustomers: {
      address: {component: "CrdCustomers", view: "base"},
      model: {values},
      attributes: {goodTrend: "up", ...attributes}
    }
  }
});

const renderCard = (attributes, values, elementList = []) =>
  renderWithProviders(<AweCard id="CrdCustomers" elementList={elementList}/>, {preloadedState: state(attributes, values)});

const card = () => document.querySelector("[data-testid='card']");
const part = selector => card().querySelector(selector);
const delta = () => part("[data-testid='card-delta']");

describe("awe-react-client/test/js/components/AweCardTest.jsx", () => {

  it("is registered as the Card component", () => {
    expect(ComponentList.Card).toBe(AweCard);
  });

  it("renders the tile of a metric with its test hooks", () => {
    renderCard({icon: "users"}, [{title: "Active customers", value: 1284, delta: 4.2, caption: "vs. last month"}]);

    expect(card().tagName).toBe("SECTION");
    expect(card()).toHaveClass("awe-card");
    expect(card()).toHaveAttribute("id", "CrdCustomers");
    expect(card()).toHaveAttribute("data-testid-owner", "CrdCustomers");
    expect(card()).toHaveAttribute("aria-labelledby", "CrdCustomers-title");
    expect(part(".awe-card-header .awe-card-heading h3.awe-card-title")).toHaveAttribute("id", "CrdCustomers-title");
    expect(part(".awe-card-title")).toHaveTextContent(/^Active customers$/);
    expect(part("[data-testid='card-value']")).toHaveClass("awe-card-value");
    expect(part(".awe-card-value .awe-card-number")).toHaveTextContent(/^1,284$/);
    expect(part(".awe-card-unit")).toBeNull();
    expect(part(".awe-card-subtitle")).toBeNull();
    expect(part(".awe-card-body")).toBeNull();
    expect(part("[data-testid='card-progress']")).toBeNull();
    expect(part(".awe-card-description")).toBeNull();
  });

  it("takes the fields of the first row over the attributes", () => {
    renderCard({title: "Static title", value: "1", unit: "kg", subtitle: "Static subtitle"},
      [{value: 2500, unit: "€", subtitle: null}, {value: 9}]);

    expect(part(".awe-card-title")).toHaveTextContent(/^Static title$/);
    expect(part(".awe-card-number")).toHaveTextContent(/^2,500$/);
    expect(part(".awe-card-unit")).toHaveTextContent(/^€$/);
    expect(part(".awe-card-subtitle")).toHaveTextContent(/^Static subtitle$/);
  });

  it("uses the label as title when there is no title", () => {
    renderCard({label: "Open orders", value: 342});

    expect(part(".awe-card-title")).toHaveTextContent(/^Open orders$/);
  });

  it("formats the value with the decimals given and shows a text value as is", () => {
    renderCard({value: "412600", numberDecimals: 2});
    expect(part(".awe-card-number")).toHaveTextContent(/^412,600\.00$/);
  });

  it("applies the decimals given to the value and the target, never to the delta", () => {
    renderCard({value: 1284, delta: 4.2, target: 2000, numberDecimals: 0});
    expect(part(".awe-card-number")).toHaveTextContent(/^1,284$/);
    expect(part(".awe-card-delta-text")).toHaveTextContent(/^\+4\.2%$/);
    expect(part(".awe-card-progress-text")).toHaveTextContent(/2,000$/);
  });

  it("shows a value that is not a number as is", () => {
    renderCard({value: "N/A"});
    expect(part(".awe-card-number")).toHaveTextContent(/^N\/A$/);
  });

  it.each([
    [-4.2, "down", "danger", "↘", "−4.2%", "SCREEN_TEXT_CARD_TREND_DOWN"],
    [0, "flat", "neutral", "→", "0%", "SCREEN_TEXT_CARD_TREND_FLAT"],
    [4.2, "up", "success", "↗", "+4.2%", "SCREEN_TEXT_CARD_TREND_UP"]
  ])("derives the trend of a delta of %s from its sign", (value, trend, tone, arrow, text, label) => {
    renderCard({title: "Sales", delta: value});

    expect(delta()).toHaveClass("awe-card-delta", `awe-card-delta-${tone}`);
    expect(delta()).toHaveAttribute("data-trend", trend);
    expect(delta()).toHaveAttribute("data-tone", tone);
    expect(delta().querySelector(".awe-card-delta-arrow")).toHaveAttribute("aria-hidden", "true");
    expect(delta().querySelector(".awe-card-delta-arrow")).toHaveTextContent(arrow);
    expect(delta().querySelector(".awe-card-delta-text")).toHaveTextContent(text);
    expect(delta().querySelector(".sr-only")).toHaveTextContent(label);
  });

  it("shows the delta without sign when the trend is explicit", () => {
    renderCard({title: "Sales", delta: "-4.2", trend: "up"});

    expect(delta()).toHaveAttribute("data-trend", "up");
    expect(delta()).toHaveAttribute("data-tone", "success");
    expect(delta().querySelector(".awe-card-delta-text")).toHaveTextContent(/^4\.2%$/);
  });

  it("writes a delta unit that is not a percent after a space", () => {
    renderCard({title: "On-time", delta: 2.4, deltaUnit: "pt"});

    expect(delta().querySelector(".awe-card-delta-text")).toHaveTextContent(/^\+2\.4 pt$/);
  });

  it("renders no delta without a numeric delta", () => {
    renderCard({title: "Sales", value: 3, trend: "up"});

    expect(delta()).toBeNull();
  });

  it("takes the tone given over the trend", () => {
    renderCard({title: "Sales", delta: 4.2, tone: "warning"});

    expect(delta()).toHaveAttribute("data-tone", "warning");
    expect(delta()).toHaveClass("awe-card-delta-warning");
  });

  it("flips the tone when the good trend is down", () => {
    renderCard({title: "Error rate", delta: 4.2, goodTrend: "down"});

    expect(delta()).toHaveAttribute("data-trend", "up");
    expect(delta()).toHaveAttribute("data-tone", "danger");
  });

  it("does not let the sign of the value change the tone", () => {
    renderCard({title: "Balance", value: -1200, delta: 3});

    expect(delta()).toHaveAttribute("data-tone", "success");
    expect(part(".awe-card-number")).toHaveTextContent(/^-1,200$/);
  });

  it("puts the icon in the header and the delta and caption in the footer when there is an icon", () => {
    renderCard({title: "Customers", icon: "users", iconTone: "warning", delta: 4.2, caption: "vs. last month", footer: "Updated today"});

    const icon = part(".awe-card-header > .awe-card-icon");
    expect(icon).toHaveClass("awe-card-icon-warning");
    expect(icon).toHaveAttribute("aria-hidden", "true");
    expect(icon.firstChild).not.toBeNull();
    expect(part(".awe-card-header [data-testid='card-delta']")).toBeNull();
    const end = part("footer.awe-card-footer > .awe-card-footer-end");
    expect(end.firstElementChild).toBe(delta());
    expect(end.lastElementChild).toHaveClass("awe-card-caption");
    expect(end.lastElementChild).toHaveTextContent(/^vs\. last month$/);
    expect(part(".awe-card-footer-text")).toHaveTextContent(/^Updated today$/);
  });

  it("colours the icon tile with the primary tone by default", () => {
    renderCard({title: "Customers", icon: "users"});

    expect(part(".awe-card-icon")).toHaveClass("awe-card-icon-primary");
    expect(part("footer.awe-card-footer")).toBeNull();
  });

  it("moves the delta to the header when there is no icon, the caption staying in the footer", () => {
    renderCard({title: "Customers", delta: 4.2, caption: "vs. last month"});

    expect(part(".awe-card-header > [data-testid='card-delta']")).toBe(delta());
    expect(part(".awe-card-icon")).toBeNull();
    const end = part(".awe-card-footer-end");
    expect(end.children).toHaveLength(1);
    expect(end.firstElementChild).toHaveClass("awe-card-caption");
    expect(part(".awe-card-footer-text")).toBeNull();
  });

  it("translates the texts", () => {
    renderCard({title: "SCREEN_TEXT_CARD_TITLE", subtitle: "Monthly", description: "Customers with orders", footer: "Footer"});

    expect(part(".awe-card-title")).toHaveTextContent(/^SCREEN_TEXT_CARD_TITLE$/);
    expect(part(".awe-card-subtitle")).toHaveTextContent(/^Monthly$/);
    expect(part(".awe-card-description")).toHaveTextContent(/^Customers with orders$/);
  });

  it("colours the progress with the icon tone, the primary colour by default", () => {
    renderCard({value: 410000, target: 500000, icon: "euro", iconTone: "success"});
    expect(part("[data-testid='card-progress']")).toHaveClass("awe-card-progress-success");
  });

  it("colours the progress with the primary colour without icon tone", () => {
    renderCard({value: 410000, target: 500000});
    expect(part("[data-testid='card-progress']")).toHaveClass("awe-card-progress-primary");
  });

  it("draws the progress towards the target", () => {
    renderCard({title: "Revenue", value: 410000, unit: "€", target: "500000"});

    const progress = part("[data-testid='card-progress']");
    expect(progress).toHaveClass("awe-card-progress");
    expect(progress).toHaveAttribute("role", "progressbar");
    expect(progress).toHaveAttribute("aria-valuemin", "0");
    expect(progress).toHaveAttribute("aria-valuemax", "100");
    expect(progress).toHaveAttribute("aria-valuenow", "82");
    expect(progress.querySelector(".awe-card-progress-bar")).toHaveStyle({width: "82%"});
    expect(part(".awe-card-progress-text")).toHaveTextContent(/^82 % SCREEN_TEXT_CARD_TARGET_OF 500,000 €$/);
  });

  it("clamps the bar of a target exceeded", () => {
    renderCard({title: "Revenue", value: 150, target: 100});

    const progress = part("[data-testid='card-progress']");
    expect(progress).toHaveAttribute("aria-valuenow", "100");
    expect(progress.querySelector(".awe-card-progress-bar")).toHaveStyle({width: "100%"});
    expect(part(".awe-card-progress-text")).toHaveTextContent(/^150 % SCREEN_TEXT_CARD_TARGET_OF 100$/);
  });

  it.each([[undefined], [0], ["abc"]])("draws no progress with a target %s", target => {
    renderCard({title: "Revenue", value: 150, target});

    expect(part("[data-testid='card-progress']")).toBeNull();
    expect(part(".awe-card-progress-text")).toBeNull();
  });

  it("renders the children in the body, below the value and above the progress", () => {
    renderCard({title: "Sales", value: 10, target: 20}, [],
      [{elementType: "Tag", type: "p", id: "CardChild", label: "Child content", elementList: []}]);

    const body = part(".awe-card-body");
    expect(body.querySelector("#CardChild")).toHaveTextContent(/^Child content$/);
    expect(body.previousElementSibling).toHaveClass("awe-card-value");
    expect(body.nextElementSibling).toHaveClass("awe-card-progress");
  });

  it("shows only the body when there is no value", () => {
    renderCard({title: "Sales"}, [], [{elementType: "Tag", type: "p", id: "CardChild", label: "Child", elementList: []}]);

    expect(part("[data-testid='card-value']")).toBeNull();
    expect(part(".awe-card-body")).not.toBeNull();
  });

  it("adds the style of the card and hides it when it is not visible", () => {
    renderCard({title: "Sales", style: "my-card", visible: false});

    expect(card()).toHaveClass("awe-card", "my-card", "hidden");
  });

  it("hides it when the attribute visible is the text false", () => {
    renderCard({title: "Sales", visible: "false"});

    expect(card()).toHaveClass("hidden");
  });

  it("shows it by default", () => {
    renderCard({title: "Sales"});

    expect(card()).not.toHaveClass("hidden");
  });

  it("renders again when the values of the model change (reload)", () => {
    const {store} = renderCard({title: "Sales"}, [{value: 10, delta: 2}]);
    expect(part(".awe-card-number")).toHaveTextContent(/^10$/);

    act(() => store.dispatch(updateModel({view: "base", component: "CrdCustomers"}, {values: [{value: 25, delta: -3}]})));

    expect(part(".awe-card-number")).toHaveTextContent(/^25$/);
    expect(delta()).toHaveAttribute("data-trend", "down");
  });
  const sparkline = () => part("[data-testid='card-sparkline']");
  const gauge = () => part("[data-testid='card-progress']");

  it("draws the sparkline of the attribute text right after the value and before the body", () => {
    renderCard({title: "Uptime", value: 99.2, sparkline: "[93.1,93.8,92.6]"}, [],
      [{elementType: "Tag", type: "p", id: "CardChild", label: "Child", elementList: []}]);

    expect(sparkline()).toHaveClass("awe-card-sparkline", "awe-card-sparkline-primary");
    expect(sparkline().previousElementSibling).toHaveClass("awe-card-value");
    expect(sparkline().nextElementSibling).toHaveClass("awe-card-body");
    const svg = sparkline().querySelector("svg.awe-sparkline");
    expect(svg).toHaveAttribute("viewBox", "0 0 100 24");
    expect(svg).toHaveAttribute("preserveAspectRatio", "none");
    expect(svg).toHaveAttribute("role", "img");
    expect(svg).toHaveAttribute("aria-label", "93.1, 93.8, 92.6");
    expect(svg).toHaveAttribute("focusable", "false");
    expect(svg).toHaveAttribute("data-type", "area");
    expect(svg.querySelector("path[stroke='currentColor']")).not.toBeNull();
  });

  it("takes the sparkline array of the row over the attribute", () => {
    renderCard({sparkline: "[1,2,3]"}, [{value: 5, sparkline: [4, 5, 6, 7]}]);

    expect(sparkline().querySelector("svg")).toHaveAttribute("aria-label", "4, 5, 6, 7");
  });

  it.each(["bar", "line"])("draws the sparkline type %s", type => {
    renderCard({value: 5, sparkline: "[1,2,3]", sparklineType: type});

    expect(sparkline().querySelector("svg")).toHaveAttribute("data-type", type);
  });

  it("colours the sparkline with the icon tone", () => {
    renderCard({value: 5, icon: "users", iconTone: "success", sparkline: "[1,2,3]"});

    expect(sparkline()).toHaveClass("awe-card-sparkline-success");
  });

  it.each([["[]"], ["not json"], ["[1,\"a\"]"], [undefined]])("draws no sparkline with the series %s", series => {
    renderCard({value: 5, sparkline: series});

    expect(sparkline()).toBeNull();
    expect(part(".awe-card-sparkline")).toBeNull();
  });

  it("draws a gauge instead of the bar with the gauge progress style", () => {
    renderCard({value: 410000, unit: "€", target: 500000, progressStyle: "gauge", iconTone: "warning"});

    expect(gauge()).toHaveClass("awe-card-progress", "awe-card-gauge", "awe-card-progress-warning");
    expect(gauge()).toHaveAttribute("role", "progressbar");
    expect(gauge()).toHaveAttribute("aria-valuemin", "0");
    expect(gauge()).toHaveAttribute("aria-valuemax", "100");
    expect(gauge()).toHaveAttribute("aria-valuenow", "82");
    expect(gauge().querySelector(".awe-card-progress-bar")).toBeNull();
    const arc = gauge().querySelector("svg.awe-card-gauge-arc");
    expect(arc).toHaveAttribute("viewBox", "0 0 100 56");
    expect(arc).toHaveAttribute("aria-hidden", "true");
    expect(arc).toHaveAttribute("focusable", "false");
    const track = arc.querySelector("path.awe-card-gauge-track");
    expect(track).toHaveAttribute("d", "M 8 50 A 42 42 0 0 1 92 50");
    expect(track).toHaveAttribute("pathLength", "100");
    const bar = arc.querySelector("path.awe-card-gauge-bar");
    expect(bar).toHaveAttribute("d", "M 8 50 A 42 42 0 0 1 92 50");
    expect(bar).toHaveAttribute("pathLength", "100");
    expect(bar).toHaveAttribute("stroke-dasharray", "82 100");
    expect(track.nextElementSibling).toBe(bar);
    expect(arc.nextElementSibling).toHaveClass("awe-card-gauge-value");
    expect(gauge().querySelector(".awe-card-gauge-value")).toHaveTextContent(/^82 %$/);
    expect(gauge().nextElementSibling).toHaveClass("awe-card-progress-text");
    // The percent is already in the middle of the arc: the text below only tells the target
    expect(part(".awe-card-progress-text")).toHaveTextContent(/^SCREEN_TEXT_CARD_TARGET_OF 500,000 €$/);
  });

  it("clamps the gauge of a target exceeded, the percent staying as it is", () => {
    renderCard({value: 150, target: 100}, [{progressStyle: "gauge"}]);

    expect(gauge()).toHaveAttribute("aria-valuenow", "100");
    expect(gauge().querySelector(".awe-card-gauge-bar")).toHaveAttribute("stroke-dasharray", "100 100");
    expect(gauge().querySelector(".awe-card-gauge-value")).toHaveTextContent(/^150 %$/);
    expect(gauge()).toHaveClass("awe-card-progress-primary");
  });

  it.each([[undefined], ["bar"]])("keeps the bar with the progress style %s", progressStyle => {
    renderCard({value: 410000, target: 500000, progressStyle});

    expect(gauge()).not.toHaveClass("awe-card-gauge");
    expect(gauge().querySelector(".awe-card-progress-bar")).not.toBeNull();
    expect(part(".awe-card-gauge-arc")).toBeNull();
  });

  it("draws neither gauge nor bar without a target", () => {
    renderCard({value: 410000, progressStyle: "gauge"});

    expect(gauge()).toBeNull();
    expect(part(".awe-card-gauge")).toBeNull();
    expect(part(".awe-card-progress-text")).toBeNull();
  });
});
