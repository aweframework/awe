import {rankArticles, responseMessage, responseRows, searchTerms} from "./chatFlow.mjs";

/*
 * Calls of the support chat to the server. They use the transport of the engine for any action: POST /action/<kind>/<target>
 * with the XSRF token of the cookie (and the session token when there is one: a visitor has none). The engine answers a list
 * of client actions, which chatFlow reads.
 */

const ARTICLES_PER_TERM = 3;

/**
 * Context path of the application, from the base tag of the page
 * @returns {string} Path with no trailing slash ("" when the application runs at the root)
 */
export function contextPath() {
  const base = document.getElementsByTagName("base")[0];
  return base ? new URL(base.href).pathname.replace(/\/+$/, "") : "";
}

/**
 * Absolute URL of a screen of the application
 * @param {string} screen Path of the screen (/screen/hd-check)
 * @returns {string} URL
 */
export function screenUrl(screen) {
  return `${window.location.origin}${contextPath()}${screen}`;
}

function xsrfToken() {
  const cookie = document.cookie.split(";").map((item) => item.trim()).find((item) => item.startsWith("XSRF-TOKEN="));
  return cookie ? cookie.slice("XSRF-TOKEN=".length) : undefined;
}

async function call(kind, target, body, token) {
  const headers = {"Accept": "application/json", "Content-Type": "application/json;charset=UTF-8"};
  if (token) {
    headers.Authorization = token;
  }
  const xsrf = xsrfToken();
  if (xsrf) {
    headers["X-XSRF-TOKEN"] = xsrf;
  }
  const response = await fetch(`${contextPath()}/action/${kind}/${target}`, {method: "POST", headers, body: JSON.stringify(body)});
  if (!response.ok) {
    throw new Error(`${target}: ${response.status}`);
  }
  return response.json();
}

/**
 * Categories of the enumerated list (public query HdkCatPub)
 * @param {string} [token] Session token
 * @returns {Promise<Array>} Rows {label, value}
 */
export async function loadCategories(token) {
  return responseRows(await call("data", "HdkCatPub", {}, token));
}

/**
 * Published articles that match the text (public query HdkArtPub, one search per meaningful word, merged and ranked)
 * @param {string} text Problem described by the visitor
 * @param {string} [token] Session token
 * @returns {Promise<Array>} At most three rows
 */
export async function searchArticles(text, token) {
  const answers = await Promise.all(searchTerms(text).map(async (term) =>
    responseRows(await call("data", "HdkArtPub", {CrtKbSch: term, max: ARTICLES_PER_TERM, page: 1}, token))));
  return rankArticles(answers);
}

/**
 * Open the ticket (public maintain HdkChatTicket)
 * @param {object} request Hp, Eml, Cat and Dsc
 * @param {string} [token] Session token
 * @returns {Promise<object|null>} {type, title, message}
 */
export async function createTicket(request, token) {
  return responseMessage(await call("maintain", "HdkChatTicket", request, token));
}
