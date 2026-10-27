import "../components/lightComponentTestUtils";

import {uploaderColumnTemplate, uploaderInputTemplate} from "../../../main/resources/js/awe/services/uploader";

// The buttons of the uploader (#885) are AWE buttons, so they take the button tokens in light and dark: a `.btn`
// without `btn-awe` gets the pixel-admin light gradient with the text colour, light on light in dark mode
describe("uploader buttons", () => {
  const buttons = template => {
    const container = document.createElement("div");
    container.innerHTML = template;
    return [...container.querySelectorAll(".pfi-actions button, .pfi-capture")];
  };

  it.each([
    ["criterion", uploaderInputTemplate],
    ["grid column", uploaderColumnTemplate]
  ])("%s uploader buttons are AWE buttons", (name, template) => {
    const found = buttons(template);
    expect(found.length).toBeGreaterThan(0);
    found.forEach(button => expect(button.classList).toContain("btn-awe"));
  });

  it.each([
    ["criterion", uploaderInputTemplate],
    ["grid column", uploaderColumnTemplate]
  ])("%s uploader choose button is the filled primary AWE button", (name, template) => {
    const choose = buttons(template).find(button => button.classList.contains("pfi-choose"));
    expect([...choose.classList]).toEqual(expect.arrayContaining(["btn", "btn-awe", "btn-primary"]));
  });
});
