package com.almis.awe.model.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorTypology enum
 */
class ErrorTypologyTest {

    @Test
    void testEnumValues() {
        // Test that all expected enum values exist
        ErrorTypology[] values = ErrorTypology.values();
        assertEquals(9, values.length);
        
        // Test specific enum values
        assertNotNull(ErrorTypology.AUTHENTICATION);
        assertNotNull(ErrorTypology.DATA_ACCESS);
        assertNotNull(ErrorTypology.AUTHORIZATION);
        assertNotNull(ErrorTypology.VALIDATION);
        assertNotNull(ErrorTypology.SYSTEM);
        assertNotNull(ErrorTypology.NETWORK);
        assertNotNull(ErrorTypology.FILE_RESOURCE);
        assertNotNull(ErrorTypology.CONFIGURATION);
        assertNotNull(ErrorTypology.UNKNOWN);
    }

    @Test
    void testGetValue() {
        // Test getValue() method for each enum
        assertEquals("AUTHENTICATION", ErrorTypology.AUTHENTICATION.getValue());
        assertEquals("DATA_ACCESS", ErrorTypology.DATA_ACCESS.getValue());
        assertEquals("AUTHORIZATION", ErrorTypology.AUTHORIZATION.getValue());
        assertEquals("VALIDATION", ErrorTypology.VALIDATION.getValue());
        assertEquals("SYSTEM", ErrorTypology.SYSTEM.getValue());
        assertEquals("NETWORK", ErrorTypology.NETWORK.getValue());
        assertEquals("FILE_RESOURCE", ErrorTypology.FILE_RESOURCE.getValue());
        assertEquals("CONFIGURATION", ErrorTypology.CONFIGURATION.getValue());
        assertEquals("UNKNOWN", ErrorTypology.UNKNOWN.getValue());
    }

    @Test
    void testToString() {
        // Test toString() method for each enum
        assertEquals("AUTHENTICATION", ErrorTypology.AUTHENTICATION.toString());
        assertEquals("DATA_ACCESS", ErrorTypology.DATA_ACCESS.toString());
        assertEquals("AUTHORIZATION", ErrorTypology.AUTHORIZATION.toString());
        assertEquals("VALIDATION", ErrorTypology.VALIDATION.toString());
        assertEquals("SYSTEM", ErrorTypology.SYSTEM.toString());
        assertEquals("NETWORK", ErrorTypology.NETWORK.toString());
        assertEquals("FILE_RESOURCE", ErrorTypology.FILE_RESOURCE.toString());
        assertEquals("CONFIGURATION", ErrorTypology.CONFIGURATION.toString());
        assertEquals("UNKNOWN", ErrorTypology.UNKNOWN.toString());
    }

    @Test
    void testValueOf() {
        // Test valueOf() method
        assertEquals(ErrorTypology.AUTHENTICATION, ErrorTypology.valueOf("AUTHENTICATION"));
        assertEquals(ErrorTypology.DATA_ACCESS, ErrorTypology.valueOf("DATA_ACCESS"));
        assertEquals(ErrorTypology.AUTHORIZATION, ErrorTypology.valueOf("AUTHORIZATION"));
        assertEquals(ErrorTypology.VALIDATION, ErrorTypology.valueOf("VALIDATION"));
        assertEquals(ErrorTypology.SYSTEM, ErrorTypology.valueOf("SYSTEM"));
        assertEquals(ErrorTypology.NETWORK, ErrorTypology.valueOf("NETWORK"));
        assertEquals(ErrorTypology.FILE_RESOURCE, ErrorTypology.valueOf("FILE_RESOURCE"));
        assertEquals(ErrorTypology.CONFIGURATION, ErrorTypology.valueOf("CONFIGURATION"));
        assertEquals(ErrorTypology.UNKNOWN, ErrorTypology.valueOf("UNKNOWN"));
    }

    @Test
    void testValueOfInvalidValue() {
        // Test valueOf() with invalid value throws exception
        assertThrows(IllegalArgumentException.class, () -> ErrorTypology.valueOf("INVALID_VALUE"));
    }

    @Test
    void testEnumConsistency() {
        // Test that getValue() and toString() return the same value
        for (ErrorTypology typology : ErrorTypology.values()) {
            assertEquals(typology.getValue(), typology.toString());
        }
    }
}