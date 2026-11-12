import "awe-react-client/css/main.css";
import "../css/main.css";

// The engine bootstraps itself on import. The look comes from the application properties (theme-primary, theme-mode), the
// screens from the XML descriptors and the landing styles from main.css. The only thing registered here is the canvas
// effect of the sign-in panel (awe.application.login.background-effect=route-grid)
const { registerLoginEffect } = require("awe-react-client/js/main");
const { startRouteGrid } = require("./login/routeGrid");

registerLoginEffect("route-grid", startRouteGrid);
