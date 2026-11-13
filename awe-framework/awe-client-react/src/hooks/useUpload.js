import {useCallback, useMemo, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import {addActionsTop} from "../redux/actions/actions";
import {generateServerAction, getCookie} from "../utilities";
import {UploadStatus} from "../utilities/components";
import {getUID} from "../redux/actions/settings";
import {isEmpty} from "../utilities/general";

/**
 * useUpload
 * Shared hook for file upload components. Extracted from utilities/components.js (getInitialFileData, uploadFile, deleteFile)
 * so it can be reused both in AweInputUploader and ColumnUploader.
 *
 * Usage:
 *   const { status, setStatus, progress, setProgress, getInitialFileData, uploadFile, deleteFile } = useUpload({ address, destination });
 */
export default function useUpload({ address, destination }) {
  const dispatch = useDispatch();
  const settings = useSelector(state => state.settings || {});

  const [status, setStatus] = useState(UploadStatus.INITIAL);
  const [progress, setProgress] = useState(0);

  const getInitialFileData = useCallback((filename) => {
    if (!isEmpty(filename)) {
      dispatch(addActionsTop([generateServerAction({ filename, destination }, "data", "getFileInfo", address, true, true, settings)]));
    }
    setStatus(isEmpty(filename) ? UploadStatus.INITIAL : UploadStatus.UPLOADED);
  }, [dispatch, address, destination, settings]);

  const onBeforeSend = useCallback((uploader) => {
    const { token, uploadIdentifier } = settings;
    // Set headers
    uploader.xhr.setRequestHeader("Authorization", token);
    uploader.xhr.setRequestHeader("X-XSRF-TOKEN", getCookie("XSRF-TOKEN"));

    // Set form data
    uploader.formData.set("address", JSON.stringify(address));
    uploader.formData.set(uploadIdentifier, getUID());
    uploader.formData.set("destination", destination || "");

    setStatus(UploadStatus.UPLOADING);
  }, [settings, address, destination]);

  const deleteFile = useCallback((filename) => {
    dispatch(addActionsTop([generateServerAction({ filename, destination }, "delete-file", null, address, true, true, settings)]));
    setProgress(0);
    setStatus(UploadStatus.INITIAL);
  }, [dispatch, address, destination, settings]);

  return useMemo(() => ({
    status,
    setStatus,
    progress,
    setProgress,
    getInitialFileData,
    uploadFile: onBeforeSend,
    deleteFile,
    settings
  }), [status, progress, getInitialFileData, onBeforeSend, deleteFile, settings]);
}
