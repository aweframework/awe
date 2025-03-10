import {bindMethods, fetchAction} from "../utilities";

/**
 * Server service
 * @category Services
 */
class ServerService {

  /**
   * Create the service
   */
  constructor() {
    bindMethods(this, ["callServer"]);
  }

  /**
   * Call server and return call
   * @param {Object} action
   * @param {Object} values
   * @param {Object} props Properties
   */
  callServer(action, values, props) {
    const {parameters, target, address} = action;
    const {component} = address || {};
    const {serverAction, targetAction} = parameters;
    return fetchAction(serverAction, targetAction, {...values, ...parameters}, props.settings.token, null)
      .then(response => {
        // Add new actions
        props.addActionsTop(response.map(a => ({
          ...a,
          address: {...address, component: (target || component), ...(a.address || {})}
        })));

        // Finish previous action
        props.acceptAction(action);
      });
  }
}

export default ServerService;
