import test from "node:test";
import assert from "node:assert/strict";
import {
  STEP, buildTicketRequest, choose, excerpt, isValidEmail, rankArticles, receiveArticles, receiveCategories,
  receiveFailure, receiveTicket, responseMessage, responseRows, searchTerms, startChat, submitText
} from "../../js/chat/chatFlow.mjs";

const CATEGORIES = [
  {label: "HD_CATEGORY_NETWORK", value: "NETWORK"},
  {label: "HD_CATEGORY_OTHER", value: "OTHER"}
];
const PROBLEM = "The guest Wi-Fi asks for a password I do not have";
const ARTICLES = [
  {IdeArt: 4, Tit: "Use the guest Wi-Fi", Txt: "How to: use the guest wi-fi. 1) Check the basics.", Vws: 428},
  {IdeArt: 2, Tit: "Fix a slow Wi-Fi connection", Txt: "How to: fix a slow wi-fi connection.", Vws: 234}
];

const last = (state) => state.messages[state.messages.length - 1];
const keys = (state) => state.messages.filter((m) => m.from === "bot").map((m) => m.key);

/** A chat that has the problem and is waiting for the category */
function atCategory() {
  let state = receiveCategories(startChat(), CATEGORIES);
  state = submitText(state, PROBLEM);
  state = receiveArticles(state, ARTICLES);
  return choose(state, "help");
}

/** A chat that has the category and the email and waits for the confirmation */
function atConfirm() {
  let state = choose(atCategory(), "NETWORK");
  return submitText(state, " Ana.Torres@Mail.Example ");
}

test("the chat greets and asks what is wrong", () => {
  const state = startChat();
  assert.equal(state.step, STEP.PROBLEM);
  assert.deepEqual(keys(state), ["HD_CHAT_GREETING", "HD_CHAT_ASK_PROBLEM"]);
  assert.deepEqual(state.options, []);
});

test("a blank text is ignored and a very short one asks for more", () => {
  const blank = submitText(startChat(), "   ");
  assert.equal(blank.messages.length, 2);
  const short = submitText(startChat(), "help");
  assert.equal(short.step, STEP.PROBLEM);
  assert.equal(last(short).key, "HD_CHAT_PROBLEM_SHORT");
});

test("the problem is stored and the chat starts searching", () => {
  const state = submitText(startChat(), `  ${PROBLEM} `);
  assert.equal(state.step, STEP.SEARCHING);
  assert.equal(state.problem, PROBLEM);
  assert.deepEqual(state.messages.at(-2), {id: state.messages.at(-2).id, from: "visitor", text: PROBLEM});
  assert.equal(last(state).key, "HD_CHAT_SEARCHING");
});

test("the suggestions show at most three articles with the quick replies", () => {
  const four = [...ARTICLES, {IdeArt: 9, Tit: "A", Txt: "a"}, {IdeArt: 10, Tit: "B", Txt: "b"}];
  const state = receiveArticles(submitText(startChat(), PROBLEM), four);
  assert.equal(state.step, STEP.SUGGESTIONS);
  assert.equal(last(state).key, "HD_CHAT_FOUND");
  assert.equal(last(state).articles.length, 3);
  assert.deepEqual(last(state).articles[0], {id: 4, title: "Use the guest Wi-Fi", excerpt: "How to: use the guest wi-fi. 1) Check the basics."});
  assert.deepEqual(state.options.map((o) => o.value), ["solved", "help"]);
});

test("with no article the chat goes on to the category", () => {
  const state = receiveArticles(receiveCategories(submitText(startChat(), PROBLEM), CATEGORIES), []);
  assert.equal(state.step, STEP.CATEGORY);
  assert.deepEqual(keys(state).slice(-2), ["HD_CHAT_NONE", "HD_CHAT_ASK_CATEGORY"]);
  assert.deepEqual(state.options, [
    {value: "NETWORK", labelKey: "HD_CATEGORY_NETWORK"}, {value: "OTHER", labelKey: "HD_CATEGORY_OTHER"}]);
});

test("a failed search also goes on to the category", () => {
  const state = receiveFailure(submitText(startChat(), PROBLEM), "search");
  assert.equal(state.step, STEP.CATEGORY);
  assert.deepEqual(keys(state).slice(-2), ["HD_CHAT_SEARCH_ERROR", "HD_CHAT_ASK_CATEGORY"]);
});

test("'This solved it' closes the conversation", () => {
  const state = choose(receiveArticles(submitText(startChat(), PROBLEM), ARTICLES), "solved");
  assert.equal(state.step, STEP.DONE);
  assert.equal(last(state).key, "HD_CHAT_GLAD");
  assert.deepEqual(state.options, []);
  assert.equal(state.messages.at(-2).from, "visitor");
});

test("'I still need help' asks for the category of the enumerated list", () => {
  const state = atCategory();
  assert.equal(state.step, STEP.CATEGORY);
  assert.equal(last(state).key, "HD_CHAT_ASK_CATEGORY");
  assert.deepEqual(state.options.map((o) => o.value), ["NETWORK", "OTHER"]);
});

test("only a category of the list is accepted", () => {
  const state = atCategory();
  assert.equal(choose(state, "BILLING"), state);
  const chosen = choose(state, "NETWORK");
  assert.equal(chosen.step, STEP.EMAIL);
  assert.deepEqual(chosen.category, {value: "NETWORK", label: "HD_CATEGORY_NETWORK"});
  assert.equal(last(chosen).key, "HD_CHAT_ASK_EMAIL");
});

test("an invalid email is asked again and a valid one shows the summary", () => {
  const chosen = choose(atCategory(), "NETWORK");
  const invalid = submitText(chosen, "ana@mail");
  assert.equal(invalid.step, STEP.EMAIL);
  assert.equal(last(invalid).key, "HD_CHAT_EMAIL_INVALID");
  const state = atConfirm();
  assert.equal(state.step, STEP.CONFIRM);
  assert.equal(state.email, "ana.torres@mail.example");
  assert.deepEqual(last(state).summary, {category: "HD_CATEGORY_NETWORK", email: "ana.torres@mail.example", problem: PROBLEM});
  assert.deepEqual(state.options.map((o) => o.value), ["send", "edit"]);
});

test("'Edit' asks for the problem again and skips the search", () => {
  const edit = choose(atConfirm(), "edit");
  assert.equal(edit.step, STEP.PROBLEM);
  assert.equal(last(edit).key, "HD_CHAT_EDIT");
  const again = submitText(edit, "The printer of the second floor is out of toner");
  assert.equal(again.step, STEP.CATEGORY);
  assert.equal(again.problem, "The printer of the second floor is out of toner");
});

test("'Send' builds the request for the server with the honeypot as it is", () => {
  const sending = choose(atConfirm(), "send");
  assert.equal(sending.step, STEP.SENDING);
  assert.deepEqual(buildTicketRequest(sending, ""), {Hp: "", Eml: "ana.torres@mail.example", Cat: "NETWORK", Dsc: PROBLEM});
  assert.equal(buildTicketRequest(sending, "http://spam.example").Hp, "http://spam.example");
});

test("the ticket number and the access key close the conversation", () => {
  const state = receiveTicket(choose(atConfirm(), "send"), {type: "ok", title: "HD-601", message: "K7M2PQ9X"});
  assert.equal(state.step, STEP.DONE);
  const done = state.messages.at(-2);
  assert.equal(done.key, "HD_CHAT_DONE");
  assert.deepEqual(done.params, {code: "HD-601", key: "K7M2PQ9X"});
  assert.equal(last(state).key, "HD_CHAT_FOLLOW");
  assert.deepEqual(last(state).links, [{href: "/screen/hd-check", labelKey: "HD_CHAT_CHECK_LINK"}]);
});

test("a request the server dropped as a bot never shows a ticket", () => {
  const state = receiveTicket(choose(atConfirm(), "send"), {type: "ok", title: "RECEIVED", message: "Done"});
  assert.equal(state.step, STEP.DONE);
  assert.deepEqual(keys(state).slice(-1), ["HD_CHAT_RECEIVED"]);
  assert.ok(!state.messages.some((m) => m.params?.code));
});

test("each refusal of the server leads to the step that fixes it", () => {
  const sending = choose(atConfirm(), "send");
  const limit = receiveTicket(sending, {type: "warning", title: "LIMIT", message: ""});
  assert.equal(limit.step, STEP.DONE);
  assert.equal(last(limit).key, "HD_CHAT_ERR_LIMIT");
  assert.equal(receiveTicket(sending, {type: "warning", title: "EMAIL", message: ""}).step, STEP.EMAIL);
  assert.equal(receiveTicket(sending, {type: "warning", title: "CATEGORY", message: ""}).step, STEP.CATEGORY);
  assert.equal(receiveTicket(sending, {type: "warning", title: "DESCRIPTION", message: ""}).step, STEP.PROBLEM);
});

test("an unreachable server lets the visitor try again", () => {
  const state = receiveFailure(choose(atConfirm(), "send"), "ticket");
  assert.equal(state.step, STEP.CONFIRM);
  assert.equal(last(state).key, "HD_CHAT_ERR_GENERIC");
  assert.deepEqual(state.options.map((o) => o.value), ["send", "edit"]);
});

test("the search terms are the few meaningful words of the text", () => {
  assert.deepEqual(searchTerms("I can't connect to the VPN from home"), ["connect", "home", "vpn"]);
  assert.deepEqual(searchTerms("I cannot connect to the VPN from home"), ["connect", "home", "vpn"]);
  assert.deepEqual(searchTerms("No puedo entrar en mi correo"), ["entrar", "correo"]);
  assert.deepEqual(searchTerms("a an to"), []);
});

test("the articles of every search are merged, ranked by matches and views, and cut to three", () => {
  const a = {IdeArt: 1, Vws: 10}, b = {IdeArt: 2, Vws: 500}, c = {IdeArt: 3, Vws: 50}, d = {IdeArt: 4, Vws: 20};
  assert.deepEqual(rankArticles([[a, b], [a, c, d], [a]]).map((x) => x.IdeArt), [1, 2, 3]);
  assert.deepEqual(rankArticles([[], []]), []);
});

test("an excerpt is cut at a word and ends with an ellipsis", () => {
  assert.equal(excerpt("short text", 40), "short text");
  assert.equal(excerpt("one two three four five", 12), "one two...");
});

test("an email needs a mailbox, an @ and a domain with a dot", () => {
  assert.ok(isValidEmail("ana@mail.example"));
  for (const email of ["", "ana", "ana@", "ana@mail", "a na@mail.example", "ana@@mail.example"]) {
    assert.ok(!isValidEmail(email), email);
  }
});

test("the engine answers are read: rows of the fill action and the message of the maintain", () => {
  const rows = [{id: 1}];
  assert.deepEqual(responseRows([{type: "fill", parameters: {datalist: {rows}}}, {type: "end-load"}]), rows);
  assert.deepEqual(responseRows([{type: "end-load"}]), []);
  assert.deepEqual(responseMessage([{type: "end-load"}, {type: "message", parameters: {type: "warning", title: "LIMIT", message: "x"}}]),
    {type: "warning", title: "LIMIT", message: "x"});
  assert.equal(responseMessage([{type: "end-load"}]), null);
});
