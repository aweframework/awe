import React, {useCallback, useEffect, useRef, useState} from "react";
import {useTranslation} from "react-i18next";
import {useSelector} from "react-redux";
import {
  STEP, buildTicketRequest, choose, receiveArticles, receiveCategories, receiveFailure, receiveTicket, startChat, submitText
} from "./chatFlow.mjs";
import {createTicket, loadCategories, screenUrl, searchArticles} from "./chatApi";

/*
 * Support chat of the portal (widget "support-chat", registered by the application in main.js). A floating button opens a
 * panel where a scripted assistant (chatFlow.mjs) looks in the knowledge base and, if that is not enough, opens a ticket for
 * a visitor who has not signed in. The widget only draws the conversation and calls the server; the flow is in chatFlow.mjs.
 */

const KB_SCREEN = "/screen/hd-kb";

const ICON_PROPS = {
  width: 24, height: 24, viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: 2, strokeLinecap: "round",
  strokeLinejoin: "round", "aria-hidden": "true", focusable: "false"
};

// Lucide: message-circle, x and send
const ChatIcon = () => <svg {...ICON_PROPS}><path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z"/></svg>;
const CloseIcon = () => <svg {...ICON_PROPS}><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>;
const SendIcon = () => <svg {...ICON_PROPS}><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>;

/**
 * Content of one message of the conversation
 */
function Message({message, t}) {
  const {key, text, params, articles, summary, links} = message;
  return (
    <>
      {(key || text) && <p className="hd-chat-text">{key ? t(key, params) : text}</p>}
      {articles && (
        <>
          <ul className="hd-chat-articles">
            {articles.map((article) => (
              <li key={article.id} className="hd-chat-article">
                <strong>{article.title}</strong>
                <span>{article.excerpt}</span>
              </li>
            ))}
          </ul>
          <a className="hd-chat-link" href={screenUrl(KB_SCREEN)} target="_blank" rel="noopener noreferrer">{t("HD_CHAT_KB_LINK")}</a>
        </>
      )}
      {summary && (
        <dl className="hd-chat-summary">
          <dt>{t("HD_CHAT_SUMMARY_CATEGORY")}</dt><dd>{t(summary.category)}</dd>
          <dt>{t("HD_CHAT_SUMMARY_EMAIL")}</dt><dd>{summary.email}</dd>
          <dt>{t("HD_CHAT_SUMMARY_PROBLEM")}</dt><dd>{summary.problem}</dd>
        </dl>
      )}
      {params?.code && (
        <dl className="hd-chat-summary hd-chat-ticket">
          <dt>{t("HD_CHAT_TICKET_CODE")}</dt><dd>{params.code}</dd>
          <dt>{t("HD_CHAT_TICKET_KEY")}</dt><dd>{params.key}</dd>
        </dl>
      )}
      {links && links.map((link) => (
        <a key={link.href} className="hd-chat-link" href={screenUrl(link.href)} target="_blank" rel="noopener noreferrer">{t(link.labelKey)}</a>
      ))}
    </>
  );
}

/**
 * Support chat widget
 * @returns {React.ReactElement} Launcher button and chat panel
 */
export default function SupportChat() {
  const {t} = useTranslation();
  const token = useSelector((state) => state.settings?.token);
  const [open, setOpen] = useState(false);
  const [chat, setChat] = useState(startChat);
  const [draft, setDraft] = useState("");
  const launcher = useRef(null);
  const input = useRef(null);
  const honeypot = useRef(null);
  const log = useRef(null);
  const firstOption = useRef(null);
  const categoriesLoaded = useRef(false);
  const wasOpen = useRef(false);
  const {step, options, messages} = chat;
  const typing = step === STEP.PROBLEM || step === STEP.EMAIL;

  // The categories come from the enumerated list of the server, the first time the chat opens
  useEffect(() => {
    if (open && !categoriesLoaded.current) {
      categoriesLoaded.current = true;
      loadCategories(token).then((rows) => setChat((current) => receiveCategories(current, rows))).catch(() => {});
    }
  }, [open, token]);

  // Search the knowledge base for the problem
  useEffect(() => {
    if (step !== STEP.SEARCHING) {
      return undefined;
    }
    let cancelled = false;
    searchArticles(chat.problem, token)
      .then((articles) => !cancelled && setChat((current) => receiveArticles(current, articles)))
      .catch(() => !cancelled && setChat((current) => receiveFailure(current, "search")));
    return () => {
      cancelled = true;
    };
  }, [step, chat.problem, token]);

  // Open the ticket
  useEffect(() => {
    if (step !== STEP.SENDING) {
      return undefined;
    }
    let cancelled = false;
    createTicket(buildTicketRequest(chat, honeypot.current?.value), token)
      .then((answer) => !cancelled && setChat((current) => answer ? receiveTicket(current, answer) : receiveFailure(current, "ticket")))
      .catch(() => !cancelled && setChat((current) => receiveFailure(current, "ticket")));
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step]);

  // The last message is always in view
  useEffect(() => {
    if (log.current) {
      log.current.scrollTop = log.current.scrollHeight;
    }
  }, [messages, options, open]);

  // Focus follows the conversation: the first quick reply when there are some, the text box when the visitor has to type
  useEffect(() => {
    if (!open) {
      return;
    }
    if (options.length) {
      firstOption.current?.focus();
    } else if (typing) {
      input.current?.focus();
    }
  }, [open, options, typing]);

  // Closing returns the focus to the launcher once it shows again (on a phone it is hidden while the panel is open)
  useEffect(() => {
    if (wasOpen.current && !open) {
      launcher.current?.focus();
    }
    wasOpen.current = open;
  }, [open]);

  const close = useCallback(() => setOpen(false), []);

  // The screen is wrapped by a form of the engine: the chat is not a form of its own (it would nest), it sends on Enter and on the button
  const submit = () => {
    if (typing && draft.trim()) {
      setChat(submitText(chat, draft));
      setDraft("");
    }
  };

  const onInputKeyDown = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      submit();
    }
  };

  const onKeyDown = (event) => {
    if (event.key === "Escape") {
      event.stopPropagation();
      close();
    }
  };

  // Nothing to choose or type once the conversation is over
  const placeholderKey = step === STEP.EMAIL ? "HD_CHAT_INPUT_EMAIL" : typing ? "HD_CHAT_INPUT_PROBLEM" : options.length ? "HD_CHAT_INPUT_CHOOSE" : null;
  const placeholder = placeholderKey ? t(placeholderKey) : "";

  return (
    <div className={`hd-chat${open ? " is-open" : ""}`}>
      <section id="hd-chat-panel" className="hd-chat-panel" role="dialog" aria-label={t("HD_CHAT_TITLE")} hidden={!open} onKeyDown={onKeyDown}>
        <header className="hd-chat-header">
          <div className="hd-chat-heading">
            <h2 className="hd-chat-title">{t("HD_CHAT_TITLE")}</h2>
            <p className="hd-chat-subtitle">{t("HD_CHAT_SUBTITLE")}</p>
          </div>
          <button type="button" className="hd-chat-close" onClick={close} aria-label={t("HD_CHAT_CLOSE")}><CloseIcon/></button>
        </header>
        <div ref={log} className="hd-chat-log" role="log" aria-live="polite" aria-relevant="additions" aria-label={t("HD_CHAT_LOG")}
             aria-busy={step === STEP.SEARCHING || step === STEP.SENDING}>
          {messages.map((message) => (
            <div key={message.id} className={`hd-chat-message hd-chat-${message.from}`}>
              <span className="hd-chat-sr">{message.from === "bot" ? t("HD_CHAT_TITLE") : t("HD_CHAT_YOU")}: </span>
              <div className="hd-chat-bubble"><Message message={message} t={t}/></div>
            </div>
          ))}
          {options.length > 0 && (
            <div className="hd-chat-options" role="group" aria-label={t("HD_CHAT_LOG")}>
              {options.map((option, index) => (
                <button key={option.value} type="button" className="hd-chat-chip" ref={index === 0 ? firstOption : undefined}
                        onClick={() => setChat(choose(chat, option.value))}>{t(option.labelKey)}</button>
              ))}
            </div>
          )}
        </div>
        <div className="hd-chat-form">
          {/* Honeypot: invisible to a person (and out of the tab order and the screen readers), a bot fills it and the server drops the request */}
          <input ref={honeypot} className="hd-chat-hp" type="text" name="hd-website" tabIndex={-1} autoComplete="off" aria-hidden="true" defaultValue=""/>
          <input ref={input} className="hd-chat-input" type={step === STEP.EMAIL ? "email" : "text"} value={draft} disabled={!typing}
                 maxLength={step === STEP.EMAIL ? 80 : 1000} placeholder={placeholder} aria-label={placeholder || t("HD_CHAT_INPUT_PROBLEM")}
                 onChange={(event) => setDraft(event.target.value)} onKeyDown={onInputKeyDown}/>
          <button type="button" className="hd-chat-send" onClick={submit} disabled={!typing || !draft.trim()} aria-label={t("HD_CHAT_SEND")}><SendIcon/></button>
        </div>
      </section>
      <button ref={launcher} type="button" className="hd-chat-launcher" aria-expanded={open} aria-controls="hd-chat-panel"
              aria-label={t("HD_CHAT_LAUNCHER")} onClick={() => (open ? close() : setOpen(true))}>
        <ChatIcon/>
        <span className="hd-chat-launcher-text">{t("HD_CHAT_LAUNCHER_SHORT")}</span>
      </button>
    </div>
  );
}
