package com.almis.awe.service.screen;

import com.almis.awe.model.dto.CellData;

import java.util.List;
import java.util.Map;

/**
 * Subject a screen is rendered for by the personalization editor: everyone (no ids), a profile (profile id) or a user
 * (user id and the profile id of that user). Nothing is stored and no session is opened as the subject.
 *
 * @param userId    User id (null when the subject is not a user)
 * @param profileId Profile id (null when the subject is everyone)
 */
public record ScreenSubject(Integer userId, Integer profileId) {

  /**
   * Keep the rules the subject would get: the ones of everyone, of its profile and of its own
   *
   * @param rules   Rules (rows with userId and profileId columns)
   * @param subject Subject
   * @return Rules of the subject
   */
  public static List<Map<String, CellData>> filter(List<Map<String, CellData>> rules, ScreenSubject subject) {
    return rules.stream().filter(subject::covers).toList();
  }

  private boolean covers(Map<String, CellData> rule) {
    String ruleUser = rule.get("userId").getStringValue();
    String ruleProfile = rule.get("profileId").getStringValue();
    // As getScreenConfiguration: a rule with a user and a profile needs both to match
    boolean userMatches = ruleUser.isEmpty() || ruleUser.equals(String.valueOf(userId));
    return userMatches && (ruleProfile.isEmpty() || ruleProfile.equals(String.valueOf(profileId)));
  }
}
