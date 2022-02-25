package com.almis.awe.service.user;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.*;
import java.util.stream.Stream;

/**
 * AWE user detail service
 * <p>
 * Retrieve user info to authenticate
 *
 * @author pvidal
 */
public class AweUserDetailService extends ServiceConfig implements UserDetailsService {

  // Autowired services
  private final UserDAO userRepository;

  @Value("${language.default:en}")
  private String defaultLanguage;

  @Value("${application.theme:sky}")
  private String defaultTheme;

  @Value("${screen.configuration.information:information}")
  private String defaultInitialScreen;

  @Value("${security.default.restriction:general}")
  private String defaultRestriction;

  /**
   * Autowired constructor
   *
   * @param userDAO User DAO
   */
  public AweUserDetailService(UserDAO userDAO) {
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
      .setUsername(user.getUserName())
      .setPassword(user.getUserPassword())
      .setName(user.getUserFullName())
      .setEnabled(user.isEnabled())
      .setCredentialsNonExpired(!checkExpiredPassword(user.getLastChangedPasswordDate()))
      .setAccountNonLocked(!user.isPasswordLocked())
      .setAuthorities(getAuthorities(user.getProfile()))
      .setEnabled2fa(user.isEnabled2fa())
      .setSecret2fa(user.getSecret2fa())
      .setProfile(user.getProfile())
      .setRestrictions(Stream.of(user.getUserFileRestriction(), user.getProfileFileRestriction())
        .filter(StringUtils::isNotBlank)
        .findFirst().orElse(defaultRestriction))
      .setLanguage(Optional.ofNullable(user.getLanguage())
        .filter(StringUtils::isNotBlank)
        .orElse(defaultLanguage)
        .substring(0, 2).toLowerCase())
      .setTheme(Stream.of(user.getUserTheme(), user.getProfileTheme())
        .filter(StringUtils::isNotBlank)
        .findFirst().orElse(defaultTheme))
      .setInitialScreen(Stream.of(user.getUserInitialScreen(), user.getProfileInitialScreen())
        .filter(StringUtils::isNotBlank)
        .findFirst().orElse(defaultInitialScreen));
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
