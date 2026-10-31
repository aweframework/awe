/**
 * Call a maintain target that keeps a preference of the user (the grid layout, the state of the advanced block of a
 * window). The user is never sent: the server takes it from the session.
 * The request parameter names are specific on purpose: the values of the screen criteria are added to the call.
 * It is silent (no loading bar) and uses maintain-silent, which shows no message when it works, only warnings and
 * errors, so saving and restoring feel immediate.
 * The call ends when the server answers (the action is accepted, after its answer actions are queued), the action
 * is rejected or aborted, or the call fails (onError, as Ajax.manageError tells the action before the error message).
 * The service worked when the action queued after the call runs: maintain-silent answers a warning or an error
 * with a cancel, which deletes the actions left in the stack, and a failed call empties it.
 *
 * @param {{$actionController: Object, $settings: Object}} services Action controller and AWE settings services
 * @param {string} target Maintain target
 * @param {object} values Service parameters
 * @param {function} onEnd Called once when the call ends
 * @param {function} [onSuccess] Called when the service worked
 */
export function launchSilentService({$actionController, $settings}, target, values, onEnd, onSuccess) {
  let parameters = {...values};
  parameters[$settings.get("serverActionKey")] = "maintain-silent";
  parameters[$settings.get("targetActionKey")] = target;
  const addressData = {address: {}, context: ""};
  const action = $actionController.generateAction({type: 'server', async: false, silent: true, parameters},
    addressData, true, false);
  let ended = false;
  const end = () => {
    if (!ended) {
      ended = true;
      onEnd();
    }
  };
  action.attr("deferred").promise.then(end, end);
  action.onError = end;
  const actions = [action];
  if (onSuccess) {
    // Silent action of no type that only tells that the service worked
    const worked = $actionController.generateAction({type: 'layout-saved', silent: true}, addressData, true, false);
    worked.launchAction = function () {
      onSuccess();
      this.accept();
    };
    actions.push(worked);
  }
  $actionController.addActionList(actions, true, addressData);
}
