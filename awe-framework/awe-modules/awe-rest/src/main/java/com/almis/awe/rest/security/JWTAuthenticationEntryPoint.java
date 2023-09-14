package com.almis.awe.rest.security;

import com.almis.awe.model.type.AnswerType;
import com.almis.awe.rest.dto.AweRestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Custom Authentication EntryPoint.
 * Used to handle exceptions in authentication process in awe-rest api.
 */
@Slf4j
public class JWTAuthenticationEntryPoint implements AuthenticationEntryPoint {

  public static final String NOT_FOUND = "not found";
  // Autowire
  private final ObjectMapper objectMapper;

  public JWTAuthenticationEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
    log.error("[awe-rest] [" + request.getRequestURI() + "] Authentication EntryPoint: " + exception.getMessage());

    AweRestResponse authenticationResponse = new AweRestResponse();
    authenticationResponse.setType(AnswerType.ERROR);
    authenticationResponse.setTitle("Unauthorized");
    authenticationResponse.setMessage("Not authorized. Token is not valid or not found");

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
    response.getWriter().write(objectMapper.writeValueAsString(authenticationResponse));
  }
}
