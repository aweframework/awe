package com.almis.awe.service;

import com.almis.awe.model.component.AweElements;
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
    void testGenerateErrorPageFromTemplate_WithAllParameters() {
        // Given
        String errorTitle = "Custom Error Title";
        String errorMessage = "Custom error message";
        String expectedHtml = "<html><body>Error Page</body></html>";

        // Mock getLocale method using reflection to simulate ServiceConfig behavior
        ErrorPageService spyService = spy(errorPageService);
        doReturn("Error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");

        when(templateEngine.process(eq("error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTitle, errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithNullTitle() {
        // Given
        String errorMessage = "System error occurred";
        String expectedHtml = "<html><body>System Error Page</body></html>";

        ErrorPageService spyService = spy(errorPageService);
        doReturn("Error Title").when(spyService).getLocale("SCREEN_TEXT_ERROR_TITLE");
        doReturn("Error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");

        when(templateEngine.process(eq("error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(null, errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithNullMessage() {
        // Given
        String errorTitle = "Validation Failed";
        String expectedHtml = "<html><body>Validation Error Page</body></html>";

        ErrorPageService spyService = spy(errorPageService);
        doReturn("Unknown error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_UNKNOWN");
        doReturn("Error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");

        when(templateEngine.process(eq("error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTitle, null);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithBothNullTitleAndMessage() {
        // Given
        String expectedHtml = "<html><body>Unknown Error Page</body></html>";

        ErrorPageService spyService = spy(errorPageService);
        doReturn("Error Title").when(spyService).getLocale("SCREEN_TEXT_ERROR_TITLE");
        doReturn("Unknown error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_UNKNOWN");
        doReturn("Error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");

        when(templateEngine.process(eq("error"), any(Context.class))).thenReturn(expectedHtml);

        // When
        String result = spyService.generateErrorPageFromTemplate( null, null);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
        verify(templateEngine).process(eq("error"), any(Context.class));
    }

    @Test
    void testGenerateErrorPageFromTemplate_WithTemplateEngineException() {
        // Given
        String errorTitle = "Network Error";
        String errorMessage = "Connection failed";

        ErrorPageService spyService = spy(errorPageService);
        doReturn("Error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");

        // Mock template engine to throw exception
        when(templateEngine.process(eq("error"), any(Context.class)))
            .thenThrow(new RuntimeException("Template processing failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> spyService.generateErrorPageFromTemplate(errorTitle, errorMessage));

        assertEquals("Template processing failed", exception.getMessage());
    }

    @Test
    void testGenerateErrorPageFromTemplate_ContextVariables() {
        // Given
        String errorTitle = "Data Access Error";
        String errorMessage = "Database connection failed";
        String expectedHtml = "<html><body>Data Access Error Page</body></html>";
        
        ErrorPageService spyService = spy(errorPageService);
        doReturn("Error occurred").when(spyService).getLocale("SCREEN_TEXT_ERROR_PREFIX");
        doReturn("Back").when(spyService).getLocale("BUTTON_BACK");
        doReturn("Home").when(spyService).getLocale("BUTTON_HOME");

        when(templateEngine.process(eq("error"), any(Context.class))).thenAnswer(invocation -> {
            Context context = invocation.getArgument(1);
            
            // Verify context variables are set correctly
            assertEquals("⚠", context.getVariable("icon"));
            assertEquals(errorTitle, context.getVariable("title"));
            assertEquals(errorMessage, context.getVariable("message"));
            assertEquals("Error occurred", context.getVariable("messagePrefix"));
            assertEquals("Back", context.getVariable("backButtonText"));
            assertEquals("Home", context.getVariable("homeButtonText"));
            assertEquals("/", context.getVariable("homeUrl"));
            
            return expectedHtml;
        });

        // When
        String result = spyService.generateErrorPageFromTemplate(errorTitle, errorMessage);

        // Then
        assertNotNull(result);
        assertEquals(expectedHtml, result);
    }

    @Test
    void testTemplateConstant() {
        // Test that the template constant is correctly defined
        assertEquals("error", ErrorPageService.TEMPLATE_PAGE_ERROR);
    }
}