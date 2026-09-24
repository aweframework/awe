package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
public class AweUserDetailService extends ServiceConfig implements UserDetailsService {

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
   * An empty {@link Optional} is also returned when no authority matches the prefix, or when the
   * matching authority is not longer than the prefix itself (i.e. it carries no role name).
   * Callers decide how to handle the "no role from provider" case (e.g. keep the current profile or
   * fall back to the application default role).
   *
   * @param authorities a collection of granted authorities which represent user permissions
   * @return the user profile mapped from the provider authorities, or empty if none was found
   */
  public Optional<String> mapGrantedAuthorityProfile(Collection<GrantedAuthority> authorities) {
    // Validate authority
    if (authorities == null || authorities.isEmpty()) {
      return Optional.empty();
    }

    // Get profile from grantedAuthority
    String filterAuthorityPrefix = securityConfigProperties.getSso().getFilterAuthorityPrefix();

    if (log.isDebugEnabled()) {
    log.debug("Mapping authorities {} with prefix {}", authorities.stream().map(GrantedAuthority::getAuthority).toList(),
        filterAuthorityPrefix);
    }
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .filter(Objects::nonNull) // Filtrar null authorities
        .filter(authority -> StringUtils.isEmpty(filterAuthorityPrefix) ||
            authority.startsWith(filterAuthorityPrefix))
        .findFirst()
        .flatMap(mapAuthority(filterAuthorityPrefix));
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

      // Check expiration date versus current date
      return expirationDate.compareTo(currentDate) > 0;
    }
  }

  private AweUserDetails getAweUserDetails(User user) {
    return user == null ? null : new AweUserDetails()
        .setUsername(user.getUsername())
        .setPassword(user.getPassword())
        .setName(user.getFullName())
        .setEmail(user.getEmail())
        .setEnabled(user.isEnabled())
        .setCredentialsNonExpired(!checkExpiredPassword(user.getLastChangedPasswordDate()))
        .setAccountNonLocked(!user.isLocked())
        .setAuthorities(getAuthorities(Optional.ofNullable(user.getProfileName())
                .orElse(baseConfigProperties.getDefaultRole())))
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
        .setLanguage(Optional.ofNullable(user.getLanguage())
            .filter(StringUtils::isNotBlank)
            .orElse(baseConfigProperties.getLanguageDefault()))
        .setTheme(Stream.of(user.getUserTheme(), user.getProfileTheme())
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(baseConfigProperties.getTheme()))
        .setInitialScreen(Stream.of(user.getUserInitialScreen(), user.getProfileInitialScreen())
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(baseConfigProperties.getScreen().getInitial()));
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