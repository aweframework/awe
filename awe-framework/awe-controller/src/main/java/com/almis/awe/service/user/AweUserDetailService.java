package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.User;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.permissions.EffectivePermissions;
import com.almis.awe.model.entities.permissions.EffectivePermissions.ProfileSource;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.*;

/**
 * AWE user detail service
 * Retrieve user info to authenticate
 *
 * @author pvidal
 */
@Slf4j
public class AweUserDetailService extends ServiceConfig implements UserDetailsService, UserDetailsPasswordService {

  /**
   * Prefix of the authority of each effective permission of a user ({@code PERM_users.delete})
   */
  public static final String PERMISSION_AUTHORITY_PREFIX = "PERM_";

  // Autowired services
  private final UserDAO userRepository;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Autowired constructor
   *
   * @param baseConfigProperties Base configuration properties
   * @param securityConfigProperties Security configuration properties
   * @param userDAO User DAO
   */
  public AweUserDetailService(BaseConfigProperties baseConfigProperties, SecurityConfigProperties securityConfigProperties, UserDAO userDAO) {
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.userRepository = userDAO;
  }

  @Override
  public AweUserDetails loadUserByUsername(String username) {
    // Get user info
    User user = userRepository.findByUserName(username);

    // Build User details
    return getAweUserDetails(user);
  }

  /**
   * Store the password hash of a user rehashed with the configured algorithm after a successful login. A failed update
   * never breaks the login: the user keeps the current hash and signs in again next time with it
   *
   * @param user        User details
   * @param newPassword New password hash
   * @return User details with the new password hash, or the same user details when it has not been stored
   */
  @Override
  public UserDetails updatePassword(UserDetails user, String newPassword) {
    boolean updated;
    try {
      updated = userRepository.updatePassword(user.getUsername(), newPassword);
    } catch (RuntimeException exc) {
      // The exception is not logged: its message may carry the new hash
      updated = false;
    }
    if (!updated) {
      log.warn("The password of user {} could not be rehashed with the configured algorithm. It keeps the current hash", user.getUsername());
      return user;
    }
    log.info("The password of user {} has been rehashed with the configured algorithm", user.getUsername());
    return user instanceof AweUserDetails aweUser ? aweUser.setPassword(newPassword) : user;
  }

  public AweUserDetails loadUserByEmail(String email) {
    // Get user info
    User user = userRepository.findByEmail(email);
    return getAweUserDetails(user);
  }

  /**
   * Get user info from the profile. If user authentication hasn't granted authorities, it recovers from the default profile
   * @param oAuth2User Oauth2User info
   * @return AWE user details
   */
  public AweUserDetails loadUserByRole(OAuth2AuthenticationToken oAuth2User) throws AWException {

    // Get profile from authority grants oauth2 user info, falling back to the default role when the
    // provider did not send a usable role (new users always get a profile)
    String profile = mapGrantedAuthorityProfile(oAuth2User.getAuthorities()).orElse(baseConfigProperties.getDefaultRole());
    // Get user info
    String userName = oAuth2User.getPrincipal().getAttribute(PREFERRED_USERNAME);
    String email = oAuth2User.getPrincipal().getAttribute(EMAIL);
    String fullName = oAuth2User.getPrincipal().getAttribute(NAME);

    if (!existRole(profile)) {
      log.warn("Profile {} from oauth information grant authority not exist in database. Using default application profile {}", profile, baseConfigProperties.getDefaultRole());
      profile = baseConfigProperties.getDefaultRole();
    }
    // Get user info
    User user = userRepository.findByRole(profile);
    user.setEmail(email);
    user.setUsername(userName);
    user.setFullName(fullName);
    user.setProfileName(profile);
    user.setEnabled(true);

    // Build User details
    return getAweUserDetails(user);
  }

  /**
   * Maps a collection of granted authorities to a user profile identifier coming from the SSO provider.
   * If the collection is null or empty, an empty {@link Optional} is returned.
   * If a filter authority prefix is specified, only authorities matching the prefix are considered.
   * The authority prefix is removed from the authority string before returning the mapped profile.
   * An empty {@link Optional} is also returned when no authority matches the prefix, or when every
   * matching authority is not longer than the prefix itself (i.e. it carries no role name).
   * When several authorities map to a profile name, the first name of {@code awe.security.sso.profile-priority} that is
   * mapped and exists as an AWE profile is returned (compared without case). When none of them is, the first name in
   * alphabetical order that exists as an AWE profile is returned (the first name in alphabetical order when none
   * exists), so the result does not depend on the order in which the provider sends the roles and a valid role is never
   * hidden by one without a profile.
   * Callers decide how to handle the "no role from provider" case (e.g. keep the current profile or
   * fall back to the application default role).
   *
   * @param authorities a collection of granted authorities which represent user permissions
   * @return the user profile mapped from the provider authorities, or empty if none was found
   * @throws AWException error checking whether a profile exists
   */
  public Optional<String> mapGrantedAuthorityProfile(Collection<GrantedAuthority> authorities) throws AWException {
    List<String> profiles = mapProfileNames(authorities);
    if (profiles.size() > 1) {
      Optional<String> priorityProfile = findPriorityProfile(profiles);
      if (priorityProfile.isPresent()) {
        return priorityProfile;
      }
      for (String profile : profiles) {
        if (existRole(profile)) {
          return Optional.of(profile);
        }
      }
    }
    return profiles.stream().findFirst();
  }

  /**
   * Find the first profile of {@code awe.security.sso.profile-priority} that is in the mapped names (compared without
   * case) and exists as an AWE profile
   *
   * @param profiles Profile names mapped from the provider authorities
   * @return Mapped name of the priority profile, or empty when none of the list is mapped and exists
   * @throws AWException error checking whether a profile exists
   */
  private Optional<String> findPriorityProfile(List<String> profiles) throws AWException {
    List<String> priority = Optional.ofNullable(securityConfigProperties.getSso().getProfilePriority())
      .orElse(List.of());
    for (String priorityProfile : priority) {
      Optional<String> mapped = profiles.stream().filter(name -> name.equalsIgnoreCase(priorityProfile)).findFirst();
      if (mapped.isPresent() && existRole(mapped.get())) {
        return mapped;
      }
    }
    return Optional.empty();
  }

  /**
   * Maps the authorities of the SSO provider to the ids of the additional profiles of the user: every profile name
   * mapped as {@link #mapGrantedAuthorityProfile(Collection)} does, in the same order, that exists as an AWE profile
   * and is not the main profile. Names without a profile are ignored.
   *
   * @param authorities SSO provider authorities
   * @param mainProfile Name of the main profile mapped from the same authorities
   * @return Ids of the additional profiles, without repetitions (empty when no other role maps to a profile)
   * @throws AWException error retrieving a profile
   */
  public List<Integer> mapAdditionalProfileIds(Collection<GrantedAuthority> authorities, String mainProfile)
    throws AWException {
    List<String> names = mapProfileNames(authorities).stream()
      .filter(name -> !name.equalsIgnoreCase(mainProfile))
      .toList();
    if (names.isEmpty()) {
      return List.of();
    }
    Optional<Integer> mainProfileId = userRepository.findProfileId(mainProfile);
    Set<Integer> profileIds = new LinkedHashSet<>();
    for (String name : names) {
      userRepository.findProfileId(name)
        .filter(id -> mainProfileId.map(mainId -> !mainId.equals(id)).orElse(true))
        .ifPresent(profileIds::add);
    }
    return List.copyOf(profileIds);
  }

  /**
   * Maps the roles of an identity provider token to AWE profiles as the single sign-on login does: the main profile of
   * {@link #mapGrantedAuthorityProfile(Collection)} first, then the other roles with the authority prefix, without it
   * and in alphabetical order. Only the names of existing profiles are kept (compared without case); the rest are
   * ignored
   *
   * @param authorities Roles of the token
   * @return Profiles with their acronym, name and restriction file, main profile first (empty when no role maps)
   * @throws AWException error checking whether a profile exists
   */
  public List<UserProfile> mapProfiles(Collection<GrantedAuthority> authorities) throws AWException {
    List<String> names = mapProfileNames(authorities);
    Optional<String> mainProfile = mapGrantedAuthorityProfile(authorities);
    List<UserProfile> profiles = new ArrayList<>();
    for (String name : Stream.concat(mainProfile.stream(), names.stream()).toList()) {
      Optional<UserProfile> profile = findProfile(name);
      if (profile.isPresent() && profiles.stream().noneMatch(found -> found.acronym().equals(profile.get().acronym()))) {
        profiles.add(profile.get());
      }
    }
    // Role values are not logged: only how many of them map to a profile
    log.debug("{} of {} roles map to an AWE profile", profiles.size(), names.size());
    return profiles;
  }

  /**
   * Find a profile by its name (compared without case)
   *
   * @param name Profile name
   * @return Profile with its acronym, name and restriction file, empty when there is no profile with that name
   * @throws AWException The profile cannot be retrieved
   */
  public Optional<UserProfile> findProfile(String name) throws AWException {
    return userRepository.findProfile(name);
  }

  /**
   * Whether the user DAO supports the additional profiles of a user (AweOpeRol)
   *
   * @return true if the additional profiles can be read and stored
   */
  public boolean supportsAdditionalProfiles() {
    return userRepository.supportsAdditionalProfiles();
  }

  /**
   * Reloads the additional profiles of a user (AweOpeRol) after they have been stored, with one role per profile
   *
   * @param userDetails User details, keeping its main profile
   */
  public void refreshAdditionalProfiles(AweUserDetails userDetails) {
    List<UserProfile> additionalProfiles = userRepository.findAdditionalProfiles(userDetails.getUsername());
    userDetails.setAdditionalProfiles(additionalProfiles);
    userDetails.setAuthorities(getAuthorities(userDetails.getProfileName(), additionalProfiles,
      userDetails.getProfiles()));
  }

  /**
   * Maps the authorities of the SSO provider to profile names: the authorities with the configured prefix, without it,
   * without repetitions and in alphabetical order
   *
   * @param authorities SSO provider authorities
   * @return Profile names (empty without authorities)
   */
  private List<String> mapProfileNames(Collection<GrantedAuthority> authorities) {
    // Validate authority
    if (authorities == null || authorities.isEmpty()) {
      return List.of();
    }

    // Get profile from grantedAuthority
    String filterAuthorityPrefix = securityConfigProperties.getSso().getFilterAuthorityPrefix();

    List<String> profiles = authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(Objects::nonNull)
        .filter(authority -> StringUtils.isEmpty(filterAuthorityPrefix) ||
            authority.startsWith(filterAuthorityPrefix))
        .map(mapAuthority(filterAuthorityPrefix))
        .flatMap(Optional::stream)
        .distinct()
        .sorted()
        .toList();
    // Role values are not logged: only how many of them map to a profile
    log.debug("{} of {} authorities map to a profile with prefix {}", profiles.size(), authorities.size(), filterAuthorityPrefix);
    return profiles;
  }

  @NotNull
  private Function<String, Optional<String>> mapAuthority(String filterAuthorityPrefix) {
    return authority -> {
      if (StringUtils.isEmpty(filterAuthorityPrefix)) {
        return Optional.of(authority); // Without prefix, return full authority
      }
      // Check authority length is not below the prefix: no role name carried by this authority
      if (authority.length() <= filterAuthorityPrefix.length()) {
        return Optional.empty();
      }
      return Optional.of(authority.substring(filterAuthorityPrefix.length()));
    };
  }

  /**
   * Get authorities from profiles
   *
   * @param profile User profile
   * @return List or granted authority
   */
  public Collection<GrantedAuthority> getAuthorities(String profile) {
    List<GrantedAuthority> authList = new ArrayList<>();
    // Add authority
    authList.add(new SimpleGrantedAuthority("ROLE_" + profile));
    return authList;
  }

  /**
   * Get the authorities of a user: one role for the main profile, then one for each additional profile
   *
   * @param profile            Name of the main profile
   * @param additionalProfiles Additional profiles of the user
   * @return List of granted authorities, without repetitions
   */
  public Collection<GrantedAuthority> getAuthorities(String profile, List<UserProfile> additionalProfiles) {
    List<GrantedAuthority> authList = new ArrayList<>(getAuthorities(profile));
    additionalProfiles.stream()
      .map(UserProfile::name)
      .filter(StringUtils::isNotBlank)
      .map(name -> new SimpleGrantedAuthority("ROLE_" + name))
      .filter(authority -> !authList.contains(authority))
      .forEach(authList::add);
    return authList;
  }

  /**
   * Get the authorities of a user: one role for the main profile, then one for each additional profile, then one
   * {@code PERM_<id>} for each effective permission of the profiles
   *
   * @param profile            Name of the main profile
   * @param additionalProfiles Additional profiles of the user
   * @param profileAcronyms    Acronyms of the main and the additional profiles
   * @return List of granted authorities, without repetitions
   */
  public Collection<GrantedAuthority> getAuthorities(String profile, List<UserProfile> additionalProfiles,
                                                     List<String> profileAcronyms) {
    List<GrantedAuthority> authList = new ArrayList<>(getAuthorities(profile, additionalProfiles));
    authList.addAll(getPermissionAuthorities(profileAcronyms));
    return authList;
  }

  /**
   * Get one {@code PERM_<id>} authority for each effective permission of some profiles, as the login loads them. It
   * needs no user: the REST API gives them to the profiles of an access token. A profile that is not an AWE profile
   * grants no permission
   *
   * @param profileAcronyms Profile acronyms
   * @return Permission authorities, without repetitions
   */
  public List<GrantedAuthority> getPermissionAuthorities(List<String> profileAcronyms) {
    return getEffectivePermissions(profileAcronyms).stream()
      .<GrantedAuthority>map(permission -> new SimpleGrantedAuthority(PERMISSION_AUTHORITY_PREFIX + permission))
      .toList();
  }

  /**
   * Get the effective permissions of some profiles: the union of the permissions of each one, plus the permissions
   * granted always. The permissions of a profile are those of its profile file (the restriction file, AwePro.Res, or
   * the default restriction file when it has none) minus the exclusions of that file, plus the permissions assigned to
   * it (AweProPer), expanded against the catalog. An exclusion only subtracts inside its profile, and an assignment
   * that matches no permission of the catalog is ignored. The permissions are read at login, so a change applies at
   * the next login (on the next request for an access token of the REST API)
   *
   * @param profileAcronyms Profile acronyms
   * @return Permission identifiers, without repetitions
   */
  public Set<String> getEffectivePermissions(List<String> profileAcronyms) {
    return getEffectivePermissionDetail(profileAcronyms).getGranted();
  }

  /**
   * Get the effective permissions of some profiles with their provenance, as {@link #getEffectivePermissions(List)}
   * computes them: what each profile declares (its profile file, its exclusions and its assignments), and the origins
   * and the exclusions of every permission. The login and the permissions view of the access window read this one
   *
   * @param profileAcronyms Profile acronyms, main profile first
   * @return Effective permissions with their provenance
   */
  public EffectivePermissions getEffectivePermissionDetail(List<String> profileAcronyms) {
    PermissionCatalog catalog = getPermissionCatalog();
    List<String> profiles = profileAcronyms.stream().filter(StringUtils::isNotBlank).distinct().toList();
    Map<String, String> profileFiles = profiles.isEmpty() ? Map.of() : userRepository.findProfileRestrictions(profiles);
    // Profile values are not logged: they may come from an access token
    long unknownProfiles = profiles.stream().filter(profile -> !profileFiles.containsKey(profile)).count();
    if (unknownProfiles > 0) {
      log.debug("{} of {} profiles are not found: they grant no profile file permission", unknownProfiles,
        profiles.size());
    }
    List<ProfileSource> sources = new ArrayList<>();
    profiles.forEach(profile -> {
      List<String> assignments = userRepository.findPermissions(List.of(profile));
      assignments.stream().filter(assignment -> catalog.expand(assignment).isEmpty()).forEach(assignment ->
        log.debug("Permission {} of profile {} matches no permission of the catalog: ignored", assignment, profile));
      String file = profileFiles.containsKey(profile) ? getProfileFileName(profileFiles.get(profile)) : null;
      Optional<Profile> profileFile = Optional.ofNullable(file).flatMap(this::findProfileFile);
      sources.add(new ProfileSource(profile, file,
        profileFile.map(Profile::getPermissionIds).orElse(List.of()),
        profileFile.map(Profile::getExclusionIds).orElse(List.of()), assignments));
    });
    return EffectivePermissions.compute(catalog, sources);
  }

  /**
   * Get the additional profiles of a user (AweOpeRol)
   *
   * @param userName User name
   * @return Additional profiles, empty when the user DAO does not support them
   */
  public List<UserProfile> getAdditionalProfiles(String userName) {
    return userRepository.findAdditionalProfiles(userName);
  }

  /**
   * Permissions of the profile file of a profile minus its exclusions, expanded against the catalog
   *
   * @param restriction Restriction file of the profile (the default restriction file when blank)
   * @return Permission identifiers, in catalog order (empty when the file does not exist)
   */
  public List<String> getProfileFilePermissions(String restriction) {
    return getProfileFilePermissions(getPermissionCatalog(), restriction);
  }

  /**
   * Permissions of a profile file minus its exclusions
   *
   * @param catalog     Permission catalog
   * @param restriction Restriction file of the profile (the default restriction file when blank)
   * @return Permission identifiers (empty when the file does not exist)
   */
  private List<String> getProfileFilePermissions(PermissionCatalog catalog, String restriction) {
    return findProfileFile(getProfileFileName(restriction))
      .map(profile -> catalog.expand(profile.getPermissionIds(), profile.getExclusionIds()))
      .orElse(List.of());
  }

  /**
   * Profile file of a profile: its restriction file, or the default restriction file when it has none
   */
  private String getProfileFileName(String restriction) {
    return StringUtils.isBlank(restriction) ? securityConfigProperties.getDefaultRestriction() : restriction;
  }

  private Optional<Profile> findProfileFile(String file) {
    Profile profile;
    try {
      AweElements elements = getElements();
      profile = elements == null ? null : elements.getProfile(file);
    } catch (AWException exc) {
      profile = null;
    }
    if (profile == null) {
      log.debug("Profile file {} not found: it grants no permission", file);
    }
    return Optional.ofNullable(profile);
  }

  private PermissionCatalog getPermissionCatalog() {
    AweElements elements = getElements();
    PermissionCatalog catalog = elements == null ? null : elements.getPermissionCatalog();
    return catalog == null ? PermissionCatalog.empty() : catalog;
  }

  /**
   * Check if the password of a user is expired
   * <ul>
   *   <li>Without the <code>PwdExp</code> parameter (number of days), passwords never expire</li>
   *   <li>With <code>PwdExp</code> and a user who has never changed the password (no change date, for example a new
   *   user or a password reset by an administrator), the password is expired: the user has to change it</li>
   *   <li>With <code>PwdExp</code> and a change date, the password is valid until <code>PwdExp</code> days after
   *   the change date, and expired from that moment on</li>
   * </ul>
   *
   * @param updateDate Date of the last password change (null if it has never been changed)
   * @return true if the password is expired
   */
  private boolean checkExpiredPassword(Date updateDate) {
    // Get PwdExp (number of days for password to expire)
    String passwordExpirationDaysStr = getProperty("PwdExp");
    if (passwordExpirationDaysStr == null) {
      return false;
    } else if (updateDate == null) {
      // Unchanged password
      return true;
    } else {
      // Add password expiration days to update date to retrieve expiration date
      int passwordExpirationDays = Integer.parseInt(passwordExpirationDaysStr);
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(updateDate);
      calendar.add(Calendar.DATE, passwordExpirationDays);
      Date expirationDate = calendar.getTime();
      Date currentDate = new Date();

      // The password is expired once the expiration date has been reached
      return !expirationDate.after(currentDate);
    }
  }

  private AweUserDetails getAweUserDetails(User user) {
    if (user == null) {
      return null;
    }
    List<UserProfile> additionalProfiles = user.getUsername() == null ? List.of()
      : userRepository.findAdditionalProfiles(user.getUsername());
    String profileName = Optional.ofNullable(user.getProfileName()).orElse(baseConfigProperties.getDefaultRole());
    AweUserDetails userDetails = new AweUserDetails()
        .setUsername(user.getUsername())
        .setPassword(user.getPassword())
        .setName(user.getFullName())
        .setEmail(user.getEmail())
        .setEnabled(user.isEnabled())
        .setCredentialsNonExpired(!checkExpiredPassword(user.getLastChangedPasswordDate()))
        .setAccountNonLocked(!user.isLocked())
        .setAdditionalProfiles(additionalProfiles)
        .setEnabled2fa(user.isEnable2fa())
        .setSecret2fa(user.getSecret2fa())
        .setProfile(Optional.ofNullable(user.getProfile())
            .filter(StringUtils::isNotBlank)
            .orElse(baseConfigProperties.getDefaultRole()))
        .setProfileName(Optional.ofNullable(user.getProfileName())
            .filter(StringUtils::isNotBlank)
            .orElse(baseConfigProperties.getDefaultRole()))
        .setRestrictions(Stream.of(user.getUserRestriction(), user.getProfileRestriction())
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(securityConfigProperties.getDefaultRestriction()))
        .setUserRestriction(user.getUserRestriction())
        .setLanguage(Optional.ofNullable(user.getLanguage())
            .filter(StringUtils::isNotBlank)
            .orElse(baseConfigProperties.getLanguageDefault()))
        .setTheme(Stream.of(user.getUserTheme(), user.getProfileTheme())
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(baseConfigProperties.getTheme()))
        .setInitialScreen(Stream.of(user.getUserInitialScreen(), user.getProfileInitialScreen())
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(baseConfigProperties.getScreen().getInitial()));
    return userDetails.setAuthorities(getAuthorities(profileName, additionalProfiles, userDetails.getProfiles()));
  }

  /**
   * Find the permission assignments of every profile that match no permission of the catalog
   *
   * @return Unknown assignments by profile acronym
   */
  public Map<String, List<String>> findUnknownPermissionAssignments() {
    PermissionCatalog catalog = getPermissionCatalog();
    Map<String, List<String>> unknown = new TreeMap<>();
    userRepository.findAllPermissions().forEach((profile, assignments) -> {
      List<String> profileUnknown = assignments.stream()
        .filter(assignment -> catalog.expand(assignment).isEmpty())
        .toList();
      if (!profileUnknown.isEmpty()) {
        unknown.put(profile, profileUnknown);
      }
    });
    return unknown;
  }

  /**
   * Log once at startup the permission assignments (AweProPer) that match no permission of the catalog. They are
   * ignored at login and never deleted
   */
  @EventListener(ApplicationReadyEvent.class)
  public void logUnknownPermissionAssignments() {
    try {
      findUnknownPermissionAssignments().forEach((profile, assignments) ->
        log.warn("Permissions {} of profile {} match no permission of the catalog: they are ignored", assignments,
          profile));
    } catch (RuntimeException exc) {
      log.warn("The permission assignments of the profiles could not be checked against the catalog: {}",
        exc.getMessage());
    }
  }

  /**
   * Verify if a profile exists in a profile list
   * @param profile Profile name
   * @return true if profile exists
   * @throws AWException AWE exception
   */
  public boolean existRole(String profile) throws AWException {
    return userRepository.existRole(profile);
  }
}
