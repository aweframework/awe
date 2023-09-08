package com.almis.awe.rest.security;

import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.almis.awe.rest.dto.JwtTokenInfo;
import com.almis.awe.rest.dto.LoginRequest;
import com.almis.awe.rest.service.JWTTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Authentication filter to create JWT tokens
 */
@Slf4j
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
    try {
      LoginRequest loginRequest = new ObjectMapper()
        .readValue(request.getInputStream(), LoginRequest.class);

      return super.getAuthenticationManager().authenticate(
          new UsernamePasswordAuthenticationToken(
            loginRequest.getUsername(),
            loginRequest.getPassword(),
            new ArrayList<>())
        );
    } catch (IOException ex) {
      log.error("Error retrieving request input data", ex);
      throw new UsernameNotFoundException("There was a problem retrieving user credentials from request");
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication auth) throws IOException {
    // Provide JWT token
    JwtTokenInfo tokenInfo = jwtTokenService.generateToken(auth, response);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(tokenInfo));
    response.getWriter().flush();
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {

    log.warn("[awe-rest] [/api/authenticate] Unsuccessful authentication in: " + exception.getMessage());

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpStatus.UNAUTHORIZED.value());

    AweRestResponse authenticationError = new AweRestResponse();
    authenticationError.setType(AnswerType.ERROR);
    authenticationError.setTitle("Unauthorized");

    if (exception instanceof BadCredentialsException) {
      authenticationError.setMessage("Bad credentials");
    } else if (exception instanceof UsernameNotFoundException){
      authenticationError.setMessage("User not found or not active");
    }
    response.getWriter().write(objectMapper.writeValueAsString(authenticationError));
  }
}