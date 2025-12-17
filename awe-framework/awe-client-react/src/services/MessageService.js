import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {confirmAction, messageAction} from "../redux/thunks/messages";

/**
 * MessageService functional component
 */
const useMessageService = () => {
  const dispatch = useDispatch();
  const {t} = useTranslation();

  // Generate a new message
  const message = (action) => dispatch(messageAction(action, t));

  // Show modal confirm
  const confirm = (action) => dispatch(confirmAction(action, t));

  const getActions = () => ({ message, targetMessage: message, confirm });
  return {getActions};
};

export default useMessageService;
