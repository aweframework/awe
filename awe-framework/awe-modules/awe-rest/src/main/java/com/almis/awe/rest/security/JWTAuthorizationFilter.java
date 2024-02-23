package com.almis.awe.rest.security;

import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.rest.service.JWTTokenService;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT Authorization filter. Used to filter awe-rest api end points
 */
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

  // Autowired
  private final UserDetailsService userDetailsService;
  private final JWTTokenService jwtTokenService;

  /**
   * JWTAuthorizationFilter constructor
   *
   * @param authenticationManager Authentication manager
   * @param userDetailsService  UserDetail service
   * @param jwtTokenService       JWT token service
   */
  public JWTAuthorizationFilter(AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JWTTokenService jwtTokenService) {
    super(authenticationManager);
    this.userDetailsService = userDetailsService;
    this.jwtTokenService = jwtTokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws IOException, ServletException {
    String header = request.getHeader(jwtTokenService.getAuthorizationHeader());
    if (header == null || !header.startsWith(jwtTokenService.getPrefix())) {
      chain.doFilter(request, response);
      return;
    }
    UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    chain.doFilter(request, response);
  }

  private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
    UsernamePasswordAuthenticationToken authenticationToken = null;
    // Verify token and extract user
    try {
      String authorizationHeader = request.getHeader(jwtTokenService.getAuthorizationHeader());
      if (authorizationHeader != null) {
        String token = jwtTokenService.extractToken(authorizationHeader);
        DecodedJWT decodedJWT = jwtTokenService.verifyToken(token);
        if (decodedJWT != null) {
          AweUserDetails userDetails = (AweUserDetails) userDetailsService.loadUserByUsername(decodedJWT.getSubject());
          authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, new ArrayList<>());
        }
      }

    } catch (JWTVerificationException ex) {
      //Invalid token
      logger.debug("Invalid JWT token", ex);
      return null;
    }
    return authenticationToken;
  }
}
