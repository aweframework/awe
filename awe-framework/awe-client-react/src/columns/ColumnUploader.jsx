import React, {useCallback, useEffect} from "react";
import {InputText} from 'primereact/inputtext';
import {useTranslation} from "react-i18next";
import {useDispatch, useSelector} from "react-redux";
import {
  formatMessage,
  generateMessageAction,
  getContextPath,
  getRestUrl,
  getSizeString, translateLabel
} from "../utilities";
import {ProgressBar} from "primereact/progressbar";
import {Button} from "primereact/button";
import {FileUpload} from "primereact/fileupload";
import {classNames, UploadStatus} from "../utilities/components";
import useUpload from "../hooks/useUpload";
import PropTypes from "prop-types";
import {addActionsTop} from "../redux/actions/actions";

const {INITIAL, UPLOADING, UPLOADED} = UploadStatus;

function ColumnUploader(props) {
  const { address, placeholder, readonly, data, style, destination } = props;
  const settings = useSelector(state => state.settings);
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const upload = useUpload({ address, destination });
  const { status, setStatus, progress, setProgress, getInitialFileData, uploadFile, deleteFile } = upload;

  useEffect(() => {
    getInitialFileData(data.value);
  }, []);

  const getValue = useCallback(() => {
    const {label, size, value} = data;
    return value ? `${label} (${getSizeString(size)})` : '';
  }, [data]);

  const onStartUpload = useCallback((e) => uploadFile(e), []);
  const onDelete = useCallback(() => deleteFile(data.value), [data.value]);
  const onProgress = useCallback((e) => {
    const {loaded, total} = e.originalEvent;
    if (total) setProgress(Math.floor(loaded / total * 100));
  }, []);
  const onError = useCallback((e) => {
    dispatch(addActionsTop([generateMessageAction("error", translateLabel('ERROR_TITLE_FILE_UPLOAD', t), JSON.parse(e.xhr.response).message)]));
    setStatus(INITIAL);
  }, []);
  const onUpload = useCallback(() => setStatus(UPLOADED), []);
  const {uploadMaxSize} = settings || {};
  const classes = classNames("p-inputgroup", "column-editor", {"p-invalid": data?.error}, style, data?.style);

  return (
    <div className={classes}>
      <InputText
        className={classNames({"hidden": status === UPLOADING})}
        value={getValue()}
        placeholder={translateLabel(placeholder, t)}
        disabled={readonly}
        readOnly={true}
        tooltip={formatMessage(data?.error, t)}
        tooltipOptions={{position: "bottom", className: "validation-tooltip"}}
      />
      {status === UPLOADING && (
        <ProgressBar
          style={{width: "100%"}}
          className={"mt-2"}
          value={progress || 0}
        />
      )}
      <FileUpload
        auto
        className={classNames({"hidden": status !== INITIAL})}
        id={address.component}
        mode="basic"
        name="file"
        url={getContextPath() + getRestUrl("file", "upload")}
        maxFileSize={(uploadMaxSize || 0) * 1024 * 1024}
        onBeforeSend={onStartUpload}
        onProgress={onProgress}
        onError={onError}
        onUpload={onUpload}
        disabled={readonly}
        withcredentials={"true"}
        chooseLabel={""}
      />
      <Button
        className={classNames("p-button-secondary", {"hidden": status !== UPLOADED})}
        type="button"
        icon={"pi pi-times"}
        label={""}
        onClick={onDelete}
        disabled={readonly}
      />
    </div>
  );
}

ColumnUploader.propTypes = {
  address: PropTypes.object.isRequired,
  data: PropTypes.object.isRequired,
  style: PropTypes.string,
  label: PropTypes.string,
  readonly: PropTypes.bool,
  placeholder: PropTypes.string,
  destination: PropTypes.string,
};

export default ColumnUploader;
