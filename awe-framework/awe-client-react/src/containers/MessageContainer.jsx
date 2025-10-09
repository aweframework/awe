import React, {useEffect, useRef} from "react";
import {useDispatch, useSelector} from "react-redux";
import {acceptAction, rejectAction} from "../redux/actions/actions";
import {removeConfirm, removeMessage, showMessages as showMessagesAction} from "../redux/actions/messages";

import "./MessageContainer.less";
import {Dialog} from "primereact/dialog";
import {Button} from "primereact/button";
import {useTranslation} from "react-i18next";
import {Toast} from "primereact/toast";
import {translateLabel} from "../utilities";

/**
 * Message container
 * @category Containers
 */
function MessageContainer() {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { messages = [], settings = {} } = useSelector(state => ({
    messages: state.messages,
    settings: state.settings
  }));
  const { confirm } = messages;
  const messageRef = useRef(null);

  const onClick = (data) => {
    const {message = {}} = data;
    messageRef.current?.remove?.(message);
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
      <Button id="confirm-cancel" label={translateLabel("BUTTON_CANCEL", t)} icon="pi pi-times" onClick={onCancel} className="p-button-secondary"/>
      <Button id="confirm-accept" label={translateLabel("BUTTON_ACCEPT", t)} icon="pi pi-check" onClick={onAccept}/>
    </div>;
  };

  useEffect(() => {
    if (messages.showing.length > 0) {
      messageRef.current?.show?.(messages.showing);
    }
  }, [messages.showing]);

  const {title, message, visible = true} = confirm ?? {visible: false};
  return <>
    <Toast ref={messageRef} onClick={onClick} onRemove={onRemove} position={settings.messagePosition}/>
    <Dialog visible={visible} header={translateLabel(title, t)} footer={confirmFooter()} focusOnShow={false}
            modal={true} closable={false} closeOnEscape={false}>
      <i className="pi pi-exclamation-triangle m-3 text-center text-warning" style={{fontSize: '8rem'}}/>
      <span>{translateLabel(message, t)}</span>
    </Dialog>
  </>;
}

export default MessageContainer;
