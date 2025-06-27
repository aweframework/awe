import React, {Component} from "react";
import {InputText} from 'primereact/inputtext';
import {withTranslation} from "react-i18next";
import {
  bindMethods,
  formatMessage,
  generateMessageAction,
  getContextPath,
  getRestUrl,
  getSizeString, translateLabel
} from "../utilities";
import {ProgressBar} from "primereact/progressbar";
import {Button} from "primereact/button";
import {FileUpload} from "primereact/fileupload";
import {classNames, deleteFile, getInitialFileData, uploadFile, UploadStatus} from "../utilities/components";
import PropTypes from "prop-types";

const {INITIAL, UPLOADING, UPLOADED} = UploadStatus;

class ColumnUploader extends Component {

  constructor(props) {
    super(props);

    // Bind events
    bindMethods(this, ["onStartUpload", "onDelete", "onProgress", "onError", "onUpload"]);

    this.state = {value: this.props.data.value, progress: 0};
  }

  /**
   * Component was mounted
   */
  componentDidMount() {
    getInitialFileData(this, this.props.data.value);
  }

  getValue() {
    const {label, size, value} = this.props.data;
    return value ? `${label} (${getSizeString(size)})` : '';
  }

  onStartUpload(e) {
    uploadFile(this, e);
  }

  onDelete() {
    deleteFile(this, this.props.data.value);
  }

  onProgress(e) {
    const {loaded, total} = e.originalEvent;
    this.setState({progress: Math.floor(loaded / total * 100)});
  }

  onError(e) {
    const {t, addActionsTop} = this.props;
    addActionsTop([generateMessageAction("error", t('ERROR_TITLE_FILE_UPLOAD'), JSON.parse(e.xhr.response).message)]);
    this.setState({status: INITIAL});
  }

  onUpload() {
    this.setState({status: UPLOADED});
  }

  render() {
    const {t, address, placeholder, readonly, settings, data, style} = this.props;
    const {uploadMaxSize} = settings;
    const {progress, status} = this.state;
    const classes = classNames("p-inputgroup", "column-editor", {"p-invalid": data?.error}, style, data?.style);
    return <div className={classes}>
      <InputText
        className={classNames({"hidden": status === UPLOADING})}
        value={this.getValue()}
        placeholder={translateLabel(placeholder, t)}
        disabled={readonly}
        readOnly={true}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
      {status === UPLOADING && <ProgressBar
        style={{width: "100%"}}
        className={"mt-2"}
        value={progress || 0}/>}
      <FileUpload
        auto
        className={classNames({"hidden": status !== INITIAL})}
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
        chooseLabel={""}
      />
      <Button
        className={classNames("p-button-secondary", {"hidden": status !== UPLOADED})}
        type="button"
        icon={"pi pi-times"}
        label={""}
        onClick={this.onDelete}
        disabled={readonly}
      />
    </div>;
  }
}

ColumnUploader.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  t: PropTypes.func.isRequired,
  style: PropTypes.string,
  label: PropTypes.string,
  readonly: PropTypes.bool,
  placeholder: PropTypes.string,
  settings: PropTypes.object,
  addActionsTop: PropTypes.func.isRequired,
};

export default withTranslation()(ColumnUploader);
