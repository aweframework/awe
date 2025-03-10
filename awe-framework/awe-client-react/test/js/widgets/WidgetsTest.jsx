import React from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import Widgets from "../../../src/widgets";

describe('awe-react-client/test/js/widgets/WidgetsTest.js', () => {

  it('renders a nonexistent widget', async () => {
    render(Widgets({type: "otro"}, 0));

    await waitFor(() => screen.findByText("The widget otro has not been created yet."));
    expect(screen.findByText("The widget otro has not been created yet.")).toBeDefined();
  });
});
