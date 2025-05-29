import React, {Component} from "react";
import {connect} from "react-redux";
import {acceptAction, rejectAction} from "../redux/actions/actions";
import {removeConfirm, removeMessage, showMessages} from "../redux/actions/messages";

import "./MessageContainer.less";
import {Dialog} from "primereact/dialog";
import {Button} from "primereact/button";
import {withTranslation} from "react-i18next";
import {Toast} from "primereact/toast";
import PropTypes from "prop-types";
import {translateLabel} from "../utilities";

/**
 * Message container
 * @extends React.Component
 * @category Containers
 */
class MessageContainer extends Component {

  /**
   * Create the container
   * @param {object} props Container properties
   */
  constructor(props) {
    super(props);
    this.state = {confirm: null};

    this.onClick = this.onClick.bind(this);
    this.onRemove = this.onRemove.bind(this);
    this.showMessages = this.showMessages.bind(this);
    this.showConfirm = this.showConfirm.bind(this);
    this.checkMessages = this.checkMessages.bind(this);
    this.confirmFooter = this.confirmFooter.bind(this);
    this.onCancel = this.onCancel.bind(this);
    this.onAccept = this.onAccept.bind(this);
    this.onHideConfirm = this.onHideConfirm.bind(this);
  }

  onClick(data) {
    const {message = {}} = data;
    this.messages.remove(message);
  }

  onRemove(message) {
    this.props.acceptAction(message.action);
    this.props.removeMessage(message);
  }

  showMessages() {
    const newMessages = this.props.messages.showing.filter(message => !message.show);
    if (newMessages.length > 0) {
      this.messages.show(newMessages);
      this.props.showMessages(newMessages);
    }
  }

  showConfirm() {
    const {confirm} = this.props.messages;
    if (confirm && (!this.state.confirm || !this.state.confirm.visible)) {
      this.setState({confirm: {...confirm, visible: true}});
      this.props.removeConfirm();
    }
  }

  onAccept() {
    const {confirm} = this.state;
    if (confirm) {
      this.props.acceptAction(confirm.action);
      this.setState({confirm: {...confirm, visible: false}});
    }
  }

  onCancel() {
    const {confirm} = this.state;
    if (confirm) {
      this.props.rejectAction(confirm.action);
      this.setState({confirm: {...confirm, visible: false}});
    }
  }

  onHideConfirm() {
    setTimeout(() => this.setState({confirm: null}), 100);
  }

  confirmFooter() {
    const {t} = this.props;
    return <div className={"flex justify-content-between flex-wrap"}>
      <Button id="confirm-cancel" label={t("BUTTON_CANCEL")} icon="pi pi-times" onClick={this.onCancel} className="p-button-secondary"/>
      <Button id="confirm-accept" label={t("BUTTON_ACCEPT")} icon="pi pi-check" onClick={this.onAccept}/>
    </div>;
  }

  checkMessages() {
    this.showMessages();
    this.showConfirm();
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    this.checkMessages();
  }

  /**
   * Component was updated
   * @param {object} prevProps Previous props
   * @param {object} prevState Previous state
   * @param {object} snapshot Current snapshot
   */
  componentDidUpdate(prevProps, prevState, snapshot) {
    this.checkMessages();
  }

  /**
   * Render component
   * @returns {JSX.Element} Rendered component
   */
  render() {
    const {t, settings} = this.props;
    const {title, message, visible} = this.state.confirm || {visible: false};
    return <>
      <Toast ref={(el) => this.messages = el} onClick={this.onClick} onRemove={this.onRemove} position={settings.messagePosition}/>
      <Dialog visible={visible} header={translateLabel(title, t)} footer={this.confirmFooter()} focusOnShow={false}
              modal={true} closable={false} closeOnEscape={false} onHide={this.onHideConfirm}>
        <i className="pi pi-exclamation-triangle m-3 text-center text-warning" style={{fontSize: '8rem'}}/>
        <span>{translateLabel(message, t)}</span>
      </Dialog>
    </>;
  }
}

MessageContainer.propTypes = {
  messages: PropTypes.object.isRequired,
  settings: PropTypes.object.isRequired
};

function mapStateToProps(state) {
  return {
    messages: state.messages,
    settings: state.settings
  };
}

// Connect redux store updates
export default connect(mapStateToProps, {
  showMessages,
  removeMessage,
  removeConfirm,
  acceptAction,
  rejectAction
})(withTranslation()(MessageContainer));
