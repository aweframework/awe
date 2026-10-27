import "awe-react-client/css/main.css";
import "../css/main.css";

const { registerLoginEffect, registerTemplate, addLocale, registerWidget } = require('awe-react-client/js/main');
const { default: EventCalendar } = require('./widgets/EventCalendar');
const { startSoftBlobs } = require('./login/softBlobs');

registerTemplate("test", {});
addLocale("ca-ES", {})

// Custom widgets
registerWidget("event-calendar", EventCalendar);

// Login canvas effect (awe.application.login.background-effect=soft-blobs)
registerLoginEffect("soft-blobs", startSoftBlobs);
