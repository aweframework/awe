package com.almis.awe.factory;

import com.almis.awe.model.component.AweUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WithMockCustomUserSecurityContextFactory
  implements WithSecurityContextFactory<WithMockCustomUser> {
  @Override
  public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();

    AweUserDetails principal =
      new AweUserDetails()
        .setAuthorities(Arrays.stream(customUser.roles()).map(SimpleGrantedAuthority::new).collect(Collectors.toList()))
        .setUsername(customUser.username())
        .setName(customUser.name())
        .setPassword(customUser.password())
        .setFullyAuthenticated(true);

    Authentication auth =
      new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
    context.setAuthentication(auth);
    return context;
  }
}
