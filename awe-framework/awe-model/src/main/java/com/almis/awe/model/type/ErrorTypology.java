package com.almis.awe.model.type;

import lombok.Getter;

/**
 * Enum for defining different error typologies
 * Used for categorizing errors and providing appropriate localized messages
 */
@Getter
public enum ErrorTypology {
    
    /**
     * Authentication-related errors
     */
    AUTHENTICATION("AUTHENTICATION"),
    
    /**
     * Data access related errors
     */
    DATA_ACCESS("DATA_ACCESS"),
    
    /**
     * Authorization/ Permission-related errors
     */
    AUTHORIZATION("AUTHORIZATION"),
    
    /**
     * Validation-related errors
     */
    VALIDATION("VALIDATION"),
    
    /**
     * System/Technical errors
     */
    SYSTEM("SYSTEM"),
    
    /**
     * Network/ Connection-related errors
     */
    NETWORK("NETWORK"),
    
    /**
     * File/ Resource-related errors
     */
    FILE_RESOURCE("FILE_RESOURCE"),
    
    /**
     * Configuration-related errors
     */
    CONFIGURATION("CONFIGURATION"),
    
    /**
     * Unknown or generic errors
     */
    UNKNOWN("UNKNOWN");
    
    private final String value;
    
    ErrorTypology(String value) {
        this.value = value;
    }

	@Override
    public String toString() {
        return value;
    }
}