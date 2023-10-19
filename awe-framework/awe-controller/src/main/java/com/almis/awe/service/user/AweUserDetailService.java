package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.*;
import java.util.stream.Stream;

/**
 * AWE user detail service
 * Retrieve user info to authenticate
 *
 * @author pvidal
 */
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

  /*
   * (non-Javadoc)
   *
   * @see org.springframework.security.core.userdetails.UserDetailsService#
   * loadUserByUsername(java.lang.String)
   */
  @Override
  public AweUserDetails loadUserByUsername(String username) {
    // Get user info
    User user = userRepository.findByUserName(username);

    // Build User details
    return new AweUserDetails()
      .setUsername(user.getUsername())
      .setPassword(user.getPassword())
      .setName(user.getFullName())
      .setEnabled(user.isEnabled())
      .setCredentialsNonExpired(!checkExpiredPassword(user.getLastChangedPasswordDate()))
      .setAccountNonLocked(!user.isLocked())
      .setAuthorities(getAuthorities(user.getProfile()))
      .setEnabled2fa(user.isEnable2fa())
      .setSecret2fa(user.getSecret2fa())
      .setProfile(user.getProfile())
      .setRestrictions(Stream.of(user.getUserRestriction(), user.getProfileRestriction())
        .filter(StringUtils::isNotBlank)
        .findFirst().orElse(securityConfigProperties.getDefaultRestriction()))
      .setLanguage(Optional.ofNullable(user.getLanguage())
        .filter(StringUtils::isNotBlank)
        .orElse(baseConfigProperties.getLanguageDefault())
        .substring(0, 2).toLowerCase())
      .setTheme(Stream.of(user.getUserTheme(), user.getProfileTheme())
        .filter(StringUtils::isNotBlank)
        .findFirst().orElse(baseConfigProperties.getTheme()))
      .setInitialScreen(Stream.of(user.getUserInitialScreen(), user.getProfileInitialScreen())
        .filter(StringUtils::isNotBlank)
        .findFirst().orElse(baseConfigProperties.getScreen().getInitial()));
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
}
