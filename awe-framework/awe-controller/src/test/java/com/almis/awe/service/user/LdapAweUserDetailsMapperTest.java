package com.almis.awe.service.user;

import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;

import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LdapAweUserDetailsMapperTest {

  @InjectMocks
  LdapAweUserDetailsMapper ldapAweUserDetailsMapper;
  @Mock
  ApplicationContext context;
  @Mock
  AweSession aweSession;
  @Mock
  UserDAO userDAO;
  @Mock
  DirContextAdapter contextAdapter;
  @Mock
  DirContextOperations contextOperations;
  @Mock
  UserDetailsService userDetailsService;
  @Mock
  AweUserDetails userDetails;

  @BeforeEach
  public void setUp() {
    ldapAweUserDetailsMapper.setApplicationContext(context);
    when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
  }

  /**
   * User details mapper test
   */
  @Test
  void testLdapUserDetails() {
    given(contextOperations.getNameInNamespace()).willReturn("test");
    UserDetails details = ldapAweUserDetailsMapper.mapUserFromContext(contextOperations, "test", Collections.singletonList(new SimpleGrantedAuthority("ROLE_DUMMY")));
    assertNotNull(details);
  }

  /**
   * User details mapper test
   */
  @Test
  void testLdapUserDetailsWithPasswordRetrieved() {
    given(contextOperations.getNameInNamespace()).willReturn("test");
    given(contextOperations.getObjectAttribute("userPassword")).willReturn("test");
    given(contextOperations.getObjectAttribute(PasswordPolicyControl.OID)).willReturn(mock(PasswordPolicyResponseControl.class));
    given(contextOperations.getStringAttributes("dummy")).willReturn(new String[]{"dummy", "dummy"});

    ldapAweUserDetailsMapper.setRoleAttributes(new String[]{"dummy", "dummy"});
    UserDetails details = ldapAweUserDetailsMapper.mapUserFromContext(contextOperations, "test", singletonList(new SimpleGrantedAuthority("ROLE_DUMMY")));

    assertThrows(UnsupportedOperationException.class, () -> ldapAweUserDetailsMapper.mapUserToContext(details, contextAdapter));
  }

}