import React, {useCallback, useEffect} from "react";
import {InputText} from 'primereact/inputtext';
import {
  generateMessageAction,
  getContextPath,
  getRestUrl,
  getSizeString,
  translateLabel
} from "../utilities";
import {ProgressBar} from "primereact/progressbar";
import {Button} from "primereact/button";
import {FileUpload} from "primereact/fileupload";
import "./AweInputUploader.less";
import {classNames, UploadStatus} from "../utilities/components";
import useUpload from "../hooks/useUpload";
import AweCriterion from "./AweCriterion";
import {useDispatch} from "react-redux";
import {useTranslation} from "react-i18next";
import {addActionsTop} from "../redux/actions/actions";
import useText from "../hooks/useText";

const {INITIAL, UPLOADING, UPLOADED} = UploadStatus;

function AweInputUploader(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { address, model = { values: [] }, attributes = {}, validationRules = {}, settings } = useText(id);

  const upload = useUpload({ address, destination: attributes?.destination });
  const { status, setStatus, progress, setProgress, getInitialFileData, uploadFile, deleteFile } = upload;

  useEffect(() => {
    getInitialFileData((model.values || []).filter(v => v.selected).map(v => v.value).join(""));
  }, []);

  const getValue = useCallback(() => {
    return (model.values || [])
      .filter(v => v.selected)
      .map(v => `${v.label} (${getSizeString(v.size)})`.trim())
      .join(", ");
  }, [model.values]);

  const onStartUpload = useCallback((e) => uploadFile(e), []);
  const onDelete = useCallback(() => deleteFile((model.values || []).filter(v => v.selected).map(v => v.value).join("")), [model.values]);
  const onProgress = useCallback((e) => {
    const { loaded, total } = e.originalEvent || {};
    if (total) setProgress(Math.floor(loaded / total * 100));
  }, []);
  const onError = useCallback((e) => {
    dispatch(addActionsTop([generateMessageAction("error", translateLabel('ERROR_TITLE_FILE_UPLOAD', t), JSON.parse(e.xhr.response).message)]));
    setStatus(INITIAL);
  }, []);
  const onUpload = useCallback(() => setStatus(UPLOADED), []);

  const { placeholder, readonly, size, error } = attributes;
  const { uploadMaxSize = 0 } = settings || {};

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      <InputText
        className={classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status === UPLOADING, "p-invalid": error })}
        value={getValue()}
        placeholder={translateLabel(placeholder, t)}
        disabled={readonly}
        readOnly={true}
      />
      {status === UPLOADING && (
        <ProgressBar style={{ width: "100%" }} className={"mt-2"} value={progress || 0} />
      )}
      <FileUpload
        auto
        className={classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status !== INITIAL, "p-invalid": error })}
        id={address?.component}
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
        chooseLabel={translateLabel("BUTTON_CHOOSE", t)}
      />
      <Button
        className={classNames("p-button-secondary", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status !== UPLOADED, "p-invalid": error })}
        type="button"
        icon={"pi pi-times"}
        label={translateLabel("BUTTON_CLEAR", t)}
        onClick={onDelete}
        disabled={readonly}
      />
    </AweCriterion>
  );
}

export default AweInputUploader;
