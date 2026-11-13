package com.almis.awe.samples.helpdesk.chat;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rules of the ticket a visitor opens through the public support chat. The client is never trusted: the honeypot, the
 * limit of the session and every field are checked here, on the server
 */
public final class ChatTicketRules {

  /** Tickets a session may open through the chat */
  public static final int SESSION_LIMIT = 3;

  private static final int EMAIL_MAX = 80;
  private static final int DESCRIPTION_MIN = 5;
  private static final int DESCRIPTION_MAX = 1000;
  private static final int SUBJECT_MAX = 120;
  private static final int NAME_MAX = 60;
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)*\\.[^@\\s.]{2,}$");

  /**
   * What the server does with a request
   */
  public enum Outcome {
    /** Create the ticket */
    CREATE,
    /** A bot filled the hidden field: answer as received and create nothing */
    HONEYPOT,
    /** The session already opened the tickets it may */
    LIMIT,
    /** The email is not valid */
    EMAIL,
    /** The category is not one of the enumerated list */
    CATEGORY,
    /** The description is missing, too short or too long */
    DESCRIPTION
  }

  private ChatTicketRules() {
  }

  /**
   * Decide what to do with a request. The honeypot goes first, so a bot learns nothing from the other answers
   *
   * @param honeypot    Hidden field: a person leaves it empty
   * @param email       Email of the requester
   * @param category    Category of the ticket
   * @param description Description of the problem
   * @param categories  Categories of the enumerated list
   * @param created     Tickets the session already opened
   * @param limit       Tickets a session may open
   * @return Outcome
   */
  public static Outcome evaluate(String honeypot, String email, String category, String description,
                                 Collection<String> categories, int created, int limit) {
    if (honeypot != null && !honeypot.isBlank()) {
      return Outcome.HONEYPOT;
    }
    if (created >= limit) {
      return Outcome.LIMIT;
    }
    if (!isEmail(email)) {
      return Outcome.EMAIL;
    }
    if (category == null || !categories.contains(category)) {
      return Outcome.CATEGORY;
    }
    int length = description == null ? 0 : description.trim().length();
    if (length < DESCRIPTION_MIN || length > DESCRIPTION_MAX) {
      return Outcome.DESCRIPTION;
    }
    return Outcome.CREATE;
  }

  /**
   * Normalise an email: trimmed and in lower case
   *
   * @param email Email
   * @return Normalised email
   */
  public static String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Subject of the ticket: the first line of the description, cut to the size of the column
   *
   * @param description Description
   * @return Subject
   */
  public static String subject(String description) {
    String firstLine = description.trim().lines().findFirst().orElse("").trim();
    return firstLine.length() <= SUBJECT_MAX ? firstLine : firstLine.substring(0, SUBJECT_MAX - 3) + "...";
  }

  /**
   * Name of a requester that only gave an email: the name of the mailbox
   *
   * @param email Normalised email
   * @return Name
   */
  public static String requesterName(String email) {
    String name = email.substring(0, email.indexOf('@'));
    return name.length() <= NAME_MAX ? name : name.substring(0, NAME_MAX);
  }

  private static boolean isEmail(String email) {
    String value = normalizeEmail(email);
    return value.length() <= EMAIL_MAX && EMAIL.matcher(value).matches();
  }
}
