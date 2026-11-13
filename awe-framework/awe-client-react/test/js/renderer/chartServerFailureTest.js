/**
 * @jest-environment node
 */
import {createChartServer} from "../../../src/renderer/chartServer";
import {renderChart} from "../../../src/renderer/chartRenderer";

jest.mock("../../../src/renderer/chartRenderer", () => ({renderChart: jest.fn()}));

describe('awe-react-client/test/js/renderer/chartServerFailureTest.js', () => {
  let server;
  let url;
  let error;

  beforeEach(async () => {
    error = jest.spyOn(console, "error").mockImplementation(() => {});
    server = createChartServer();
    await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
    url = `http://127.0.0.1:${server.address().port}`;
  });

  afterEach(async () => {
    error.mockRestore();
    await new Promise(resolve => server.close(resolve));
  });

  it('should answer a server error and keep running when the render throws something that is not an error', async () => {
    renderChart.mockImplementation(() => {
      throw null;
    });
    const post = () => fetch(`${url}/`, {method: "POST", headers: {"Content-Type": "application/json"},
      body: JSON.stringify({infile: {series: []}}), signal: AbortSignal.timeout(2000)});

    const response = await post();

    expect(response.status).toBe(500);
    expect(await response.text()).toBe("The chart could not be rendered: null");
    expect(error).toHaveBeenCalled();
    expect((await fetch(`${url}/health`)).status).toBe(200);
  });
});
