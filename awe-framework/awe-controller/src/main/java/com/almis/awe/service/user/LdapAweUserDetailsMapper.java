package com.almis.awe.service.user;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.component.AweUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * Custom ldap user details mapper
 * Used to set UserDetails
 */
@Slf4j
public class LdapAweUserDetailsMapper extends ServiceConfig implements UserDetailsContextMapper {

  private String userCredentialsAttribute = "userPassword";
  private String rolePrefix = "ROLE_";
  private String[] roleAttributes = null;
  private boolean convertToUpperCase = true;

  // Autowired services
  private final UserDetailsService userDetailService;

  /**
   * Autowired constructor
   *
   * @param userDetailService AWE user detaill service
   */
  public LdapAweUserDetailsMapper(UserDetailsService userDetailService) {
    this.userDetailService = userDetailService;
  }

  @Override
  public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<? extends GrantedAuthority> authorities) {

    // Get user info
    AweUserDetails userDetails = (AweUserDetails) userDetailService.loadUserByUsername(username);
    String dn = ctx.getNameInNamespace();

    log.debug("Mapping user details from context with DN: {}", dn);

    userDetails.setDn(dn);

    Object passwordValue = ctx.getObjectAttribute(userCredentialsAttribute);

    if (passwordValue != null) {
      userDetails.setPassword(mapPassword(passwordValue));
    }

    userDetails.setUsername(username);

    // Map the roles
    if (roleAttributes != null) {
      mapRoleAttributes(ctx, userDetails, dn);
    }

    // Add the supplied authorities
    for (GrantedAuthority authority : authorities) {
      userDetails.addAuthority(authority);
    }

    // Check for PPolicy data

    PasswordPolicyResponseControl passwordPolicy = (PasswordPolicyResponseControl) ctx
      .getObjectAttribute(PasswordPolicyControl.OID);

    if (passwordPolicy != null) {
      userDetails.setTimeBeforeExpiration(passwordPolicy.getTimeBeforeExpiration());
      userDetails.setGraceLoginsRemaining(passwordPolicy.getGraceLoginsRemaining());
    }

    return userDetails;
  }

  @Override
  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException(
      "LdapUserDetailsMapper only supports reading from a context. Please"
        + "use a subclass if mapUserToContext() is required.");
  }

  /**
   * Map role attributes
   *
   * @param ctx     Dir context operations
   * @param userDetails User details
   * @param dn      DN
   */
  private void mapRoleAttributes(DirContextOperations ctx, AweUserDetails userDetails, String dn) {
    for (String roleAttribute : roleAttributes) {
      String[] rolesForAttribute = ctx.getStringAttributes(roleAttribute);

      if (rolesForAttribute != null) {
        for (String role : rolesForAttribute) {
          GrantedAuthority authority = createAuthority(role);

          if (authority != null) {
            userDetails.addAuthority(authority);
          }
        }
      } else {
        log.debug("Couldn't read role attribute ''{}'' for user {}", roleAttribute, dn);
      }
    }
  }

  /**
   * Extension point to allow customized creation of the user's password from the
   * attribute stored in the directory.
   *
   * @param passwordValue the value of the password attribute
   * @return a String representation of the password.
   */
  private String mapPassword(Object passwordValue) {

    if (!(passwordValue instanceof String)) {
      // Assume it's binary
      passwordValue = new String((byte[]) passwordValue);
    }

    return (String) passwordValue;

  }

  /**
   * Creates a GrantedAuthority from a role attribute. Override to customize authority
   * object creation.
   * <p>
   * The default implementation converts string attributes to roles, making use of the
   * rolePrefix and convertToUpperCase properties. Non-String
   * attributes are ignored.
   * </p>
   *
   * @param role the attribute returned from
   * @return the authority to be added to the list of authorities for the user, or null
   * if this attribute should be ignored.
   */
  private GrantedAuthority createAuthority(Object role) {
    if (role instanceof String) {
      if (convertToUpperCase) {
        role = ((String) role).toUpperCase();
      }
      return new SimpleGrantedAuthority(rolePrefix + role);
    }
    return null;
  }

  /**
   * Determines whether role field values will be converted to upper case when loaded.
   * The default is true.
   *
   * @param convertToUpperCase true if the roles should be converted to upper case.
   */
  public void setConvertToUpperCase(boolean convertToUpperCase) {
    this.convertToUpperCase = convertToUpperCase;
  }

  /**
   * The name of the attribute which contains the user's password. Defaults to
   * "userPassword".
   *
   * @param passwordAttributeName the name of the attribute
   */
  public void setPasswordAttributeName(String passwordAttributeName) {
    this.userCredentialsAttribute = passwordAttributeName;
  }

  /**
   * The names of any attributes in the user's entry which represent application roles.
   * These will be converted to GrantedAuthorities and added to the list in the
   * returned LdapUserDetails object. The attribute values must be Strings by default.
   *
   * @param roleAttributes the names of the role attributes.
   */
  public void setRoleAttributes(String[] roleAttributes) {
    Assert.notNull(roleAttributes, "roleAttributes array cannot be null");
    this.roleAttributes = roleAttributes;
  }

  /**
   * The prefix that should be applied to the role names
   *
   * @param rolePrefix the prefix (defaults to "ROLE_").
   */
  public void setRolePrefix(String rolePrefix) {
    this.rolePrefix = rolePrefix;
  }
}
