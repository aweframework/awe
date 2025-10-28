package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Service responsible for generating error pages using Thymeleaf templates.
 * It extends the base {@code ServiceConfig} class, leveraging its localization
 * and dependency injection capabilities to process and deliver localized error
 * messages in the form of an HTML page.
 */
@Service
public class ErrorPageService extends ServiceConfig {

	// Constants
	static final String TEMPLATE_PAGE_ERROR = "error";

	// Autowired dependencies
	private final SpringTemplateEngine templateEngine;

	@Autowired
	public ErrorPageService(SpringTemplateEngine springTemplateEngine) {
		this.templateEngine = springTemplateEngine;
	}

	/**
	 * Generates an HTML error page using the Spring template engine (Thymeleaf)
	 *
	 * @param errorTitle The error title to display
	 * @param errorMessage The error message to display
	 * @return HTML string containing the error page
	 */
	public String generateErrorPageFromTemplate(String errorTitle, String errorMessage) {
		Context context = new Context();

		context.setVariable("icon", "⚠");
		context.setVariable("title", errorTitle != null ? errorTitle : getLocale("SCREEN_TEXT_ERROR_TITLE"));
		context.setVariable("message", errorMessage != null ? errorMessage : getLocale("SCREEN_TEXT_ERROR_UNKNOWN"));
		context.setVariable("messagePrefix", getLocale("SCREEN_TEXT_ERROR_PREFIX"));
		context.setVariable("backButtonText", getLocale("BUTTON_BACK"));
		context.setVariable("homeButtonText", getLocale("BUTTON_HOME"));
		context.setVariable("homeUrl", "/");

		return templateEngine.process(TEMPLATE_PAGE_ERROR, context);
	}
}
