import http from "node:http";
import {renderChart} from "./chartRenderer";

/**
 * HTTP server of the AWE chart render. It keeps the contract of the Highcharts export server that AWE used before:
 * `POST /` with a JSON body `{type, infile, ...}` answers the image of the chart. Only SVG images are rendered.
 *
 * `GET /health` answers `OK`, for the health checks of the container.
 */

const SVG = "image/svg+xml";
const SVG_TYPES = new Set([SVG, "svg"]);
const MAX_SIZE = 20000;
const DEFAULT_MAX_BODY_SIZE = 10 * 1024 * 1024;

/**
 * Error with the HTTP status to answer
 */
class RequestError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

/**
 * Read the body of a request
 * @param {http.IncomingMessage} request Request
 * @param {number} maxBodySize Maximum size of the body in bytes
 * @returns {Promise<string>} Body
 */
function readBody(request, maxBodySize) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    const collect = (chunk) => {
      size += chunk.length;
      if (size > maxBodySize) {
        // The rest of the body is drained, not kept, so that the client still gets the answer
        request.off("data", collect);
        request.resume();
        reject(new RequestError(413, `The request body is larger than ${maxBodySize} bytes`));
        return;
      }
      chunks.push(chunk);
    };
    request.on("data", collect);
    request.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    request.on("error", reject);
  });
}

const validSize = (value) => value === undefined || (Number.isFinite(value) && value >= 1 && value <= MAX_SIZE);

/**
 * Parse and check a render request
 * @param {string} body Body of the request
 * @returns {object} Render request
 */
function parseRequest(body) {
  let request;
  try {
    request = JSON.parse(body);
  } catch {
    throw new RequestError(400, "The request body is not valid JSON");
  }
  if (!request || typeof request !== "object" || !request.infile || typeof request.infile !== "object") {
    throw new RequestError(400, "The request has no chart (infile)");
  }
  const type = request.type ?? SVG;
  if (!SVG_TYPES.has(type)) {
    throw new RequestError(400, `Unsupported type '${type}': the chart server only renders ${SVG}`);
  }
  if (!validSize(request.width) || !validSize(request.height)) {
    throw new RequestError(400, `The width and the height must be numbers between 1 and ${MAX_SIZE}`);
  }
  return request;
}

/**
 * Answer a text
 * @param {http.ServerResponse} response Response
 * @param {number} status HTTP status
 * @param {string} text Text
 * @param {string} [contentType] Content type
 */
function answer(response, status, text, contentType = "text/plain") {
  response.writeHead(status, {"Content-Type": `${contentType}; charset=utf-8`});
  response.end(text);
}

/**
 * Render the chart of a request
 * @param {http.IncomingMessage} request Request
 * @param {http.ServerResponse} response Response
 * @param {number} maxBodySize Maximum size of the body in bytes
 */
async function render(request, response, maxBodySize) {
  let chart;
  try {
    chart = parseRequest(await readBody(request, maxBodySize));
  } catch (error) {
    answer(response, error.status || 400, error.message);
    return;
  }
  try {
    answer(response, 200, renderChart(chart), SVG);
  } catch (error) {
    console.error("[ERROR] The chart could not be rendered", error);
    answer(response, 500, `The chart could not be rendered: ${error instanceof Error ? error.message : String(error)}`);
  }
}

/**
 * Answer a server error for a failure that the render did not handle, when the answer has not been sent yet. The server
 * keeps running
 * @param {http.ServerResponse} response Response
 * @param {*} error Failure
 */
function renderFailed(response, error) {
  console.error("[ERROR] The chart request failed", error);
  if (response.headersSent) {
    response.destroy();
  } else {
    answer(response, 500, "The chart request failed");
  }
}

/**
 * Create the HTTP server of the chart render
 * @param {{maxBodySize: number}} [options] Options: maximum size of the request body in bytes
 * @returns {http.Server} Server, not listening yet
 */
export function createChartServer({maxBodySize = DEFAULT_MAX_BODY_SIZE} = {}) {
  return http.createServer((request, response) => {
    const path = new URL(request.url, "http://localhost").pathname;
    if (request.method === "GET" && path === "/health") {
      answer(response, 200, "OK");
    } else if (request.method === "POST" && path === "/") {
      render(request, response, maxBodySize).catch(error => renderFailed(response, error));
    } else {
      answer(response, 404, "Not found");
    }
  });
}
