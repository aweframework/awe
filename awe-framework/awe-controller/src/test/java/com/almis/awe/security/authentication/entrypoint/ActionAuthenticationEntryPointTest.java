package com.almis.awe.security.authentication.entrypoint;

import com.almis.awe.session.AweSessionDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;

import static org.mockito.Mockito.*;

class ActionAuthenticationEntryPointTest {

	@Test
	void testCommence_WithSession() throws Exception {
		// Mock dependencies
		AweSessionDetails sessionDetails = mock(AweSessionDetails.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		AuthenticationException authException = mock(AuthenticationException.class);
		ActionAuthenticationEntryPoint entryPoint = new ActionAuthenticationEntryPoint(sessionDetails);
		PrintWriter writer = mock(PrintWriter.class);

		// Set up request and response
		when(request.getSession(false)).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
		when(request.getSession(false).getId()).thenReturn("session-id");
		when(request.getRequestURI()).thenReturn("/test/action");
		when(response.getWriter()).thenReturn(writer);

		// Execute method
		entryPoint.commence(request, response, authException);

		// Verify behavior
		verify(sessionDetails, times(1)).onLogoutSuccess();
		verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(response, times(1)).setContentType("text/plain;charset=UTF-8");
		verify(writer, times(1)).write("ERROR_MESSAGE_SESSION_EXPIRED");
		verify(writer, times(1)).flush();
	}

	@Test
	void testCommence_WithoutSession() throws Exception {
		// Mock dependencies
		AweSessionDetails sessionDetails = mock(AweSessionDetails.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		AuthenticationException authException = mock(AuthenticationException.class);
		ActionAuthenticationEntryPoint entryPoint = new ActionAuthenticationEntryPoint(sessionDetails);
		PrintWriter writer = mock(PrintWriter.class);

		// Set up request and response
		when(request.getSession(false)).thenReturn(null);
		when(request.getRequestURI()).thenReturn("/test/action");
		when(response.getWriter()).thenReturn(writer);

		// Execute method
		entryPoint.commence(request, response, authException);

		// Verify behavior
		verify(sessionDetails, times(1)).onLogoutSuccess();
		verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(response, times(1)).setContentType("text/plain;charset=UTF-8");
		verify(writer, times(1)).write("ERROR_MESSAGE_SESSION_EXPIRED");
		verify(writer, times(1)).flush();
	}
}