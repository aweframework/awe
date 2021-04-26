package com.almis.awe.rest.security;

import com.almis.awe.rest.dto.Error;
import com.almis.awe.rest.service.JWTTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Authentication filter to create JWT tokens
 */
@Log4j2
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  // Autowired
  private final ObjectMapper objectMapper;
  private final JWTTokenService jwtTokenService;

  /**
   * Instantiates a new Jwt authentication filter.
   *  @param authenticationManager the authentication manager
   * @param objectMapper          the object mapper
   * @param jwtTokenService       JWT token service util
   */
  public JWTAuthenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper, JWTTokenService jwtTokenService) {
    super(authenticationManager);
    this.objectMapper = objectMapper;
    this.jwtTokenService = jwtTokenService;
    setFilterProcessesUrl("/api/authenticate");
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    // Get user info from request
    String username = obtainUsername(request);
    String password = obtainPassword(request);
    return super.getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(username, password, new ArrayList<>()));
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication auth) throws IOException, ServletException {
    // Provide JWT token
    jwtTokenService.generateToken(auth, response);
    chain.doFilter(request, response);
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

    log.warn("[awe-rest] [/api/authenticate] Unsuccessful authentication in : " + exception.getMessage());
    Error authenticationError = new Error();
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    if (exception instanceof BadCredentialsException) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      authenticationError.setCode(HttpStatus.UNAUTHORIZED.value());
      authenticationError.setMessage("Bad credentials");
    } else if (exception instanceof UsernameNotFoundException){
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      authenticationError.setCode(HttpStatus.UNAUTHORIZED.value());
      authenticationError.setMessage("User not found or not active");
    } else {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      authenticationError.setCode(HttpStatus.BAD_REQUEST.value());
      authenticationError.setMessage(exception.getMessage());
    }
    response.getWriter().write(objectMapper.writeValueAsString(authenticationError));
  }
}