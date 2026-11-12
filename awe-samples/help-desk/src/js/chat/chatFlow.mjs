/*
 * Conversation of the support chat of the portal: a scripted flow, no AI and no external service. Pure functions only (no
 * React, no network), so the whole conversation is unit tested. The widget (SupportChat.jsx) calls the server and hands
 * the answers to these functions, which return the next state: the messages to show, the quick replies and the step.
 *
 * Messages hold locale keys, never English text, so the widget translates them with the engine's locale mechanism.
 * Steps: problem -> searching -> suggestions -> category -> email -> confirm -> sending -> done
 */

export const STEP = {
  PROBLEM: "problem", SEARCHING: "searching", SUGGESTIONS: "suggestions", CATEGORY: "category", EMAIL: "email",
  CONFIRM: "confirm", SENDING: "sending", DONE: "done"
};

const MIN_PROBLEM = 5;
const MAX_ARTICLES = 3;
const MAX_TERMS = 3;
const EMAIL = /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)*\.[^@\s.]{2,}$/;
const FALLBACK_CATEGORIES = [{label: "HD_CATEGORY_OTHER", value: "OTHER"}];
const OPT_SOLVED = {value: "solved", labelKey: "HD_CHAT_OPT_SOLVED"};
const OPT_HELP = {value: "help", labelKey: "HD_CHAT_OPT_HELP"};
const OPT_SEND = {value: "send", labelKey: "HD_CHAT_OPT_SEND"};
const OPT_EDIT = {value: "edit", labelKey: "HD_CHAT_OPT_EDIT"};

// Words that tell nothing about the problem (English, Spanish and French)
const STOP_WORDS = new Set(("the and for can cant cannot could would should does doesnt dont wont isnt not with from that this have has was are you your our why how what when its but too " +
  "please need want help puedo quiero necesito tengo los las una que con por para mis del como pero cuando " +
  "les des une pour avec mon mes pas peux veux besoin sur dans").split(" "));

/** @returns {object} State of a new conversation: the greeting and the question */
export function startChat() {
  return bot({step: STEP.PROBLEM, messages: [], options: [], problem: "", category: null, email: "", categories: [], searched: false, nextId: 1},
    {key: "HD_CHAT_GREETING"}, {key: "HD_CHAT_ASK_PROBLEM"});
}

/** Categories of the enumerated list the server offers (HdkCatPub): {label (locale key), value} */
export function receiveCategories(state, rows) {
  return {...state, categories: rows.filter((row) => row && row.value).map(({label, value}) => ({label, value}))};
}

/** Text typed by the visitor: the problem or the email, depending on the step. Anything else is ignored */
export function submitText(state, input) {
  const text = (input || "").trim();
  if (!text || (state.step !== STEP.PROBLEM && state.step !== STEP.EMAIL)) {
    return state;
  }
  return state.step === STEP.PROBLEM ? submitProblem(visitor(state, {text}), text) : submitEmail(visitor(state, {text}), text);
}

/** Articles found for the problem (rows of HdkArtPub, already ranked) */
export function receiveArticles(state, articles) {
  if (state.step !== STEP.SEARCHING) {
    return state;
  }
  const found = articles.slice(0, MAX_ARTICLES).map((row) => ({id: row.IdeArt, title: row.Tit, excerpt: excerpt(row.Txt, 110)}));
  if (!found.length) {
    return askCategory(bot(state, {key: "HD_CHAT_NONE"}));
  }
  return bot({...state, step: STEP.SUGGESTIONS, options: [OPT_SOLVED, OPT_HELP]}, {key: "HD_CHAT_FOUND", articles: found});
}

/** A call to the server failed: "search" goes on without suggestions, "ticket" lets the visitor send again */
export function receiveFailure(state, kind) {
  if (kind === "search" && state.step === STEP.SEARCHING) {
    return askCategory(bot(state, {key: "HD_CHAT_SEARCH_ERROR"}));
  }
  if (kind === "ticket" && state.step === STEP.SENDING) {
    return bot({...state, step: STEP.CONFIRM, options: [OPT_SEND, OPT_EDIT]}, {key: "HD_CHAT_ERR_GENERIC"});
  }
  return state;
}

/** Quick reply chosen by the visitor. A value that is not one of the current options is ignored */
export function choose(state, value) {
  const option = state.options.find((candidate) => candidate.value === value);
  if (!option) {
    return state;
  }
  const next = visitor({...state, options: []}, {key: option.labelKey});
  switch (state.step) {
    case STEP.SUGGESTIONS:
      return value === "solved" ? bot({...next, step: STEP.DONE}, {key: "HD_CHAT_GLAD"}) : askCategory(next);
    case STEP.CATEGORY:
      return bot({...next, step: STEP.EMAIL, category: {value, label: option.labelKey}}, {key: "HD_CHAT_ASK_EMAIL"});
    case STEP.CONFIRM:
      return value === "send"
        ? bot({...next, step: STEP.SENDING}, {key: "HD_CHAT_SENDING"})
        : bot({...next, step: STEP.PROBLEM, searched: true}, {key: "HD_CHAT_EDIT"});
    default:
      return state;
  }
}

/**
 * Answer of the maintain HdkChatTicket: {type, title, message}. "ok" carries the ticket code as title and the access key as
 * message; "ok" with another title is a request the server dropped as a bot; a warning has the refusal as title
 */
export function receiveTicket(state, answer) {
  if (state.step !== STEP.SENDING || !answer) {
    return state;
  }
  if (answer.type === "ok") {
    if (/^HD-\d+$/.test(answer.title) && answer.message) {
      return bot({...state, step: STEP.DONE}, {key: "HD_CHAT_DONE", params: {code: answer.title, key: answer.message}},
        {key: "HD_CHAT_FOLLOW", links: [{href: "/screen/hd-check", labelKey: "HD_CHAT_CHECK_LINK"}]});
    }
    return bot({...state, step: STEP.DONE}, {key: "HD_CHAT_RECEIVED"});
  }
  switch (answer.title) {
    case "LIMIT":
      return bot({...state, step: STEP.DONE}, {key: "HD_CHAT_ERR_LIMIT"});
    case "EMAIL":
      return bot({...state, step: STEP.EMAIL}, {key: "HD_CHAT_EMAIL_INVALID"});
    case "CATEGORY":
      return askCategory(state);
    case "DESCRIPTION":
      return bot({...state, step: STEP.PROBLEM, searched: true}, {key: "HD_CHAT_ERR_DESCRIPTION"});
    default:
      return receiveFailure(state, "ticket");
  }
}

/** Parameters of the maintain HdkChatTicket. The honeypot is a hidden field: only a bot fills it, the server drops it */
export function buildTicketRequest(state, honeypot) {
  return {Hp: honeypot || "", Eml: state.email, Cat: state.category.value, Dsc: state.problem};
}

/** The few meaningful words of a text, longest first, to search the knowledge base with */
export function searchTerms(text) {
  const words = (text || "").toLowerCase().split(/[^\p{L}\p{N}-]+/u).map((word) => word.replace(/^-+|-+$/g, ""));
  const terms = [...new Set(words.filter((word) => word.length >= 3 && !STOP_WORDS.has(word)))];
  return terms.map((term, index) => ({term, index})).sort((a, b) => b.term.length - a.term.length || a.index - b.index)
    .slice(0, MAX_TERMS).map(({term}) => term);
}

/** Merge the rows of every search: most terms matched first, then most viewed. At most three */
export function rankArticles(resultsByTerm) {
  const found = new Map();
  resultsByTerm.flat().forEach((row) => {
    const entry = found.get(row.IdeArt) || {row, hits: 0};
    entry.hits += 1;
    found.set(row.IdeArt, entry);
  });
  return [...found.values()].sort((a, b) => b.hits - a.hits || (b.row.Vws || 0) - (a.row.Vws || 0))
    .slice(0, MAX_ARTICLES).map(({row}) => row);
}

/** Text cut at a word, with an ellipsis, to fit a card */
export function excerpt(text, max) {
  const value = (text || "").trim();
  if (value.length <= max) {
    return value;
  }
  const cut = value.slice(0, max - 3);
  const space = cut.lastIndexOf(" ");
  return `${(space > 0 ? cut.slice(0, space) : cut).trimEnd()}...`;
}

/** Same rule as the server: a mailbox, an @ and a domain with a dot (the server checks it again) */
export function isValidEmail(email) {
  return EMAIL.test((email || "").trim());
}

/** Rows of the "fill" action of a query answer */
export function responseRows(actions) {
  const fill = (actions || []).find((action) => action.type === "fill");
  return fill?.parameters?.datalist?.rows || [];
}

/** {type, title, message} of the "message" action of a maintain answer, or null */
export function responseMessage(actions) {
  const message = (actions || []).find((action) => action.type === "message");
  if (!message) {
    return null;
  }
  const {type, title, message: text} = message.parameters || {};
  return {type, title, message: text};
}

function submitProblem(state, text) {
  if (text.length < MIN_PROBLEM) {
    return bot(state, {key: "HD_CHAT_PROBLEM_SHORT"});
  }
  const next = {...state, problem: text};
  return state.searched ? askCategory(next) : bot({...next, step: STEP.SEARCHING}, {key: "HD_CHAT_SEARCHING"});
}

function submitEmail(state, text) {
  if (!isValidEmail(text)) {
    return bot(state, {key: "HD_CHAT_EMAIL_INVALID"});
  }
  const email = text.toLowerCase();
  return bot({...state, step: STEP.CONFIRM, email, options: [OPT_SEND, OPT_EDIT]},
    {key: "HD_CHAT_SUMMARY", summary: {category: state.category.label, email, problem: state.problem}});
}

function askCategory(state) {
  const categories = state.categories.length ? state.categories : FALLBACK_CATEGORIES;
  return bot({...state, step: STEP.CATEGORY, options: categories.map(({label, value}) => ({value, labelKey: label}))},
    {key: "HD_CHAT_ASK_CATEGORY"});
}

function bot(state, ...messages) {
  return addMessages(state, messages.map((message) => ({from: "bot", ...message})));
}

function visitor(state, message) {
  return addMessages(state, [{from: "visitor", ...message}]);
}

function addMessages(state, messages) {
  let id = state.nextId;
  const added = messages.map((message) => ({id: id++, ...message}));
  return {...state, messages: [...state.messages, ...added], nextId: id};
}
