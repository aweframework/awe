package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Saved searches of the advanced block of the windows (#790)
 */
@Data
@ConfigurationProperties(prefix = "awe.saved-search")
public class SavedSearchConfigProperties {
  /**
   * Menu option that gives access to publish searches for a profile: a user whose restricted menu holds this option,
   * not restricted, publishes, renames and deletes the searches of any profile. Users without access to it (or when
   * the option is not in the menu) only manage their own searches. Default value profiles
   */
  private String publishOption = "profiles";
}
