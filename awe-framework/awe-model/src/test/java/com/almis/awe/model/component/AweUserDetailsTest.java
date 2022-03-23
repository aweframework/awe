package com.almis.awe.model.component;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AweUserDetailsTest {

  @Test
  void addAuthority() {
    AweUserDetails aweUserDetails = new AweUserDetails();
    aweUserDetails.addAuthority(new SimpleGrantedAuthority("test"));
    assertEquals(1, aweUserDetails.getAuthorities().size());
    aweUserDetails.addAuthority(new SimpleGrantedAuthority("test"));
    assertEquals(1, aweUserDetails.getAuthorities().size());
    aweUserDetails.addAuthority(new SimpleGrantedAuthority("test2"));
    assertEquals(2, aweUserDetails.getAuthorities().size());
  }
}