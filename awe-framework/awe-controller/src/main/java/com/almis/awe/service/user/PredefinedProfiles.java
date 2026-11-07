package com.almis.awe.service.user;

import java.util.List;
import java.util.Optional;

/**
 * Predefined profiles of AWE: Reader ({@code RDR}) and Contributor ({@code CTB}). Their permissions are in their
 * profile files, so they are created at startup ({@link PredefinedProfileSeed}) and cannot be changed from the
 * profiles screen: they are duplicated and the copy is changed. A profile is predefined when it has the acronym and the
 * profile file of one of them, so a profile of the application with the same acronym and another file is not
 */
public final class PredefinedProfiles {

  /**
   * Predefined profiles
   */
  public static final List<PredefinedProfile> PROFILES = List.of(
    new PredefinedProfile("RDR", "Reader", "reader"),
    new PredefinedProfile("CTB", "Contributor", "contributor"));

  private PredefinedProfiles() {
  }

  /**
   * Find the predefined profile of a stored profile
   *
   * @param acronym     Profile acronym (AwePro.Acr)
   * @param restriction Profile file (AwePro.Res)
   * @return Predefined profile, empty when the profile is not predefined
   */
  public static Optional<PredefinedProfile> find(String acronym, String restriction) {
    if (acronym == null || restriction == null) {
      return Optional.empty();
    }
    return PROFILES.stream()
      .filter(profile -> profile.acronym().equalsIgnoreCase(acronym.trim()))
      .filter(profile -> profile.file().equals(restriction.trim()))
      .findFirst();
  }

  /**
   * Predefined profile
   *
   * @param acronym Profile acronym (AwePro.Acr)
   * @param name    Profile name (AwePro.Nam)
   * @param file    Profile file (AwePro.Res)
   */
  public record PredefinedProfile(String acronym, String name, String file) {
  }
}
