package com.almis.awe.service.user;

import com.almis.awe.dao.UserDAO;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AweUserDetailServiceTest {

  @InjectMocks
  AweUserDetailService userDetailsService;
  @Mock
  ApplicationContext context;
  @Mock
  AweSession aweSession;
  @Mock
  AweElements aweElements;
  @Mock
  UserDAO userDAO;

  @BeforeEach
  void setUp() {
    userDetailsService.setApplicationContext(context);
    ReflectionTestUtils.setField(userDetailsService, "defaultLanguage", "ES");
    ReflectionTestUtils.setField(userDetailsService, "defaultTheme", "sunset");
    ReflectionTestUtils.setField(userDetailsService, "defaultRestriction", "restriction");
  }

  @Test
  void giveSomeUser_willReturnUserDetails() {
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLastChangedPasswordDate(new Date())
      .setLocked(true));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertAll(
      () -> assertNotNull(details),
      () -> assertFalse(details.isCredentialsNonExpired()),
      () -> assertFalse(details.isAccountNonLocked())
    );
  }

  @Test
  void giveSomeUserWithCredentialExpired_willReturnUserDetails() {
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLocked(false));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertAll(
      () -> assertNotNull(details),
      () -> assertFalse(details.isCredentialsNonExpired()),
      () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @Test
  void getAuthorities() {
    GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_DUMMY");
    List<GrantedAuthority> expectedValue = Collections.singletonList(grantedAuthority);
    assertEquals(expectedValue, userDetailsService.getAuthorities("DUMMY"));
  }
}