/**
 * @jest-environment node
 */
import models from "../components/fixtures/chrTstEchartsModels.json";
import {createChartServer} from "../../../src/renderer/chartServer";
// Requests sent by the Java ChartService (awe-boot ChrTst screen), captured from its debug log
import chartServiceRequests from "./fixtures/chartServiceRequests.json";

const values = [{names: "First", serie1: 1}, {names: "Second", serie1: 2}];

describe('awe-react-client/test/js/renderer/chartServerTest.js', () => {
  let server;
  let url;
  let error;

  const start = async (options) => {
    server = createChartServer(options);
    await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
    url = `http://127.0.0.1:${server.address().port}`;
  };

  const post = (body, path = "/") => fetch(`${url}${path}`, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: typeof body === "string" ? body : JSON.stringify(body)
  });

  beforeEach(async () => {
    error = jest.spyOn(console, "error").mockImplementation(() => {});
    await start();
  });

  afterEach(async () => {
    error.mockRestore();
    await new Promise(resolve => server.close(resolve));
  });

  it('should answer the export server request with the SVG image of the chart', async () => {
    const response = await post({type: "image/svg+xml", infile: models.ChrPieTst, values, width: 500, height: 250});

    expect(response.status).toBe(200);
    expect(response.headers.get("content-type")).toBe("image/svg+xml; charset=utf-8");
    const svg = await response.text();
    expect(svg).toMatch(/^<svg/);
    expect(svg).toContain("width=\"500\"");
    expect(svg).toContain("Second");
  });

  it.each(Object.keys(chartServiceRequests))('should render the %s request of ChartService', async (name) => {
    const response = await post(chartServiceRequests[name]);

    expect(response.status).toBe(200);
    const svg = await response.text();
    expect(svg).toMatch(/^<svg/);
    expect(svg).toContain("width=\"600\"");
    expect(error).not.toHaveBeenCalled();
  });

  it('should translate the locale keys that ChartService sends', async () => {
    const svg = await (await post(chartServiceRequests.ChrLinTst)).text();

    expect(svg).toContain("Chart 1");
    expect(svg).not.toContain("SCREEN_TEXT_CHART_TITLE_1");
  });

  it('should render SVG when the request does not give the type', async () => {
    const response = await post({infile: models.ChrPieTst, values});

    expect(response.status).toBe(200);
    expect(await response.text()).toMatch(/^<svg/);
  });

  it.each(["image/png", "application/pdf", "image/jpeg"])('should reject the type %s', async (type) => {
    const response = await post({type, infile: models.ChrPieTst, values});

    expect(response.status).toBe(400);
    expect(await response.text()).toBe(`Unsupported type '${type}': the chart server only renders image/svg+xml`);
  });

  it('should reject a request without chart', async () => {
    const response = await post({type: "image/svg+xml", values});

    expect(response.status).toBe(400);
    expect(await response.text()).toBe("The request has no chart (infile)");
  });

  it('should reject a body that is not JSON', async () => {
    const response = await post("not json");

    expect(response.status).toBe(400);
    expect(await response.text()).toBe("The request body is not valid JSON");
  });

  it.each([[0, 400], [-5, 400], [500, "high"], [20001, 400]])('should reject the size %s x %s', async (width, height) => {
    const response = await post({infile: models.ChrPieTst, values, width, height});

    expect(response.status).toBe(400);
    expect(await response.text()).toBe("The width and the height must be numbers between 1 and 20000");
  });

  it('should reject a body larger than the limit', async () => {
    await new Promise(resolve => server.close(resolve));
    await start({maxBodySize: 100});

    const response = await post({infile: models.ChrPieTst, values});

    expect(response.status).toBe(413);
    expect(await response.text()).toBe("The request body is larger than 100 bytes");
  });

  it('should answer a server error when the chart cannot be drawn', async () => {
    const response = await post({infile: {series: "not a list", awe: {inverted: true}, xAxis: [null]}, values});

    expect(response.status).toBe(500);
    expect(await response.text()).toMatch(/^The chart could not be rendered: /);
    expect(error).toHaveBeenCalled();
  });

  it('should answer the health check', async () => {
    const response = await fetch(`${url}/health`);

    expect(response.status).toBe(200);
    expect(await response.text()).toBe("OK");
  });

  it.each([["GET", "/"], ["POST", "/other"], ["PUT", "/"]])('should not find %s %s', async (method, path) => {
    const response = await fetch(`${url}${path}`, {method, body: method === "GET" ? undefined : "{}"});

    expect(response.status).toBe(404);
    expect(await response.text()).toBe("Not found");
  });
});
