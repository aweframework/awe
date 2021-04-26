package com.almis.awe.rest.security;

import com.almis.awe.rest.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Custom Authentication EntryPoint.
 * Used to handle exceptions in authentication process in awe-rest api.
 */
@Log4j2
public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {

  // Autowire
  private final ObjectMapper objectMapper;

  public JWTAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
    log.warn("[awe-rest] [" + request.getRequestURI() + "] Authentication EntryPoint: " + exception.getMessage());

    ErrorResponse authenticationResponse = new ErrorResponse();
    authenticationResponse.setCode(HttpServletResponse.SC_UNAUTHORIZED);
    authenticationResponse.setMessage("Not authorized. Token is not valid or not found");

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
    response.getWriter().write(objectMapper.writeValueAsString(authenticationResponse));
  }
}
