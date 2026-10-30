package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The favourites and avatar of a user only change for the user of the session: the click on a favourite takes the user
 * from the session, and the targets that write them are server-only, so a client that launches them through
 * /action/maintain cannot add, remove or replace the favourites or the avatar of any user
 */
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
public abstract class AbstractUserTargetsSessionTest extends AbstractSpringAppIntegrationTest {

  private static final String USER = "test";
  private static final String OTHER_USER = "other-user-885";
  private static final String USER_FIELD = "\"user\":\"";
  private static final String FAVOURITES_TABLE = "AweUsrFav";
  private static final String SETTINGS_TABLE = "AweUserSettings";
  private static final String THEMES_OPTION = "themes";

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbc;
  private MockHttpSession session;

  @BeforeEach
  void setUpSession() throws Exception {
    jdbc = new JdbcTemplate(dataSource);
    deleteUserData();
    session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", USER).session(session))
      .andExpect(status().isOk());
  }

  @AfterEach
  void deleteUserData() {
    jdbc.update("DELETE FROM AweUsrFav WHERE Ope IN (?, ?)", USER, OTHER_USER);
    jdbc.update("DELETE FROM AweUserSettings WHERE Ope IN (?, ?)", USER, OTHER_USER);
  }

  @Test
  void clickFavouriteOnlyAddsTheFavouritesOfTheUserOfTheSession() throws Exception {
    maintain("clickFavourite", USER_FIELD + OTHER_USER + "\",\"currentOption\":\"themes\"");

    assertThat(count(FAVOURITES_TABLE, OTHER_USER)).isZero();
    assertThat(jdbc.queryForList("SELECT Opt FROM AweUsrFav WHERE Ope = ?", String.class, USER)).containsExactly(THEMES_OPTION);
  }

  @Test
  void clickFavouriteDoesNotRemoveTheFavouritesOfAnotherUser() throws Exception {
    jdbc.update("INSERT INTO AweUsrFav (IdeFav, Ope, Opt, Ord) VALUES (?, ?, ?, ?)", 885001, OTHER_USER, THEMES_OPTION, 1);

    maintain("clickFavourite", USER_FIELD + OTHER_USER + "\",\"currentOption\":\"themes\"");

    assertThat(count(FAVOURITES_TABLE, OTHER_USER)).isOne();
  }

  @Test
  void theServerOnlyFavouriteAndAvatarTargetsAreRefusedForAClient() throws Exception {
    jdbc.update("INSERT INTO AweUsrFav (IdeFav, Ope, Opt, Ord) VALUES (?, ?, ?, ?)", 885001, OTHER_USER, THEMES_OPTION, 1);
    jdbc.update("INSERT INTO AweUserSettings (IdeUsrSet, Ope, AvatarImage) VALUES (?, ?, ?)", 885001, OTHER_USER, "other-token");

    // Even for the user of the session: these targets are only launched by the server
    for (String user : new String[]{OTHER_USER, USER}) {
      maintain("addToFavourites", USER_FIELD + user + "\",\"option\":\"users\"");
      maintain("removeFromFavourites", USER_FIELD + user + "\",\"option\":\"themes\"");
      maintain("updateUserAvatar", USER_FIELD + user + "\",\"avatarToken\":\"forged-token\"");
      maintain("insertUserAvatar", USER_FIELD + user + "2\",\"avatarToken\":\"forged-token\"");
    }

    assertThat(jdbc.queryForList("SELECT Opt FROM AweUsrFav WHERE Ope = ?", String.class, OTHER_USER)).containsExactly(THEMES_OPTION);
    assertThat(count(FAVOURITES_TABLE, USER)).isZero();
    assertThat(jdbc.queryForObject("SELECT AvatarImage FROM AweUserSettings WHERE Ope = ?", String.class, OTHER_USER))
      .isEqualTo("other-token");
    assertThat(count(SETTINGS_TABLE, USER)).isZero();
    assertThat(count(SETTINGS_TABLE, USER + "2")).isZero();
    assertThat(count(SETTINGS_TABLE, OTHER_USER + "2")).isZero();
  }

  private int count(String table, String user) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE Ope = ?", Integer.class, user);
  }

  private void maintain(String target, String variables) throws Exception {
    mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{" + variables + ",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }
}
