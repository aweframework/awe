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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;


import java.util.*;
import java.util.stream.Stream;

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
   * Get user info from profile. If user authentication hasn't granted authorities, it recovers from the default profile
   * @param oAuth2User Oauth2User info
   * @return AWE user details
   */
  public AweUserDetails loadUserByRole(DefaultOAuth2User oAuth2User) throws AWException {

    // Get profile from authority grants oauth2 user info
    String profile = mapGrantedAuthorityProfile(oAuth2User);
    // Get email
    String email = oAuth2User.getAttribute(PREFERRED_USERNAME);

    if (!userRepository.existRole(profile)) {
      log.warn("Profile {} from oauth information grant authority not exist in database. Using default application profile {}", profile, baseConfigProperties.getDefaultRole());
      profile = baseConfigProperties.getDefaultRole();
    }
    // Get user info
    User user = userRepository.findByRole(profile);
    user.setEmail(email);
    user.setUsername(StringUtils.substringBefore(email, "@"));
    user.setFullName(oAuth2User.getName());
    user.setEnabled(true);

    // Build User details
    return getAweUserDetails(user);
  }

  private String mapGrantedAuthorityProfile(DefaultOAuth2User oAuth2User) {
    // Get profile from grantedAuthority
    return oAuth2User.getAuthorities().stream()
        .findFirst()
        .map(GrantedAuthority::getAuthority)
        .map(role -> Arrays.stream(StringUtils.split(role, "_"))
            .skip(1).findFirst()
            .orElse(baseConfigProperties.getDefaultRole()))
        .orElse(baseConfigProperties.getDefaultRole());
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
        .setAuthorities(getAuthorities(
            Optional.ofNullable(user.getProfile())
                .orElse(baseConfigProperties.getDefaultRole())))
        .setEnabled2fa(user.isEnable2fa())
        .setSecret2fa(user.getSecret2fa())
        .setProfile(Optional.ofNullable(user.getProfile())
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
}
