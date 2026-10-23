import React, {useEffect, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {selectMessages, selectSettings} from "../redux/selectors/stateSelectors";
import {acceptAction, rejectAction} from "../redux/actions/actions";
import {removeConfirm, removeMessage, showMessages as showMessagesAction} from "../redux/actions/messages";

import "./MessageContainer.less";
import {Dialog} from "primereact/dialog";
import {ESC_KEY_HANDLING_PRIORITIES, useGlobalOnEscapeKey} from "primereact/hooks";
import {Button} from "primereact/button";
import {useTranslation} from "react-i18next";
import {Toast} from "primereact/toast";
import {translateLabel} from "../utilities";
import {TestIds, testHook} from "../utilities/testIds";
import {toastPassThrough} from "../utilities/testPassThrough";
import AweTargetMessage from "../components/AweTargetMessage";

const NO_MESSAGES = {};
const NO_SETTINGS = {};
// The confirm is modal and shown over everything: Escape cancels it before closing any overlay below it (the menu that
// launched the action, for instance)
const CONFIRM_ESC_KEY_PRIORITY = [ESC_KEY_HANDLING_PRIORITIES.TOOLTIP + 100, 0];

/**
 * Message container
 * @category Containers
 */
function MessageContainer() {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const messages = useSelector(selectMessages) ?? NO_MESSAGES;
  const settings = useSelector(selectSettings) ?? NO_SETTINGS;
  const { confirm, popover } = messages;
  const toastRef = useRef(null);

  const onClick = (data) => {
    const {message = {}} = data;
    toastRef.current?.remove?.(message);
  };

  const onRemove = (message) => {
    dispatch(acceptAction(message.action));
    dispatch(removeMessage(message));
  };

  const onAccept = () => {
    if (confirm) {
      dispatch(acceptAction(confirm.action));
      dispatch(removeConfirm());
    }
  };

  const onCancel = () => {
    if (confirm) {
      dispatch(rejectAction(confirm.action));
      dispatch(removeConfirm());
    }
  };

  const confirmFooter =() => {
    return <div className={"flex justify-content-between flex-wrap"}>
      <Button id="confirm-cancel" label={translateLabel("BUTTON_CANCEL", t)} icon="pi pi-times" onClick={onCancel} className="p-button-secondary"
              {...testHook(TestIds.confirmCancel)}/>
      <Button id="confirm-accept" label={translateLabel("BUTTON_ACCEPT", t)} icon="pi pi-check" onClick={onAccept}
              {...testHook(TestIds.confirmAccept)}/>
    </div>;
  };

  useEffect(() => {
    if (toastRef.current && messages.showing && messages.showing.length > 0) {
      const newMessages = messages.showing.filter(msg => !msg.show);
      if (newMessages.length > 0) {
        toastRef.current.show(newMessages);
        dispatch(showMessagesAction(newMessages));
      }
    }
  }, [dispatch, messages.showing]);


  const {title, message, visible = true} = confirm ?? {visible: false};

  // Escape cancels the confirm, as the Cancel button does
  useGlobalOnEscapeKey({callback: onCancel, when: visible, priority: CONFIRM_ESC_KEY_PRIORITY});

  return <>
    <Toast ref={toastRef} onClick={onClick} onRemove={onRemove} position={settings.messagePosition} pt={toastPassThrough()}/>
    <AweTargetMessage popover={popover}/>
    <Dialog visible={visible} header={translateLabel(title, t)} footer={confirmFooter()} focusOnShow={false}
            modal={true} closable={false} closeOnEscape={false} onHide={onCancel}
            pt={{root: testHook(TestIds.confirmDialog)}}>
      <i className="pi pi-exclamation-triangle m-3 text-center text-warning" style={{fontSize: '8rem'}}/>
      <span>{translateLabel(message, t)}</span>
    </Dialog>
  </>;
}

export default MessageContainer;
