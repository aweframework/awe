package com.almis.awe.autoconfigure;

import com.almis.awe.controller.CustomErrorController;
import com.almis.awe.service.ErrorPageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for setting up the custom error controller.
 * This class provides a bean definition for the {@code CustomErrorController}
 * to handle error pages within the application. It is configured to initialize
 * before the default Spring Boot {@code ErrorMvcAutoConfiguration}.
 * <p>
 * The custom error controller bean is created only if no other
 * {@code CustomErrorController} bean is already defined in the application context.
 * <p>
 * An instance of {@code ErrorPageService} is required as a dependency
 * for the {@code CustomErrorController}.
 * <p>
 * Annotations:
 * - {@code @Configuration}: Marks this class as a Spring configuration class.
 * - {@code @AutoConfiguration(before = ErrorMvcAutoConfiguration.class)}: Indicates
 *   that this configuration should be applied before the default error MVC autoconfiguration.
 * - {@code @Bean}: Indicates that the method will produce a bean to be managed by the Spring container.
 * - {@code @ConditionalOnMissingBean}: Ensures the bean is registered only if
 *   no other bean of the same type is already present.
 */
@Configuration
@AutoConfiguration(before = ErrorMvcAutoConfiguration.class)
public class ErrorControllerConfig {

	@Bean
	@ConditionalOnMissingBean
	public CustomErrorController customErrorController(ErrorPageService errorPageService) {
		return new CustomErrorController(errorPageService);
	}
}
