import React from "react";
import {InputText} from 'primereact/inputtext';
import {connectComponent} from "../components/AweComponent";
import AweTextComponent from "./AweTextComponent";
import {bindMethods, generateMessageAction, getContextPath, getRestUrl, getSizeString} from "../utilities";
import {ProgressBar} from "primereact/progressbar";
import {Button} from "primereact/button";
import {FileUpload} from "primereact/fileupload";
import "./AweInputUploader.less";
import {classNames, deleteFile, getInitialFileData, uploadFile, UploadStatus} from "../utilities/components";

const {INITIAL, UPLOADING, UPLOADED} = UploadStatus;

class AweInputUploader extends AweTextComponent {

  constructor(props) {
    super(props);
    bindMethods(this, ["getComponent", "onStartUpload", "onDelete", "onProgress", "onError", "onUpload"]);
    this.state = {progress: 0};
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    super.componentDidMount();
    getInitialFileData(this, this.props.model.values.filter(v => v.selected).map(v => v.value).join(""));
  }

  getValue() {
    return this.props.model.values.filter(v => v.selected).map(v => `${v.label} (${getSizeString(v.size)})`.trim()).join(", ");
  }

  onStartUpload(e) {
    uploadFile(this, e);
  }

  onDelete() {
    deleteFile(this, this.props.model.values.filter(v => v.selected).map(v => v.value).join(""));
  }

  onProgress(e) {
    const {loaded, total} = e.originalEvent;
    this.setState({progress: Math.floor(loaded / total * 100)});
  }

  onUpload() {
    this.setState({status: UPLOADED});
  }

  onError(e) {
    const {t, addActionsTop} = this.props;
    addActionsTop([generateMessageAction("error", t('ERROR_TITLE_FILE_UPLOAD'), JSON.parse(e.xhr.response).message)]);
    this.setState({status: INITIAL});
  }

  getComponent(style) {
    const {t, address, attributes, settings} = this.props;
    const {placeholder, readonly, size} = attributes;
    const {uploadMaxSize} = settings;
    const {status} = this.state;

    return <>
      <InputText
        className={classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style, {[`hidden`]: status === UPLOADING})}
        value={this.getValue()}
        placeholder={t(placeholder)}
        disabled={readonly}
        readOnly={true}
      />
      {status === UPLOADING && <ProgressBar
        style={{width: "100%"}}
        className={"mt-2"}
        value={this.state.progress || 0}/>}
      <FileUpload
        auto
        className={classNames({[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style, {[`hidden`]: status !== INITIAL})}
        id={address.component}
        mode="basic"
        name="file"
        url={getContextPath() + getRestUrl("file", "upload")}
        maxFileSize={uploadMaxSize * 1024 * 1024}
        onBeforeSend={this.onStartUpload}
        onProgress={this.onProgress}
        onError={this.onError}
        onUpload={this.onUpload}
        disabled={readonly}
        withcredentials={"true"}
        chooseLabel={t("BUTTON_CHOOSE")}
      />
      <Button
        className={classNames("p-button-secondary", {[`text-${size}`]: size, [`p-inputtext-${size}`]: size}, style, {[`hidden`]: status !== UPLOADED})}
        type="button"
        icon={"pi pi-times"}
        label={t("BUTTON_CLEAR")}
        onClick={this.onDelete}
        disabled={readonly}
      />
    </>;
  }
}

export default connectComponent(AweInputUploader);
