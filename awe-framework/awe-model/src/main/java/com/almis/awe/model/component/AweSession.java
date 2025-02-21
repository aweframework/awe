package com.almis.awe.model.component;

import com.almis.awe.model.util.security.mapper.mapper.AuthenticateUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.Serializable;
import java.util.Set;

import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

@Slf4j
public class AweSession implements Serializable {

  public static final String ROLE = "ROLE_";
  private final AweSessionStorage sessionStorage;

  /**
   * Autowired constructor
   */
  public AweSession() {
    // Generate session storage
    sessionStorage = new AweSessionStorage();
  }

  /**
   * Retrieve session authentication
   *
   * @return Authentication
   */
  public Authentication getAuthentication() {
     return SecurityContextHolder.getContext().getAuthentication();
  }

  /**
   * Get current session id
   *
   * @return Session id
   */
  public String getSessionId() {
    return RequestContextHolder.currentRequestAttributes().getSessionId();
  }

  /**
   * Get current session user
   *
   * @return Session user
   */
  public String getUser() {
    Authentication authentication = getAuthentication();
    return AuthenticateUserMapper.getUserFromAuthentication(authentication);
  }

  /**
   * Check if current user has the given role
   *
   * @param roleName Role name
   * @return Has the role
   */
  public boolean hasRole(String roleName) {
    String fullRoleName = roleName.startsWith(ROLE) ? roleName : ROLE + roleName;
    return getAuthentication() != null && getAuthentication().getAuthorities().contains(new SimpleGrantedAuthority(fullRoleName));
  }

  /**
   * Check if current user has all the given role
   *
   * @param roleName Role names
   * @return Has all roles
   */
  public boolean hasRoles(String... roleName) {
    if (roleName != null) {
      for (String role : roleName) {
        if (!hasRole(role)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Check if current user has any of the given role
   *
   * @param roleName Role names
   * @return Has any of the roles
   */
  public boolean hasAnyRole(String... roleName) {
    if (roleName != null) {
      for (String role : roleName) {
        if (hasRole(role)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Set new parameter
   *
   * @param name  Parameter name
   * @param value Parameter value
   */
  public void setParameter(String name, Object value) {
    sessionStorage.store(name, value);
    log.trace("Attribute added: {} = {} ", name, value);
  }

  /**
   * Get parameter
   *
   * @param name Parameter name
   * @return Parameter value
   */
  public Object getParameter(String name) {
    Object value = sessionStorage.retrieve(name);
    log.trace("Attribute retrieved: {} = {}", name, value);
    return value;
  }

  /**
   * Returns parameter value cast to the given class
   *
   * @param clazz Parameter class
   * @param name Parameter name
   * @param <T> class parameter type
   * @return Parameter value
   */
  public <T> T getParameter(Class<T> clazz, String name) {
    T value = sessionStorage.retrieve(clazz, name);
    log.trace("Attribute retrieved: {} = {}", name, value);
    return value;
  }

  /**
   * Remove parameter from session
   *
   * @param name Parameter name
   */
  public void removeParameter(String name) {
    sessionStorage.remove(name);
    log.trace("Attribute removed: {}", name);
  }

  /**
   * Check if there is a parameter in the session
   *
   * @param name Parameter name
   * @return Session has parameter
   */
  public boolean hasParameter(String name) {
    return sessionStorage.has(name);
  }

  /**
   * Get parameter names from current session
   *
   * @return Parameter names
   */
  public Set<String> getParameterNames() {
    return sessionStorage.sessionKeys();
  }

  /**
   * Check if user is authenticated
   *
   * @return User is authenticated
   */
  public boolean isAuthenticated() {
    boolean authenticated = false;
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken))  {
      if (authentication.getPrincipal() instanceof AweUserDetails aweUserDetails) {
        if (aweUserDetails.isEnabled2fa()) {
          authenticated = aweUserDetails.isFullyAuthenticated();
        } else {
          authenticated = authentication.isAuthenticated();
        }
      } else {
        authenticated = authentication.isAuthenticated();
      }
    }
    return authenticated;
  }
}