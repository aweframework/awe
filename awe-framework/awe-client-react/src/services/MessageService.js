import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {confirmAction, messageAction, targetMessageAction} from "../redux/thunks/messages";

/**
 * MessageService functional component
 */
const useMessageService = () => {
  const dispatch = useDispatch();
  const {t} = useTranslation();

  // Generate a new message
  const message = (action) => dispatch(messageAction(action, t));

  // Show a message over the target component of the action
  const targetMessage = (action) => dispatch(targetMessageAction(action, t));

  // Show modal confirm
  const confirm = (action) => dispatch(confirmAction(action, t));

  const getActions = () => ({ message, "target-message": targetMessage, confirm });
  return {getActions};
};

export default useMessageService;
