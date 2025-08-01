package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.type.ErrorTypology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ErrorPageService
 */
@ExtendWith(MockitoExtension.class)
class ErrorPageServiceTest {

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private AweElements aweElements;

    @Mock
    private EnumeratedGroup enumeratedGroup;

    @Mock
    private ApplicationContext applicationContext;

    private ErrorPageService errorPageService;

    @BeforeEach
    void setUp() {
        errorPageService = new ErrorPageService(templateEngine);
        
        // Mock the ApplicationContext to return AweElements (lenient to avoid unnecessary stubbing exceptions)
        lenient().when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
        ReflectionTestUtils.setField(errorPageService, "context", applicationContext);
    }

    @Test
    void testConstructor() {
        // Test constructor injection
        ErrorPageService service = new ErrorPageService(templateEngine);
        assertNotNull(service);
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithAllParameters() throws AWException {
        // Given
        ErrorTypology errorTypology = ErrorTypology.AUTHENTICATION;
        String errorTitle = "Custom Error Title";
        String errorMessage = "Custom error message";
        String expectedHtml = "<html><body>Error Page</body></html>";

        // Mock the locale and enumerated methods
        when(aweElements.getEnumerated("ErrorTypology")).thenReturn(enumeratedGroup);
        when(enumeratedGroup.findLabel("AUTHENTICATION")).thenReturn("Authentication Error");
        
        // Mock getLocale method using reflection to simulate ServiceConfig behavior
        ErrorPageService spyService = spy(errorPageService);
        doReturn("Authentication Error").when(spyService).getLocale("Authentication Error");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");
        doReturn("Error occurred: Authentication Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX", "Authentication Error");

        when(templateEngine.process(eq("page-error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTypology, errorTitle, errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("page-error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithNullTitle() throws AWException {
        // Given
        ErrorTypology errorTypology = ErrorTypology.SYSTEM;
        String errorMessage = "System error occurred";
        String expectedHtml = "<html><body>System Error Page</body></html>";

        // Mock the locale and enumerated methods
        when(aweElements.getEnumerated("ErrorTypology")).thenReturn(enumeratedGroup);
        when(enumeratedGroup.findLabel("SYSTEM")).thenReturn("System Error");
        
        ErrorPageService spyService = spy(errorPageService);
        doReturn("System Error").when(spyService).getLocale("System Error");
        doReturn("Error Title: System Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_TITLE", "System Error");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");
        doReturn("Error occurred: System Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX", "System Error");

        when(templateEngine.process(eq("page-error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTypology, null, errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("page-error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithNullMessage() throws AWException {
        // Given
        ErrorTypology errorTypology = ErrorTypology.VALIDATION;
        String errorTitle = "Validation Failed";
        String expectedHtml = "<html><body>Validation Error Page</body></html>";

        // Mock the locale and enumerated methods
        when(aweElements.getEnumerated("ErrorTypology")).thenReturn(enumeratedGroup);
        when(enumeratedGroup.findLabel("VALIDATION")).thenReturn("Validation Error");
        
        ErrorPageService spyService = spy(errorPageService);
        doReturn("Validation Error").when(spyService).getLocale("Validation Error");
        doReturn("Unknown error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_UNKNOWN");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");
        doReturn("Error occurred: Validation Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX", "Validation Error");

        when(templateEngine.process(eq("page-error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTypology, errorTitle, null);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("page-error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithBothNullTitleAndMessage() throws AWException {
        // Given
        ErrorTypology errorTypology = ErrorTypology.UNKNOWN;
        String expectedHtml = "<html><body>Unknown Error Page</body></html>";

        // Mock the locale and enumerated methods
        when(aweElements.getEnumerated("ErrorTypology")).thenReturn(enumeratedGroup);
        when(enumeratedGroup.findLabel("UNKNOWN")).thenReturn("Unknown Error");
        
        ErrorPageService spyService = spy(errorPageService);
        doReturn("Unknown Error").when(spyService).getLocale("Unknown Error");
        doReturn("Error Title: Unknown Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_TITLE", "Unknown Error");
        doReturn("Unknown error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_UNKNOWN");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");
        doReturn("Error occurred: Unknown Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX", "Unknown Error");

        when(templateEngine.process(eq("page-error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTypology, null, null);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("page-error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_ThrowsAWERuntimeException() throws AWException {
        // Given
        ErrorTypology errorTypology = ErrorTypology.NETWORK;
        String errorTitle = "Network Error";
        String errorMessage = "Connection failed";

        // Mock to throw AWException
        when(aweElements.getEnumerated("ErrorTypology")).thenThrow(new AWException("Mocked AWException"));

        // When & Then
        AWERuntimeException exception = assertThrows(AWERuntimeException.class, () -> errorPageService.generateErrorPageFromTemplate(errorTypology, errorTitle, errorMessage));

        assertNotNull(exception.getCause());
			  assertInstanceOf(AWException.class, exception.getCause());
        assertEquals("Mocked AWException", exception.getCause().getMessage());
    }

    @Test
    void testGenerateErrorPageFromTemplate_ContextVariables() throws AWException {
        // Given
        ErrorTypology errorTypology = ErrorTypology.DATA_ACCESS;
        String errorTitle = "Data Access Error";
        String errorMessage = "Database connection failed";
        String expectedHtml = "<html><body>Data Access Error Page</body></html>";

        // Mock the locale and enumerated methods
        when(aweElements.getEnumerated("ErrorTypology")).thenReturn(enumeratedGroup);
        when(enumeratedGroup.findLabel("DATA_ACCESS")).thenReturn("Data Access Error");
        
        ErrorPageService spyService = spy(errorPageService);
        doReturn("Data Access Error").when(spyService).getLocale("Data Access Error");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");
        doReturn("Error occurred: Data Access Error").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX", "Data Access Error");

        when(templateEngine.process(eq("page-error"), any(Context.class))).thenAnswer(invocation -> {
            Context context = invocation.getArgument(1);
            
            // Verify context variables are set correctly
            assertEquals("⚠", context.getVariable("icon"));
            assertEquals(errorTitle, context.getVariable("title"));
            assertEquals(errorMessage, context.getVariable("message"));
            assertEquals("Error occurred: Data Access Error", context.getVariable("messagePrefix"));
            assertEquals("Back", context.getVariable("backButtonText"));
            assertEquals("Home", context.getVariable("homeButtonText"));
            assertEquals("/", context.getVariable("homeUrl"));
            
            return expectedHtml;
        });

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTypology, errorTitle, errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
    }

    @Test
    void testTemplateConstant() {
        // Test that the template constant is correctly defined
        assertEquals("page-error", ErrorPageService.TEMPLATE_PAGE_ERROR);
    }
}