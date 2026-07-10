package com.almis.awe.controller;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.service.FileService;
import com.almis.awe.service.UserSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Serve the avatar image for the currently authenticated user. Resolution is strictly bound to
 * the session user: no client-supplied token or path parameter is ever accepted, so a caller can
 * only ever read their own avatar. Avatar upload does not go through this controller: it follows
 * AWE's stage-then-claim model (stage via the public {@code /file/upload}, claim via the
 * authenticated {@code saveUserAvatar} maintain / {@code UserSettingsService#saveUserAvatar}).
 *
 * <p>{@code /avatar} is intentionally NOT added to {@code SecurityEndpoints.authenticatedRequestMatchers}:
 * that matcher group is gated by {@code PublicQueryMaintainAuthorization}, a custom
 * {@code AuthorizationManager} that only grants access to requests matching its hardcoded
 * query/maintain URI allow-lists and otherwise hard-denies (see
 * {@code PublicQueryMaintainAuthorization#check}). A plain controller route like {@code /avatar}
 * matches neither list, so adding it there would make the endpoint return {@code 403} even for a
 * correctly authenticated user (verified empirically, see {@code AvatarSecurityIntegrationTest}
 * and the design doc's Decision 5 addendum). {@code /avatar} is correctly protected by the final
 * {@code .anyRequest().authenticated()} catch-all in {@code AweWebSecurityConfig#configureAuthorization},
 * which is a plain, unconditional authentication check. Do not "fix" this by adding a matcher.
 */
@RestController
@Slf4j
public class AvatarController extends ServiceConfig {

  // Autowired services
  private final UserSettingsService userSettingsService;
  private final FileService fileService;

  /**
   * Autowired constructor
   *
   * @param userSettingsService User settings service
   * @param fileService         File service
   */
  @Autowired
  public AvatarController(UserSettingsService userSettingsService, FileService fileService) {
    this.userSettingsService = userSettingsService;
    this.fileService = fileService;
  }

  /**
   * Retrieve the avatar image for the current session user.
   *
   * @return Avatar image bytes, or 404 if the user has no stored avatar
   */
  @GetMapping("/avatar")
  public ResponseEntity<FileSystemResource> getAvatar() {
    try {
      Optional<FileData> avatar = userSettingsService.getAvatarForCurrentUser();

      if (avatar.isEmpty()) {
        return ResponseEntity.notFound().build();
      }

      ResponseEntity<FileSystemResource> fileStream = fileService.getFileStream(avatar.get());
      return ResponseEntity.status(fileStream.getStatusCode())
        .headers(fileStream.getHeaders())
        .cacheControl(CacheControl.noCache())
        .body(fileStream.getBody());
    } catch (AWException exc) {
      // Never leak a 500 on a decode/storage failure: treat it the same as no avatar
      log.warn("Could not resolve the avatar for the current user", exc);
      return ResponseEntity.notFound().build();
    }
  }
}
