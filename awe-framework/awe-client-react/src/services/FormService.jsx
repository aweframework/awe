import {ButtonTypes} from "../redux/actions/components";
import ServerService from "./ServerService";
import {
  asArray,
  fetchFile,
  generateServerAction,
  getActionAddress,
  getActionSource,
  getComponent,
  getRestUrl,
  isInsideContext
} from "../utilities";
import {
  checkModelIsEmpty,
  checkModelIsUnchanged,
  checkModelIsUpdated,
  getFormValues,
  getFormValuesForPrinting
} from "../utilities/components";

const {BUTTON_SUBMIT} = ButtonTypes;

let downloadFormIdentifier = 0;

/**
 * Form service
 * @category Services
 */
class FormService {

  /**
   * Create the service
   */
  constructor() {
    this.serverService = new ServerService();
  }

  getActions() {
    return {
      "reset": this.reset.bind(this),
      "restore": this.restore.bind(this),
      "restore-target": this.restore.bind(this),
      "submit": this.submit.bind(this),
      "validate": this.validate.bind(this),
      "verify-validation": this.verifyValidation.bind(this),
      "set-valid": this.setValid.bind(this),
      "set-invalid": this.setInvalid.bind(this),
      "server": this.server.bind(this),
      "server-print": this.serverPrint.bind(this),
      "server-download": this.serverDownload.bind(this),
      "fill": this.fill.bind(this),
      "update-controller": this.updateController.bind(this),
      "select": this.select.bind(this),
      "cancel": this.cancel.bind(this),
      "confirm-updated-data": this.checkModelUpdated.bind(this),
      "confirm-not-updated-data": this.checkModelNoUpdated.bind(this),
      "confirm-empty-data": this.checkModelEmpty.bind(this),
      "value": this.value.bind(this),
      "filter": this.filter.bind(this),
      "start-load": this.startLoad.bind(this),
      "end-load": this.endLoad.bind(this),
      "keep": this.keep.bind(this)
    };
  }

  /**
   * Validate the form
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  validate(action, props) {
    // Validate all selected components
    const address = getActionAddress(action);
    props.validateComponents(Object.values(props.components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, props.components))));

    // Accept the action
    props.acceptAction(action);
  }

  /**
   * Validate the form
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  submit(action, props) {
    // Validate all selected components
    const address = getActionAddress(action);
    const submitButton = Object.values(props.components)
      .find(component => component.attributes.buttonType === BUTTON_SUBMIT &&
        isInsideContext(component.context, address.view, getActionSource(action, props.components)));

    if (submitButton) {
      props.addActionsTop([{type: "click", address: submitButton.address, target: submitButton.address.component}]);
    }

    // Accept the action
    props.acceptAction(action);
  }

  /**
   * Verify a validation
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  verifyValidation(action, props) {
    // Check if validation has been successful
    if (Object.values(props.components).filter(component => component.attributes.error).length) {
      // If there are errors, reject action
      props.rejectAction(action);
    } else {
      // If is ok, accept the action
      props.acceptAction(action);
    }
  }

  /**
   * Set a criterion as valid
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  setValid(action, props) {
    // Update error attribute
    const address = getActionAddress(action);
    props.updateAttributes(address, {error: null});

    // Accept the current action
    props.acceptAction(action);
  }

  /**
   * Set a criterion as invalid
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  setInvalid(action, props) {
    // Update error attribute
    const address = getActionAddress(action);
    props.updateAttributes(address, {error: action.parameters});

    // Accept the current action
    props.acceptAction(action);
  }

  /**
   * Retrieve parameters and send them to the server
   * @param {Object} action Action received
   * @param {Object} props Properties
   */
  server(action, props) {
    // Launch server action with form values
    this.serverService.callServer(action, getFormValues(props), props);
  }

  /**
   * Retrieve parameters and send them to the server for printing actions (send images and text)
   * @param {Object} action Action received
   * @param {Object} props Properties
   */
  serverPrint(action, props) {
    // Launch server action for printing
    this.serverService.callServer(action, getFormValuesForPrinting(props), props);
  }

  /**
   * Retrieve parameters and send them to the server for printing actions (send images and text)
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  serverDownload(action, props) {
    // Get component
    const address = getActionAddress(action);
    let component = getComponent(props.components, address);
    const {specificAttributes} = component;
    const {token} = props.settings;

    // Store parameters
    let parameters = {
      ...action.parameters,
      ...getFormValues(props),
      ...(specificAttributes || {})
    };

    // Get target action
    let targetAction = parameters[props.settings.targetActionKey];

    // Generate url parameter
    fetchFile(getRestUrl("file", "download", "maintain", targetAction), {...parameters, d: downloadFormIdentifier++}, token)
      .then(() => props.acceptAction(action));
  }

  /**
   * Update model with action values
   * @param {Action} action Action received
   * @param {Object} props Properties
   */
  fill(action, props) {
    // Retrieve parameters
    const {parameters} = action;
    const address = getActionAddress(action);
    const {datalist} = parameters;

    // Generate model
    let model = {...datalist, values: [...datalist.rows]};
    delete model.rows;

    // Publish model change
    props.updateModelWithDependencies(address, model);

    // Finish action
    props.acceptAction(action);
  }

  /**
   * Update controller with action values
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  updateController(action, props) {
    // Get values
    const address = getActionAddress(action);
    const values = [...((action.parameters.datalist || {}).rows || [{}])];

    // Change controller
    props.updateAttributes(address, {[action.parameters.attribute]: action.parameters.value || values[0].value});

    // Finish action
    props.acceptAction(action);
  }

  /**
   * Update model with action values
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  select(action, props) {
    // Retrieve parameters
    const address = getActionAddress(action);
    let values = [...action.parameters.values];

    // Call the method update selected value from API
    props.updateModelWithDependencies(address, {selected: values});

    // Finish action
    props.acceptAction(action);
  }

  /**
   * Reset view selected values
   * @param {object} action
   * @param {Object} props Properties
   */
  reset(action, props) {
    // Check reset target
    const address = getActionAddress(action);
    props.resetMultipleModelWithDependencies(Object.values(props.components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, props.components))));

    // Finish action
    props.acceptAction(action);
  }

  /**
   * Restore view selected values
   * @param {object} action
   * @param {Object} props Properties
   */
  restore(action, props) {
    // Check reset target
    const address = getActionAddress(action);
    props.restoreMultipleModelWithDependencies(Object.values(props.components)
      .filter(component => isInsideContext(component.context, address.view, getActionSource(action, props.components))));

    // Finish action
    props.acceptAction(action);
  }

  /**
   * Check if model has been modified
   * @param {object} action
   * @param {Object} props Properties
   */
  checkModelUpdated(action, props) {
    // Define server and target action
    const address = getActionAddress(action);

    // If model has not changed
    if (checkModelIsUpdated(props)) {
      const values = {
        title: 'CONFIRM_TITLE_UPDATED_DATA',
        message: 'CONFIRM_MESSAGE_UPDATED_DATA'
      };
      // Generate server action
      let confirmAction = generateServerAction(values, "confirm", null, address, false, false, props.settings);

      // Send action list
      props.addActionsTop([confirmAction]);
    }

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Check if model hasn't been modified
   * @param {object} action
   * @param {Object} props Properties
   */
  checkModelNoUpdated(action, props) {
    // Define server and target action
    const address = getActionAddress(action);

    // If model has not changed
    if (checkModelIsUnchanged(props)) {
      const values = {
        title: 'CONFIRM_TITLE_NOT_UPDATED_DATA',
        message: 'CONFIRM_MESSAGE_NOT_UPDATED_DATA'
      };
      // Generate server action
      let confirmAction = generateServerAction(values, "confirm", null, address, false, false, props.settings);

      // Send action list
      props.addActionsTop([confirmAction]);
    }

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Check if model has empty data
   * @param {object} action
   * @param {Object} props Properties
   */
  checkModelEmpty(action, props) {
    // Define server and target action
    const address = getActionAddress(action);

    // If model is empty, launch confirm screen
    if (checkModelIsEmpty(props)) {
        const values = {
          title: 'CONFIRM_TITLE_EMPTY_DATA',
          message: 'CONFIRM_MESSAGE_EMPTY_DATA'
        };
        // Generate server action
        let confirmAction = generateServerAction(values, "confirm", null, address, false, false, props.settings);

        // Send action list
        props.addActionsTop([confirmAction]);
    }

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Set a static value for an element
   * @param {object} action
   * @param {Object} props Properties
   */
  value(action, props) {
    // Retrieve parameters
    action.parameters.values = asArray(action.value);
    this.select(action, props);
  }

  /**
   * Cancel all actions of the current stack
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  cancel(action, props) {
    props.deleteStack();
  }

  /**
   * Filter a component data
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  filter(action, props) {
    // Define server and target action
    const {serverActionKey, targetActionKey} = props.settings;
    const address = getActionAddress(action);

    // Start loading
    let component = getComponent(props.components, address);

    // Add action to actions stack
    const serverAction = component.attributes[serverActionKey] || "data";
    const targetAction = component.attributes[targetActionKey];
    let values = {
      ...getFormValues(props),
      ...(component.specificAttributes || {})
    };

    // Generate server action
    let filterAction = generateServerAction(values, serverAction, targetAction, address, action.async, action.silent, props.settings);

    // Send action list
    props.addActionsTop([filterAction]);

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Start loading
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  startLoad(action, props) {
    // Start loading
    const address = getActionAddress(action);
    props.updateAttributes(address, {loading: true});

    // Accept action
    props.acceptAction(action);
  }

  /**
   * Finish loading
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  endLoad(action, props) {
    // Start loading
    const address = getActionAddress(action);
    props.updateAttributes(address, {loading: false});

    // Close action
    props.acceptAction(action);
  }

  /**
   * Keep criteria values after initial initialization
   * @param {object} action Action received
   * @param {Object} props Properties
   */
  keep(action, props) {
    // Start loading
    const address = getActionAddress(action);

    // Keep model
    props.keepModel(address);

    // Close action
    props.acceptAction(action);
  }
}

export default FormService;
