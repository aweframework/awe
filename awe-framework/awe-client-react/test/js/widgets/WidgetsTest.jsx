import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import Widgets, {registerWidget, getWidget} from "../../../src/widgets";

describe('awe-react-client/test/js/widgets/WidgetsTest.js', () => {

  it('renders a nonexistent widget', async () => {
    render(Widgets({type: "otro"}, 0));

    await waitFor(() => screen.findByText("The widget otro has not been created yet."));
    expect(await screen.findByText("The widget otro has not been created yet.")).toBeDefined();
  });

  it('renders a custom widget registered at runtime', async () => {
    const CustomWidget = ({id}) => <div>Custom widget {id}</div>;
    registerWidget("custom-widget", CustomWidget);

    render(Widgets({type: "custom-widget", id: "myWidget"}, 0));

    await waitFor(() => screen.findByText("Custom widget myWidget"));
    expect(await screen.findByText("Custom widget myWidget")).toBeDefined();
  });

  it('retrieves a registered widget by type', () => {
    const AnotherWidget = () => <div>Another</div>;
    registerWidget("another-widget", AnotherWidget);

    expect(getWidget("another-widget")).toBe(AnotherWidget);
    expect(getWidget("missing-widget")).toBeUndefined();
  });

  it('throws when registering a widget type that already exists', () => {
    expect(() => registerWidget("carousel", () => <div/>))
      .toThrow('Widget with type "carousel" already exists.');
  });

  it('registers built-in widgets through the registry', () => {
    ["file-manager", "log-viewer", "help-viewer", "pdf-viewer", "carousel"]
      .forEach((type) => expect(getWidget(type)).toBeDefined());
  });
});
