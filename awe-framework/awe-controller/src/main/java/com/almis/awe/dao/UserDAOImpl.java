package com.almis.awe.dao;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * User data implementation
 */
@Slf4j
public class UserDAOImpl extends ServiceConfig implements UserDAO {

  /** Maintain target that stores a rehashed password */
  public static final String REHASH_MAINTAIN = "UsrRehashPwd";
  /** Session parameter with the user whose password is rehashed (only set while the target runs) */
  public static final String REHASH_USER_SESSION_PARAMETER = "passwordRehashUser";
  /** Session parameter with the new password hash (only set while the target runs) */
  public static final String REHASH_VALUE_SESSION_PARAMETER = "passwordRehashValue";
  /** Parameter and column with the profile acronym */
  private static final String PROFILE = "profile";
  /** Column with the permission identifier */
  private static final String PERMISSION = "permission";
  /** Query with the acronym, the name and the restriction file of a profile by its id */
  private static final String PROFILE_BY_ID_QUERY = "access-subject-profile";

  // Query service
  private final QueryUtil queryUtil;
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param queryUtil            QueryUtil service
   * @param queryService         Query service
   */
  public UserDAOImpl(QueryUtil queryUtil, QueryService queryService) {
    this.queryUtil = queryUtil;
    this.queryService = queryService;
  }

  @Override
  public User findByUserName(String userName) {
    try {
      // Get user details from database
      return DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_QUERY, queryUtil.getParameters()
          .put("user", userName)).getDataList(), User.class)
        .stream()
        .findFirst()
        .orElseThrow(() -> new UsernameNotFoundException(userName));
    } catch (Exception exc) {
      log.error("Error retrieving user details", exc);
      throw new UsernameNotFoundException(userName, exc);
    }
  }

  public User findByEmail(String email) {
    User userInfo = null;
    try {
      // Get user details from database
      userInfo = DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_EMAIL_QUERY, queryUtil.getParameters()
              .put("email", email)).getDataList(), User.class)
          .stream()
          .findFirst()
          .orElse(null);
    } catch (Exception exc) {
      log.error("It has not been possible to recover the user details from email {}", email, exc);
    }
    return userInfo;
  }

  @Override
  public User findByRole(String profile) {
    try {
      // Get profile details from database
      return DataListUtil.asBeanList(queryService.launchPrivateQuery(AweConstants.USER_DETAIL_PROFILE_QUERY, queryUtil.getParameters()
              .put(PROFILE, profile)).getDataList(), User.class)
          .stream()
          .findFirst()
          .orElseThrow();
    } catch (Exception exc) {
      log.error("Error retrieving profile {} details. Check application configuration", profile);
      return new User();
    }
  }

  @Override
  public boolean existRole(String role) throws AWException {
    return queryService.launchPrivateQuery(AweConstants.CHECK_EXIST_PROFILE,
        queryUtil.getParameters().put(PROFILE, role)).getDataList().getRecords() == 1;
  }

  @Override
  public Optional<Integer> findProfileId(String profile) throws AWException {
    List<Map<String, CellData>> rows = queryService.launchPrivateQuery(AweConstants.CHECK_EXIST_PROFILE,
      queryUtil.getParameters().put(PROFILE, profile)).getDataList().getRows();
    return rows.size() == 1 ? Optional.ofNullable(rows.getFirst().get("IdePro")).map(CellData::getIntegerValue)
      : Optional.empty();
  }

  @Override
  public Optional<UserProfile> findProfile(String profile) throws AWException {
    // A failed lookup is thrown, never read as a missing profile: the token would get the default profile instead
    Optional<Integer> profileId = findProfileId(profile);
    if (profileId.isEmpty()) {
      return Optional.empty();
    }
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put(PROFILE, profileId.get().intValue());
    return queryService.launchPrivateQuery(PROFILE_BY_ID_QUERY, parameters).getDataList().getRows().stream()
      .filter(row -> value(row, PROFILE) != null)
      .map(row -> new UserProfile(value(row, PROFILE), value(row, "name"), value(row, "restriction")))
      .findFirst();
  }

  @Override
  public boolean supportsAdditionalProfiles() {
    return true;
  }

  @Override
  public List<UserProfile> findAdditionalProfiles(String userName) {
    try {
      DataList profiles = queryService.launchPrivateQuery(AweConstants.USER_ADDITIONAL_PROFILES_QUERY,
        unpaginatedParameters().put("user", userName)).getDataList();
      return profiles.getRows().stream()
        .map(row -> new UserProfile(value(row, PROFILE), value(row, "profileName"), value(row, "profileRestriction")))
        .toList();
    } catch (Exception exc) {
      // Without its additional profiles the user only gets the access of the main profile
      log.error("The additional profiles of the user could not be retrieved", exc);
      return Collections.emptyList();
    }
  }

  @Override
  public List<String> findPermissions(List<String> profileAcronyms) {
    if (profileAcronyms == null || profileAcronyms.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      ObjectNode parameters = unpaginatedParameters();
      ArrayNode profiles = parameters.putArray("profiles");
      profileAcronyms.forEach(profiles::add);
      return queryService.launchPrivateQuery(AweConstants.PROFILE_PERMISSIONS_QUERY, parameters).getDataList().getRows()
        .stream()
        .map(row -> value(row, PERMISSION))
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    } catch (Exception exc) {
      // Without its permissions the user is granted nothing that requires one
      log.error("The permissions of the profiles could not be retrieved", exc);
      return Collections.emptyList();
    }
  }

  @Override
  public Map<String, String> findProfileRestrictions(List<String> profileAcronyms) {
    if (profileAcronyms == null || profileAcronyms.isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      ObjectNode parameters = queryUtil.getParameters();
      ArrayNode profiles = parameters.putArray("profiles");
      profileAcronyms.forEach(profiles::add);
      Map<String, String> restrictions = new HashMap<>();
      queryService.launchPrivateQuery(AweConstants.PROFILE_RESTRICTIONS_QUERY, parameters).getDataList().getRows()
        .stream()
        .filter(row -> value(row, PROFILE) != null)
        .forEach(row -> restrictions.put(value(row, PROFILE),
          Optional.ofNullable(value(row, "restriction")).filter(file -> !file.isBlank()).orElse(null)));
      return restrictions;
    } catch (Exception exc) {
      // Without its profile files the user only gets the permissions assigned to its profiles
      log.error("The restriction files of the profiles could not be retrieved", exc);
      return Collections.emptyMap();
    }
  }

  @Override
  public Map<String, List<String>> findAllPermissions() {
    try {
      Map<String, List<String>> permissions = new LinkedHashMap<>();
      queryService.launchPrivateQuery(AweConstants.ALL_PROFILE_PERMISSIONS_QUERY, unpaginatedParameters())
        .getDataList().getRows().stream()
        .filter(row -> value(row, PROFILE) != null && value(row, PERMISSION) != null)
        .forEach(row -> permissions.computeIfAbsent(value(row, PROFILE), profile -> new ArrayList<>())
          .add(value(row, PERMISSION)));
      return permissions;
    } catch (Exception exc) {
      log.error("The permissions of the profiles could not be retrieved", exc);
      return Collections.emptyMap();
    }
  }

  /**
   * Parameters of a list query read at login or startup: every row, not only the first page
   */
  private ObjectNode unpaginatedParameters() {
    return queryUtil.getParameters(null, "1", "0");
  }

  private static String value(Map<String, CellData> row, String column) {
    CellData cell = row.get(column);
    return cell == null ? null : cell.getStringValue();
  }

  @Override
  public boolean updatePassword(String userName, String encodedPassword) {
    // The target is reachable through /action/maintain by any signed in user, so it never reads the user or the hash
    // from the request: it reads them from these session parameters, which only the server sets and which live only
    // during the update
    AweSession aweSession = getSession();
    if (aweSession == null) {
      return false;
    }
    aweSession.setParameter(REHASH_USER_SESSION_PARAMETER, userName);
    aweSession.setParameter(REHASH_VALUE_SESSION_PARAMETER, encodedPassword);
    try {
      getBean(MaintainService.class).launchPrivateMaintain(REHASH_MAINTAIN, JsonNodeFactory.instance.objectNode());
      return true;
    } catch (Exception exc) {
      // The exception is not logged: its message may carry the new hash
      log.debug("Error storing the rehashed password of user {}: {}", userName, exc.getClass().getSimpleName());
      return false;
    } finally {
      aweSession.removeParameter(REHASH_USER_SESSION_PARAMETER);
      aweSession.removeParameter(REHASH_VALUE_SESSION_PARAMETER);
    }
  }
}
