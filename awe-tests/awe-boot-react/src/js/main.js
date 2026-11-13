import "awe-react-client/css/main.css";
import "../css/main.css";

const { registerLoginEffect, registerTemplate, addLocale, registerWidget } = require('awe-react-client/js/main');
const { createElement, lazy, Suspense } = require('react');
const { startSoftBlobs } = require('./login/softBlobs');

registerTemplate("test", {});
addLocale("ca-ES", {});

// Custom widgets. FullCalendar is loaded on demand, in its own chunk, the first time a screen shows the widget
const EventCalendar = lazy(() => import('./widgets/EventCalendar'));
registerWidget("event-calendar", (props) => createElement(Suspense, {fallback: null}, createElement(EventCalendar, props)));

// Login canvas effect (awe.application.login.background-effect=soft-blobs)
registerLoginEffect("soft-blobs", startSoftBlobs);
