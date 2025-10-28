package com.almis.awe.controller;

import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.ErrorPageService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTest {

  @Mock
  private ErrorPageService errorPageService;

  @Mock
  private HttpServletRequest request;

  private CustomErrorController customErrorController;

  @BeforeEach
  void setUp() {
    customErrorController = new CustomErrorController(errorPageService);
  }

  @Test
  void testHandleError_With404Status() {
    // Given
    Integer statusCode = 404;
    String errorMessage = "Page not found";
    String expectedHtml = "<html>404 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Page Not Found", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Page Not Found", errorMessage);
  }

  @Test
  void testHandleError_With500Status() {
    // Given
    Integer statusCode = 500;
    String errorMessage = "Internal error";
    String expectedHtml = "<html>500 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", errorMessage);
  }

  @Test
  void testHandleError_With400Status() {
    // Given
    Integer statusCode = 400;
    String errorMessage = "Bad request";
    String expectedHtml = "<html>400 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Bad Request", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With401Status() {
    // Given
    Integer statusCode = 401;
    String errorMessage = "Unauthorized";
    String expectedHtml = "<html>401 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Unauthorized", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With403Status() {
    // Given
    Integer statusCode = 403;
    String errorMessage = "Forbidden";
    String expectedHtml = "<html>403 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Forbidden", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With405Status() {
    // Given
    Integer statusCode = 405;
    String errorMessage = "Method not allowed";
    String expectedHtml = "<html>405 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Method Not Allowed", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With408Status() {
    // Given
    Integer statusCode = 408;
    String errorMessage = "Request timeout";
    String expectedHtml = "<html>408 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Request Timeout", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.REQUEST_TIMEOUT, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With502Status() {
    // Given
    Integer statusCode = 502;
    String errorMessage = "Bad gateway";
    String expectedHtml = "<html>502 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Bad Gateway", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With503Status() {
    // Given
    Integer statusCode = 503;
    String errorMessage = "Service unavailable";
    String expectedHtml = "<html>503 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Service Unavailable", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_With504Status() {
    // Given
    Integer statusCode = 504;
    String errorMessage = "Gateway timeout";
    String expectedHtml = "<html>504 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Gateway Timeout", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_WithUnknownStatus() {
    // Given
    Integer statusCode = 418; // I'm a teapot
    String errorMessage = "I'm a teapot";
    String expectedHtml = "<html>418 Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Error 418", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(418, response.getStatusCode().value());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Error 418", errorMessage);
  }

  @Test
  void testHandleError_WithNullStatus() {
    // Given
    String errorMessage = "Error occurred";
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
  }

  @Test
  void testHandleError_WithStringMessage() {
    // Given
    Integer statusCode = 500;
    String errorMessage = "Custom error message";
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", errorMessage);
  }

  @Test
  void testHandleError_WithEmptyMessage() {
    // Given
    Integer statusCode = 500;
    String errorMessage = "   ";
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", "Unknown error occurred")).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", "Unknown error occurred");
  }

  @Test
  void testHandleError_WithThrowableException() {
    // Given
    Integer statusCode = 500;
    Throwable exception = new RuntimeException("Runtime error");
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(exception);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(null);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", "Runtime error")).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", "Runtime error");
  }

  @Test
  void testHandleError_WithAWERuntimeException() {
    // Given
    Integer statusCode = 500;
    AWException aweException = new AWException("AWE error message");
    AWERuntimeException runtimeException = new AWERuntimeException(aweException);
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(runtimeException);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(null);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", "AWE error message")).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", "AWE error message");
  }

  @Test
  void testHandleError_WithAWERuntimeException_WithoutAWExceptionCause() {
    // Given
    Integer statusCode = 500;
    AWERuntimeException runtimeException = new AWERuntimeException("Runtime error", new Exception("Other cause"));
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(runtimeException);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(null);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", "Unknown error occurred")).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", "Unknown error occurred");
  }

  @Test
  void testHandleError_WithNullExceptionAndMessage() {
    // Given
    Integer statusCode = 500;
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(null);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(null);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", "Unknown error occurred")).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", "Unknown error occurred");
  }

  @Test
  void testHandleError_MessageTakesPrecedenceOverException() {
    // Given
    Integer statusCode = 500;
    String errorMessage = "Message from attribute";
    Throwable exception = new RuntimeException("Exception message");
    String expectedHtml = "<html>Error</html>";

    when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
    when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(exception);
    when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(errorMessage);
    when(errorPageService.generateErrorPageFromTemplate("Internal Server Error", errorMessage)).thenReturn(expectedHtml);

    // When
    ResponseEntity<String> response = customErrorController.handleError(request);

    // Then
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(expectedHtml, response.getBody());
    verify(errorPageService).generateErrorPageFromTemplate("Internal Server Error", errorMessage);
  }

  @Test
  void testConstructor() {
    // When
    CustomErrorController controller = new CustomErrorController(errorPageService);

    // Then
    assertNotNull(controller);
  }
}
