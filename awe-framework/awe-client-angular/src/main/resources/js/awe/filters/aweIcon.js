import {aweApplication} from "../awe";
import {getIcon} from "../services/icon";

// Icon filter: the icon of a value of the data for the served templates ({{(value | aweIcon).href}}), see services/icon
aweApplication.filter('aweIcon', () => getIcon);
