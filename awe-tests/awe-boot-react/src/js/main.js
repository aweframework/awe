import "awe-react-client/css/main.css";
import "../css/main.css";

const { registerTemplate, addLocale, registerWidget } = require('awe-react-client/js/main');
const { default: EventCalendar } = require('./widgets/EventCalendar');

registerTemplate("test", {});
addLocale("ca-ES", {})

// Custom widgets
registerWidget("event-calendar", EventCalendar);
