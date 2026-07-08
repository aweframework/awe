package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.file.FileUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Manage per-user settings, currently limited to the user's avatar image.
 */
@Slf4j
public class UserSettingsService extends ServiceConfig {

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;
  private final FileService fileService;

  private static final String AVATAR_TOKEN_QUERY = "getAvatarToken";
  private static final String AVATAR_TOKEN_FIELD = "avatarToken";
  private static final long AVATAR_MAX_FILE_SIZE = 2L * 1024 * 1024;
  // WebP is intentionally excluded: stock JDK 17 ImageIO has no WebP reader (no
  // TwelveMonkeys/WebP plugin on the classpath), so validateAvatarContentIsImage would always
  // decode null for a real WebP file and reject every legitimate upload. Supporting WebP would
  // require adding a WebP ImageIO plugin (e.g. TwelveMonkeys) as a future enhancement.
  private static final List<String> AVATAR_ALLOWED_MIME_TYPES = Arrays.asList(
    "image/png", "image/jpeg", "image/gif");

  private static final String ERROR_TITLE_INVALID_AVATAR_MIME_TYPE = "ERROR_TITLE_INVALID_AVATAR_MIME_TYPE";
  private static final String ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE = "ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE";
  private static final String ERROR_TITLE_AVATAR_TOO_LARGE = "ERROR_TITLE_AVATAR_TOO_LARGE";
  private static final String ERROR_MESSAGE_AVATAR_TOO_LARGE = "ERROR_MESSAGE_AVATAR_TOO_LARGE";

  // The home screen's avatar button lives in the "base" view (sidebar/navbar), independent of
  // the "report" view where the user-settings screen that triggers the save runs.
  private static final String AVATAR_BUTTON_VIEW = "base";
  private static final String AVATAR_BUTTON_COMPONENT = "ButUsrAct";

  /**
   * Autowired constructor
   *
   * @param queryService    Query service
   * @param queryUtil       Query utilities
   * @param maintainService Maintain service
   * @param fileService     File service
   */
  public UserSettingsService(QueryService queryService, QueryUtil queryUtil, MaintainService maintainService, FileService fileService) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
    this.fileService = fileService;
  }

  /**
   * Claim a staged avatar image for the current session user. The image must have already been
   * staged via the public {@code /file/upload} endpoint (stage-then-claim model); this method
   * decodes the staged {@link FileData} token, validates it (MIME allow-list, magic-byte image
   * decode, size ceiling), and on success delegates to the existing {@link #upsertUserAvatar}. On
   * validation failure the staged file is deleted (it is already on disk from the staging step,
   * unlike a rejected {@code MultipartFile} which is never persisted) and a localized
   * {@link AWException} is thrown.
   *
   * @param avatarToken Staged file reference token (the uploader criterion's value after staging)
   * @return Service data carrying a client action that reloads the home view's avatar
   * @throws AWException Rejected claim (invalid MIME type, content, or oversized) or storage error
   */
  public ServiceData saveUserAvatar(String avatarToken) throws AWException {
    FileData stagedFileData = FileUtil.stringToFileData(avatarToken);

    // MUST run before the validate-and-delete-orphan-on-failure block below, and MUST NOT be
    // inside that try/catch: on a forged token the catch calls fileService.deleteFile(staged),
    // which on a path-traversal payload would delete the arbitrary target file instead of a
    // legitimately staged one. Rejecting here first means deleteFile is never invoked at all for
    // a forged token.
    if (!fileService.isPathWithinUploadArea(stagedFileData)) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_AVATAR_MIME_TYPE), getLocale(ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE));
    }

    try {
      validateAvatarMimeType(stagedFileData);
      validateAvatarContentIsImage(stagedFileData);
      validateAvatarSize(stagedFileData);
    } catch (AWException exc) {
      fileService.deleteFile(stagedFileData);
      throw exc;
    }

    String user = getSession().getUser();
    Optional<String> previousToken = getStoredAvatarToken(user);

    upsertUserAvatar(user, avatarToken);

    previousToken.ifPresent(this::deletePreviousAvatarFile);

    // The avatar shown in the home sidebar/navbar (ButUsrAct) lives in a different view ("base")
    // than this maintain runs in (the user-settings modal, view "report"), so a plain grid/component
    // refresh in the caller's own view would never reach it. Addressing the client action explicitly
    // to the "base" view's ButUsrAct makes the home avatar reload after every successful save.
    return new ServiceData().addClientAction(new ClientAction("filter")
      .setAddress(new ComponentAddress(AVATAR_BUTTON_VIEW, AVATAR_BUTTON_COMPONENT, null, null)));
  }

  /**
   * Retrieve the avatar file data for the current session user, if any.
   *
   * @return File data, or empty if the user has no stored avatar
   * @throws AWException Error decoding the stored avatar token
   */
  public Optional<FileData> getAvatarForCurrentUser() throws AWException {
    String user = getSession().getUser();
    Optional<String> token = getStoredAvatarToken(user);

    if (token.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(FileUtil.stringToFileData(token.get()));
  }

  /**
   * Validate the MIME type of a staged avatar file against the allow-list. The content type was
   * already resolved server-side at staging time by {@code FileService.uploadFile} (via
   * {@code FileUtil.extractContentType}), so it carries the same trust level a client-declared
   * MIME type would have had.
   *
   * @param fileData Staged file data
   * @throws AWException Rejected claim (missing or disallowed MIME type)
   */
  private void validateAvatarMimeType(FileData fileData) throws AWException {
    String mimeType = fileData.getMimeType();
    if (mimeType == null || !AVATAR_ALLOWED_MIME_TYPES.contains(mimeType)) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_AVATAR_MIME_TYPE), getLocale(ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE));
    }
  }

  /**
   * Validate that a staged avatar file's bytes actually decode as an image, instead of trusting
   * only the staged MIME type (which {@link #validateAvatarMimeType(FileData)} checks). Reads the
   * staged bytes from disk via the resolved upload path.
   *
   * @param fileData Staged file data
   * @throws AWException Rejected claim (declared MIME type does not match actual content)
   */
  private void validateAvatarContentIsImage(FileData fileData) throws AWException {
    String fullPath = fileService.getFullPath(fileData, false) + fileData.getFileName();
    BufferedImage decoded;
    try (InputStream inputStream = new FileInputStream(fullPath)) {
      decoded = ImageIO.read(inputStream);
    } catch (Exception exc) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_AVATAR_MIME_TYPE), getLocale(ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE), exc);
    }

    if (decoded == null) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_AVATAR_MIME_TYPE), getLocale(ERROR_MESSAGE_INVALID_AVATAR_MIME_TYPE));
    }
  }

  /**
   * Validate the size of a staged avatar file against the avatar-specific size ceiling.
   * {@code /file/upload} only enforces the global multipart size limit, so the avatar-specific
   * 2MB ceiling must be re-applied here at claim time.
   *
   * @param fileData Staged file data
   * @throws AWException Rejected claim (exceeds the maximum avatar size)
   */
  private void validateAvatarSize(FileData fileData) throws AWException {
    Long fileSize = fileData.getFileSize();
    if (fileSize != null && fileSize > AVATAR_MAX_FILE_SIZE) {
      throw new AWException(getLocale(ERROR_TITLE_AVATAR_TOO_LARGE), getLocale(ERROR_MESSAGE_AVATAR_TOO_LARGE));
    }
  }

  /**
   * Delete a previously stored avatar file, logging and continuing on failure so that the
   * already-updated token is not rolled back because of an orphaned file.
   *
   * @param previousToken Previous avatar reference token
   */
  private void deletePreviousAvatarFile(String previousToken) {
    try {
      fileService.deleteFile(FileUtil.stringToFileData(previousToken));
    } catch (AWException exc) {
      log.warn("Could not delete the previous avatar file, it may remain orphaned on disk", exc);
    }
  }

  /**
   * Insert or update the AweUserSettings row for the given user with the given avatar token.
   * Inserts a new row if none exists for the user, or updates the existing row otherwise, so
   * that at most one row per user is ever kept.
   *
   * <p>The existence check is check-then-act (TOCTOU): between reading "no row exists" and
   * executing the insert, a concurrent request for the same user may win the race and insert
   * first. The {@code UNIQUE(Ope)} constraint on {@code AweUserSettings} keeps the data correct
   * in that case, but the losing insert would otherwise surface as an untranslated exception
   * (a raw constraint violation wrapped as {@link AWException} by the maintain layer). Instead of
   * letting that propagate as an ugly failure, retry the losing insert as an update: the row now
   * exists (the concurrent request created it), so updating it converges to the same end state
   * both concurrent uploads intended.
   *
   * @param user        Username (Ope)
   * @param avatarToken Avatar reference token (FileData encoded as string)
   * @return Service data
   * @throws AWException Error upserting the avatar token (after the update fallback also fails)
   */
  public ServiceData upsertUserAvatar(String user, String avatarToken) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put("user", user);
    parameters.put(AVATAR_TOKEN_FIELD, avatarToken);

    if (getStoredAvatarToken(user).isPresent()) {
      return maintainService.launchPrivateMaintain("updateUserAvatar", parameters);
    }

    try {
      return maintainService.launchPrivateMaintain("insertUserAvatar", parameters);
    } catch (AWException exc) {
      // Lost the insert race to a concurrent upload for the same user: the row now exists,
      // so converge by updating it instead of propagating the constraint-violation failure.
      log.warn("Insert of AweUserSettings for user '{}' failed, likely a concurrent insert race; retrying as update", user, exc);
      return maintainService.launchPrivateMaintain("updateUserAvatar", parameters);
    }
  }

  /**
   * Retrieve the stored avatar token for the given user, if any.
   *
   * @param user Username (Ope)
   * @return Avatar token, or empty if the user has no AweUserSettings row or no token stored
   * @throws AWException Error retrieving the avatar token
   */
  private Optional<String> getStoredAvatarToken(String user) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put("user", user);

    ServiceData serviceData = queryService.launchPrivateQuery(AVATAR_TOKEN_QUERY, parameters);
    DataList dataList = serviceData.getDataList();

    if (dataList == null || dataList.getRows() == null || dataList.getRows().isEmpty()) {
      return Optional.empty();
    }

    CellData tokenCell = dataList.getRows().get(0).get(AVATAR_TOKEN_FIELD);
    if (tokenCell == null) {
      return Optional.empty();
    }

    String token = tokenCell.getStringValue();
    return token.isEmpty() ? Optional.empty() : Optional.of(token);
  }
}
