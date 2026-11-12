import "awe-react-client/css/main.css";
import "../css/main.css";

// The engine bootstraps itself on import. Its look comes from the application properties (theme-primary, theme-mode) and its
// screens from the XML descriptors. The one thing the application registers is the widget of the support chat of the portal
// (<widget type="support-chat"/> in hd-home.xml): a floating button with a scripted assistant that finds articles and opens a ticket
const { registerWidget } = require("awe-react-client/js/main");
const { default: SupportChat } = require("./chat/SupportChat");

registerWidget("support-chat", SupportChat);
