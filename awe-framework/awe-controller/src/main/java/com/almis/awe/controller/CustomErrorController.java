package com.almis.awe.controller;

import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.ErrorPageService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Order(-1)
@RestController
public class CustomErrorController implements ErrorController {

	private final ErrorPageService errorPageService;

	public CustomErrorController(ErrorPageService errorPageService) {
		this.errorPageService = errorPageService;
	}

	@GetMapping(value = "/error")
	public ResponseEntity<String> handleError(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

		int statusCode = status != null ? (Integer) status : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		String errorMessage = getErrorMessage(message, exception);

		// Determine error typology based on status code
		String errorTitle = getErrorTitle(statusCode);
		String errorPageHtml = errorPageService.generateErrorPageFromTemplate(errorTitle, errorMessage);

		return ResponseEntity.status(statusCode).body(errorPageHtml);
	}

	private String getErrorMessage(Object message, Object exception) {
		if (message instanceof String stringMessage && !stringMessage.trim().isEmpty()) {
			return stringMessage;
		}

		if (exception instanceof AWERuntimeException throwable) {
			Throwable throwableCause = throwable.getCause();
			if (throwableCause instanceof AWException causeException) {
				return causeException.getMessage();
			}
		} else if (exception instanceof Throwable exceptionException) {
			return exceptionException.getMessage();
		}

		return "Unknown error occurred";
	}

	private String getErrorTitle(int statusCode) {
		return switch (statusCode) {
			case 400 -> "Bad Request";
			case 401 -> "Unauthorized";
			case 403 -> "Forbidden";
			case 404 -> "Page Not Found";
			case 405 -> "Method Not Allowed";
			case 408 -> "Request Timeout";
			case 500 -> "Internal Server Error";
			case 502 -> "Bad Gateway";
			case 503 -> "Service Unavailable";
			case 504 -> "Gateway Timeout";
			default -> "Error " + statusCode;
		};
	}
}
