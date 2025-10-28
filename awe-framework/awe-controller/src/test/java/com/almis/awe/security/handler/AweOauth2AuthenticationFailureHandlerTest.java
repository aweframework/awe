package com.almis.awe.security.handler;

import com.almis.awe.service.ErrorPageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AweOauth2AuthenticationFailureHandlerTest {

  @Mock
  private ErrorPageService errorPageService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private PrintWriter writer;

  private AweOauth2AuthenticationFailureHandler handler;

  @BeforeEach
  void setUp() throws IOException {
    handler = new AweOauth2AuthenticationFailureHandler(errorPageService);
    lenient().when(response.getWriter()).thenReturn(writer);
  }

  @Test
  void testOnAuthenticationFailure_WithBadCredentialsException() throws IOException {
    // Given
    String errorMessage = "Bad credentials";
    AuthenticationException exception = new BadCredentialsException(errorMessage);
    String expectedHtml = "<html>Authentication failed</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, errorMessage)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("text/html; charset=UTF-8");
    verify(response).getWriter();
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(null, errorMessage);
  }

  @Test
  void testOnAuthenticationFailure_WithDisabledException() throws IOException {
    // Given
    String errorMessage = "User is disabled";
    AuthenticationException exception = new DisabledException(errorMessage);
    String expectedHtml = "<html>User disabled</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, errorMessage)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("text/html; charset=UTF-8");
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(null, errorMessage);
  }

  @Test
  void testOnAuthenticationFailure_WithOAuth2AuthenticationException() throws IOException {
    // Given
    OAuth2Error oauth2Error = new OAuth2Error("invalid_token", "Token is invalid", null);
    AuthenticationException exception = new OAuth2AuthenticationException(oauth2Error);
    String expectedHtml = "<html>OAuth2 error</html>";

    when(errorPageService.generateErrorPageFromTemplate(eq(null), anyString())).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("text/html; charset=UTF-8");
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(eq(null), anyString());
  }

  @Test
  void testOnAuthenticationFailure_WithNullMessage() throws IOException {
    // Given
    AuthenticationException exception = new BadCredentialsException(null);
    String expectedHtml = "<html>Error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, null)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("text/html; charset=UTF-8");
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(null, null);
  }

  @Test
  void testOnAuthenticationFailure_WithEmptyMessage() throws IOException {
    // Given
    String errorMessage = "";
    AuthenticationException exception = new BadCredentialsException(errorMessage);
    String expectedHtml = "<html>Empty error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, errorMessage)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("text/html; charset=UTF-8");
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(null, errorMessage);
  }

  @Test
  void testOnAuthenticationFailure_WithLongMessage() throws IOException {
    // Given
    String errorMessage = "This is a very long error message that contains multiple details about what went wrong during authentication";
    AuthenticationException exception = new BadCredentialsException(errorMessage);
    String expectedHtml = "<html>Long error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, errorMessage)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(response).setContentType("text/html; charset=UTF-8");
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(null, errorMessage);
  }

  @Test
  void testOnAuthenticationFailure_VerifyResponseStatus() throws IOException {
    // Given
    AuthenticationException exception = new BadCredentialsException("Error");
    String expectedHtml = "<html>Error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, "Error")).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(401); // SC_UNAUTHORIZED
    verify(response).setContentType("text/html; charset=UTF-8");
  }

  @Test
  void testOnAuthenticationFailure_VerifyContentType() throws IOException {
    // Given
    AuthenticationException exception = new BadCredentialsException("Error");
    String expectedHtml = "<html>Error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, "Error")).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setContentType("text/html; charset=UTF-8");
  }

  @Test
  void testOnAuthenticationFailure_VerifyHtmlIsWritten() throws IOException {
    // Given
    AuthenticationException exception = new BadCredentialsException("Test error");
    String expectedHtml = "<html><body>Test error page</body></html>";

    when(errorPageService.generateErrorPageFromTemplate(null, "Test error")).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(writer).write(expectedHtml);
  }

  @Test
  void testOnAuthenticationFailure_WithSpecialCharactersInMessage() throws IOException {
    // Given
    String errorMessage = "Error: <script>alert('xss')</script> & special chars";
    AuthenticationException exception = new BadCredentialsException(errorMessage);
    String expectedHtml = "<html>Sanitized error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, errorMessage)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(writer).write(expectedHtml);
    verify(errorPageService).generateErrorPageFromTemplate(null, errorMessage);
  }

  @Test
  void testOnAuthenticationFailure_CallsErrorPageServiceWithNullTitle() throws IOException {
    // Given
    String errorMessage = "Authentication failed";
    AuthenticationException exception = new BadCredentialsException(errorMessage);
    String expectedHtml = "<html>Error</html>";

    when(errorPageService.generateErrorPageFromTemplate(null, errorMessage)).thenReturn(expectedHtml);

    // When
    handler.onAuthenticationFailure(request, response, exception);

    // Then
    verify(errorPageService).generateErrorPageFromTemplate(null, errorMessage);
    verify(errorPageService).generateErrorPageFromTemplate(isNull(), eq(errorMessage));
  }

  @Test
  void testConstructor() {
    // Create a new mock that won't have the setUp stubbing
    ErrorPageService mockErrorPageService = mock(ErrorPageService.class);
    
    // When
    AweOauth2AuthenticationFailureHandler newHandler = new AweOauth2AuthenticationFailureHandler(mockErrorPageService);

    // Then
    assertNotNull(newHandler);
  }
}
