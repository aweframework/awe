import React, {useCallback, useEffect, useRef, useState} from "react";
import {InputText} from 'primereact/inputtext';
import {generateMessageAction, getContextPath, getIconCode, getRestUrl, getSizeString, translateLabel} from "../utilities";
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
import PropTypes from "prop-types";
import { TestIds, testHook } from "../utilities/testIds";

const {INITIAL, UPLOADING, UPLOADED} = UploadStatus;

// Raster images that can be previewed safely (SVG is excluded because it can carry scripts)
const PREVIEWABLE_IMAGES = /^image\/(png|jpe?g|gif|webp|bmp|avif)$/i;
const CAPTURE_MODES = new Set(["user", "environment"]);

function AweInputUploader(props) {
  const { id } = props;
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const { address, model = { values: [] }, attributes = {}, validationRules = {}, settings } = useText(id);

  const { placeholder, readonly, size, error, dropZone, capture } = attributes;
  const captureMode = CAPTURE_MODES.has(capture) ? capture : null;
  const upload = useUpload({ address, destination: attributes?.destination });
  const { status, setStatus, progress, setProgress, getInitialFileData, uploadFile, deleteFile } = upload;

  const fileUploadRef = useRef(null);
  const cameraInputRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const [preview, setPreview] = useState(null);

  // Revoke the object URL of the preview when it is replaced, cleared or the component unmounts
  useEffect(() => () => {
    if (preview) {
      URL.revokeObjectURL(preview.url);
    }
  }, [preview]);

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
  const onSelect = useCallback((e) => {
    const file = (e.files || [])[0];
    setPreview(file && PREVIEWABLE_IMAGES.test(file.type) ? { url: URL.createObjectURL(file), name: file.name } : null);
  }, []);
  const onDragEnter = useCallback((e) => {
    e.preventDefault();
    setDragging(true);
  }, []);
  const onDragLeave = useCallback(() => setDragging(false), []);
  const onDrop = useCallback((e) => {
    e.preventDefault();
    setDragging(false);
    if (status === INITIAL && !readonly && e.dataTransfer?.files?.length === 1) {
      fileUploadRef.current?.onFileSelect({ dataTransfer: e.dataTransfer });
    }
  }, [status, readonly]);
  const onOpenCamera = useCallback(() => cameraInputRef.current?.click(), []);
  const onCameraChange = useCallback((e) => {
    const { files } = e.target;
    if (files?.length) {
      fileUploadRef.current?.onFileSelect({ target: { files } });
    }
    e.target.value = "";
  }, []);
  const onDelete = useCallback(() => {
    setPreview(null);
    deleteFile((model.values || []).filter(v => v.selected).map(v => v.value).join(""));
  }, [model.values]);
  const onProgress = useCallback((e) => {
    const { loaded, total } = e.originalEvent || {};
    if (total) setProgress(Math.floor(loaded / total * 100));
  }, []);
  const onError = useCallback((e) => {
    dispatch(addActionsTop([generateMessageAction("error", translateLabel('ERROR_TITLE_FILE_UPLOAD', t), JSON.parse(e.xhr.response).message)]));
    setPreview(null);
    setStatus(INITIAL);
  }, []);
  const onUpload = useCallback(() => setStatus(UPLOADED), []);

  const { uploadMaxSize = 0 } = settings || {};

  const dropHandlers = dropZone ? {
    onDragEnter,
    onDragOver: onDragEnter,
    onDragLeave,
    onDrop
  } : {};

  // The drop zone only wraps the controls when enabled, so existing uploaders keep their markup
  const wrap = (content) => dropZone ? (
    <div
      className={classNames("upload-drop-zone", { "upload-drop-zone-active": dragging && status === INITIAL && !readonly })}
      {...testHook(TestIds.uploadDropZone)}
      {...dropHandlers}>
      {content}
    </div>
  ) : content;

  return (
    <AweCriterion address={address} attributes={attributes} validationRules={validationRules}>
      {wrap(<>
        <InputText
          className={classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status === UPLOADING, "p-invalid": error })}
          value={getValue()}
          placeholder={translateLabel(placeholder, t)}
          disabled={readonly}
          readOnly={true}
          {...testHook(TestIds.uploadFilename)}
        />
        {status === UPLOADING && (
          <ProgressBar style={{ width: "100%" }} className={"mt-2"} value={progress || 0} />
        )}
        {dropZone && status === INITIAL && !readonly && (
          <div className="upload-drop-hint">{translateLabel("UPLOADER_DROP_HINT", t)}</div>
        )}
        <FileUpload
          ref={fileUploadRef}
          auto
          className={classNames({ [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status !== INITIAL, "p-invalid": error })}
          id={address?.component}
          mode="basic"
          name="file"
          url={getContextPath() + getRestUrl("file", "upload")}
          maxFileSize={(uploadMaxSize || 0) * 1024 * 1024}
          onBeforeSend={onStartUpload}
          onSelect={onSelect}
          onProgress={onProgress}
          onError={onError}
          onUpload={onUpload}
          disabled={readonly}
          withcredentials={"true"}
          chooseLabel={translateLabel("BUTTON_CHOOSE", t)}
          pt={{ input: testHook(TestIds.criterionInput) }}
        />
        {captureMode && (
          <>
            <Button
              className={classNames("p-button-secondary", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status !== INITIAL })}
              type="button"
              icon={getIconCode("pi-camera", "p-button-icon p-c p-button-icon-left")}
              label={translateLabel("BUTTON_CAPTURE", t)}
              onClick={onOpenCamera}
              disabled={readonly}
              {...testHook(TestIds.uploadCapture)}
            />
            <input
              ref={cameraInputRef}
              type="file"
              accept="image/*"
              capture={captureMode}
              className="hidden"
              tabIndex={-1}
              aria-hidden="true"
              disabled={readonly}
              onChange={onCameraChange}
            />
          </>
        )}
        {preview && (
          <img className="upload-preview" src={preview.url} alt={preview.name} {...testHook(TestIds.uploadPreview)} />
        )}
        <Button
          className={classNames("p-button-secondary", { [`text-${size}`]: size, [`p-inputtext-${size}`]: size, [`hidden`]: status !== UPLOADED, "p-invalid": error })}
          type="button"
          icon={getIconCode("pi-times", "p-button-icon p-c p-button-icon-left")}
          label={translateLabel("BUTTON_CLEAR", t)}
          onClick={onDelete}
          disabled={readonly}
          {...testHook(TestIds.uploadClear)}
        />
      </>)}
    </AweCriterion>
  );
}

AweInputUploader.propTypes = {
  id: PropTypes.string,
};

export default AweInputUploader;
