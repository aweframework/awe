package com.almis.awe.model.dto;

import java.io.Serializable;

/**
 * Additional profile of a user (AweOpeRol): a profile the user has besides the main one
 *
 * @param acronym     Profile acronym (AwePro.Acr)
 * @param name        Profile name (AwePro.Nam), used for the Spring Security role
 * @param restriction Restriction file of the profile (AwePro.Res), null when the profile has none
 */
public record UserProfile(String acronym, String name, String restriction) implements Serializable {
}
