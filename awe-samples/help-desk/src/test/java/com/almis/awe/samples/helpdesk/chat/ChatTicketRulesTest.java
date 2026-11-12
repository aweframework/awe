package com.almis.awe.samples.helpdesk.chat;

import com.almis.awe.samples.helpdesk.chat.ChatTicketRules.Outcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules of the public chat ticket: a client is never trusted, so every one of them is checked on the server
 */
class ChatTicketRulesTest {

  private static final List<String> CATEGORIES = List.of("NETWORK", "SOFTWARE", "OTHER");
  private static final String PROBLEM = "The guest Wi-Fi asks for a password I do not have";

  private Outcome evaluate(String honeypot, String email, String category, String description, int created) {
    return ChatTicketRules.evaluate(honeypot, email, category, description, CATEGORIES, created, 3);
  }

  @Test
  void acceptsAValidRequest() {
    assertThat(evaluate("", "ana@mail.example", "NETWORK", PROBLEM, 0)).isEqualTo(Outcome.CREATE);
    assertThat(evaluate(null, " Ana@Mail.Example ", "OTHER", PROBLEM, 2)).isEqualTo(Outcome.CREATE);
  }

  @Test
  void aFilledHoneypotIsSilentlyDropped() {
    assertThat(evaluate("http://spam.example", "ana@mail.example", "NETWORK", PROBLEM, 0)).isEqualTo(Outcome.HONEYPOT);
    // Even an invalid request: a bot gets no hint of what was wrong
    assertThat(evaluate("x", "nonsense", "NOPE", "", 9)).isEqualTo(Outcome.HONEYPOT);
  }

  @Test
  void refusesTheTicketsAboveTheLimitOfTheSession() {
    assertThat(evaluate("", "ana@mail.example", "NETWORK", PROBLEM, 3)).isEqualTo(Outcome.LIMIT);
    assertThat(evaluate("", "ana@mail.example", "NETWORK", PROBLEM, 4)).isEqualTo(Outcome.LIMIT);
  }

  @Test
  void refusesAnInvalidEmail() {
    for (String email : new String[]{null, "", "ana", "ana@", "@mail.example", "ana@mail", "a na@mail.example", "ana@@mail.example",
      "a".repeat(80) + "@mail.example"}) {
      assertThat(evaluate("", email, "NETWORK", PROBLEM, 0)).as("email " + email).isEqualTo(Outcome.EMAIL);
    }
  }

  @Test
  void refusesACategoryThatIsNotInTheEnumeratedList() {
    assertThat(evaluate("", "ana@mail.example", "BILLING", PROBLEM, 0)).isEqualTo(Outcome.CATEGORY);
    assertThat(evaluate("", "ana@mail.example", null, PROBLEM, 0)).isEqualTo(Outcome.CATEGORY);
  }

  @Test
  void refusesAMissingTooShortOrTooLongDescription() {
    assertThat(evaluate("", "ana@mail.example", "NETWORK", null, 0)).isEqualTo(Outcome.DESCRIPTION);
    assertThat(evaluate("", "ana@mail.example", "NETWORK", "  hi  ", 0)).isEqualTo(Outcome.DESCRIPTION);
    assertThat(evaluate("", "ana@mail.example", "NETWORK", "a".repeat(1001), 0)).isEqualTo(Outcome.DESCRIPTION);
    assertThat(evaluate("", "ana@mail.example", "NETWORK", "a".repeat(1000), 0)).isEqualTo(Outcome.CREATE);
  }

  @Test
  void theSubjectIsTheFirstLineOfTheDescriptionAndFitsTheColumn() {
    assertThat(ChatTicketRules.subject("Cannot print\nThe queue is stuck")).isEqualTo("Cannot print");
    assertThat(ChatTicketRules.subject("a".repeat(300))).hasSize(120).endsWith("...");
  }

  @Test
  void theRequesterNameIsTheNameOfTheMailbox() {
    assertThat(ChatTicketRules.requesterName("ana.torres@mail.example")).isEqualTo("ana.torres");
    assertThat(ChatTicketRules.requesterName("a".repeat(70) + "@mail.example")).hasSize(60);
  }
}
