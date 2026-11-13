import React, { useCallback, useRef, useState } from "react";
import PropTypes from "prop-types";
import { useTranslation } from "react-i18next";
import FullCalendar from "@fullcalendar/react";
import dayGridPlugin from "@fullcalendar/daygrid";
import timeGridPlugin from "@fullcalendar/timegrid";
import listPlugin from "@fullcalendar/list";
import interactionPlugin from "@fullcalendar/interaction";
import esLocale from "@fullcalendar/core/locales/es";
import frLocale from "@fullcalendar/core/locales/fr";
import enGbLocale from "@fullcalendar/core/locales/en-gb";

const EVENT_COLORS = ["#1b809e", "#2e7d32", "#c1440e", "#6a1b9a", "#f9a825"];

// AWE language codes mapped to FullCalendar locales
const CALENDAR_LOCALES = {
  "es-ES": esLocale,
  "fr-FR": frLocale,
  "en-GB": enGbLocale
};

/**
 * Format a date as an input[type=date] value (YYYY-MM-DD)
 * @param {Date} date Source date
 * @returns {string} Date value
 */
function toDateValue(date) {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

/**
 * Format a date as an input[type=time] value (HH:mm)
 * @param {Date} date Source date
 * @returns {string} Time value
 */
function toTimeValue(date) {
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

/**
 * Composer form for a given day, in create mode
 * @param {string} [day] Day as YYYY-MM-DD; defaults to today
 * @returns {object} Form state
 */
function emptyForm(day) {
  return {
    id: null,
    title: "",
    date: day || toDateValue(new Date()),
    time: "",
    color: EVENT_COLORS[0]
  };
}

// In-memory demo events, as in the awe-boot (Angular) event-calendar example.
// Persistence would go through the widget server-action instead.
function seedEvents() {
  const today = new Date();
  const day = toDateValue(today);
  const soon = new Date(today);
  soon.setDate(today.getDate() + 2);
  const later = new Date(today);
  later.setDate(today.getDate() + 5);
  const withColor = (event, color) => ({ ...event, backgroundColor: color, borderColor: color });
  return [
    withColor({ id: "seed-1", title: "Daily standup", start: `${day}T09:30:00` }, EVENT_COLORS[0]),
    withColor({ id: "seed-2", title: "Design review", start: `${day}T16:00:00` }, EVENT_COLORS[3]),
    withColor({ id: "seed-3", title: "Release planning", start: `${toDateValue(soon)}T11:00:00` }, EVENT_COLORS[1]),
    withColor({ id: "seed-4", title: "Team offsite", start: toDateValue(later), allDay: true }, EVENT_COLORS[2])
  ];
}

/**
 * Custom widget example: FullCalendar event calendar.
 *
 * React port of the awe-boot aweEventCalendar AngularJS widget. Registered by
 * the host application through `registerWidget("event-calendar", EventCalendar)`,
 * so `<widget type="event-calendar" id="..."/>` resolves it without modifying
 * awe-react core. Receives the widget node attributes (id, style, ...) as props.
 *
 * Events are kept in memory; persisting them would go through the widget
 * server-action instead. The composer below the calendar creates events with
 * title, date, optional time (all-day when empty) and color; clicking a day
 * preloads the date and clicking an event loads it for edit/delete.
 */
function EventCalendar({ id, style = "" }) {
  const { t, i18n } = useTranslation();
  const [events, setEvents] = useState(seedEvents);
  const [mode, setMode] = useState("create");
  const [form, setForm] = useState(() => emptyForm());
  const nextIdRef = useRef(1);

  const setField = (field) => (event) => {
    const { value } = event.target;
    setForm((current) => ({ ...current, [field]: value }));
  };

  const resetForm = useCallback((day) => {
    setMode("create");
    setForm(emptyForm(day));
  }, []);

  // Clicking a day preloads the composer for that date
  const onDateClick = useCallback((info) => {
    resetForm(info.dateStr.slice(0, 10));
  }, [resetForm]);

  // Clicking an event loads it into the composer for edit/delete
  const onEventClick = useCallback((info) => {
    info.jsEvent.preventDefault();
    const start = info.event.start || new Date();
    setMode("edit");
    setForm({
      id: info.event.id,
      title: info.event.title,
      date: toDateValue(start),
      time: info.event.allDay ? "" : toTimeValue(start),
      color: info.event.backgroundColor || EVENT_COLORS[0]
    });
  }, []);

  // Create or update an event from the composer. Not a native <form>: the AWE
  // screen already wraps content in one (FormContainer) and forms cannot nest.
  const submitForm = () => {
    if (!form.title) {
      return;
    }
    const changes = {
      title: form.title,
      start: form.time ? `${form.date}T${form.time}:00` : form.date,
      allDay: !form.time,
      backgroundColor: form.color,
      borderColor: form.color
    };
    if (mode === "edit") {
      setEvents((current) => current.map((item) => item.id === form.id ? { ...item, ...changes } : item));
    } else {
      setEvents((current) => [...current, { id: `ec-${nextIdRef.current++}`, ...changes }]);
    }
    resetForm(form.date);
  };

  // Remove the event currently loaded in the composer
  const removeEvent = () => {
    setEvents((current) => current.filter((item) => item.id !== form.id));
    resetForm();
  };

  return (
    <div id={id} className={`awe-ec ${style}`}>
      <div className="awe-ec-mount">
        <FullCalendar
          plugins={[dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin]}
          initialView="dayGridMonth"
          firstDay={1}
          height="100%"
          headerToolbar={{
            left: "prev,next today",
            center: "title",
            right: "dayGridMonth,timeGridWeek,listWeek"
          }}
          locale={CALENDAR_LOCALES[i18n.language] ?? enGbLocale}
          selectable={true}
          nowIndicator={true}
          dayMaxEvents={true}
          events={events}
          dateClick={onDateClick}
          eventClick={onEventClick}
        />
      </div>
      <div className="awe-ec-composer">
        <div className="awe-ec-composer-head">
          <i className={`fa ${mode === "edit" ? "fa-pencil" : "fa-plus-circle"}`} />
          <span>{t(mode === "edit" ? "SCR_EVENT_CALENDAR_EDIT" : "SCR_EVENT_CALENDAR_NEW")}</span>
        </div>
        <div className="awe-ec-form">
          <input className="awe-ec-input awe-ec-input-title" type="text" required
            placeholder={t("SCR_EVENT_CALENDAR_TITLE")} value={form.title} onChange={setField("title")}
            onKeyDown={(event) => event.key === "Enter" && submitForm()} />
          <div className="awe-ec-field">
            <label htmlFor={`${id}-date`}>{t("SCR_EVENT_CALENDAR_DATE")}</label>
            <input id={`${id}-date`} className="awe-ec-input" type="date" required
              value={form.date} onChange={setField("date")} />
          </div>
          <div className="awe-ec-field">
            <label htmlFor={`${id}-time`}>{t("SCR_EVENT_CALENDAR_TIME")}</label>
            <input id={`${id}-time`} className="awe-ec-input" type="time"
              value={form.time} onChange={setField("time")} />
          </div>
          <div className="awe-ec-colors">
            {EVENT_COLORS.map((color) => (
              <span key={color} role="button" aria-label={color}
                className={`awe-ec-color ${form.color === color ? "awe-ec-color-on" : ""}`}
                style={{ background: color }}
                onClick={() => setForm((current) => ({ ...current, color }))} />
            ))}
          </div>
          <div className="awe-ec-form-actions">
            <button type="button" className="awe-ec-btn awe-ec-btn-primary" disabled={!form.title}
              onClick={submitForm}>
              {t("BUTTON_SAVE")}
            </button>
            {mode === "edit" && (
              <button type="button" className="awe-ec-btn awe-ec-btn-danger" onClick={removeEvent}>
                {t("BUTTON_DELETE")}
              </button>
            )}
            {mode === "edit" && (
              <button type="button" className="awe-ec-btn" onClick={() => resetForm()}>
                {t("BUTTON_CANCEL")}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

EventCalendar.propTypes = {
  id: PropTypes.string.isRequired,
  style: PropTypes.string
};

export default EventCalendar;
