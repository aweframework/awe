import {createBrowserHistory} from 'history';
import {getContextPath} from "../utilities";

export const history = createBrowserHistory({
  basename: getContextPath(),
});
