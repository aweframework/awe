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
 * The favourites and avatar targets of a user only change the data of the user of the session: a client that launches
 * them through /action/maintain cannot add, remove or replace the favourites or the avatar of another user
 */
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
public abstract class AbstractUserTargetsSessionTest extends AbstractSpringAppIntegrationTest {

  private static final String USER = "test";
  private static final String OTHER_USER = "other-user-885";

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
  void addToFavouritesOnlyAddsTheFavouritesOfTheUserOfTheSession() throws Exception {
    maintain("addToFavourites", "\"user\":\"" + OTHER_USER + "\",\"option\":\"themes\"");

    assertThat(count("AweUsrFav", OTHER_USER)).isZero();
    assertThat(jdbc.queryForList("SELECT Opt FROM AweUsrFav WHERE Ope = ?", String.class, USER)).containsExactly("themes");
  }

  @Test
  void removeFromFavouritesOnlyRemovesTheFavouritesOfTheUserOfTheSession() throws Exception {
    jdbc.update("INSERT INTO AweUsrFav (IdeFav, Ope, Opt, Ord) VALUES (?, ?, ?, ?)", 885001, OTHER_USER, "themes", 1);

    maintain("removeFromFavourites", "\"user\":\"" + OTHER_USER + "\",\"option\":\"themes\"");

    assertThat(count("AweUsrFav", OTHER_USER)).isOne();
  }

  @Test
  void theAvatarTargetsOnlyChangeTheAvatarOfTheUserOfTheSession() throws Exception {
    jdbc.update("INSERT INTO AweUserSettings (IdeUsrSet, Ope, AvatarImage) VALUES (?, ?, ?)", 885001, OTHER_USER, "other-token");

    maintain("updateUserAvatar", "\"user\":\"" + OTHER_USER + "\",\"avatarToken\":\"forged-token\"");
    maintain("insertUserAvatar", "\"user\":\"" + OTHER_USER + "2\",\"avatarToken\":\"forged-token\"");

    assertThat(jdbc.queryForObject("SELECT AvatarImage FROM AweUserSettings WHERE Ope = ?", String.class, OTHER_USER))
      .isEqualTo("other-token");
    assertThat(count("AweUserSettings", OTHER_USER + "2")).isZero();
    assertThat(jdbc.queryForObject("SELECT AvatarImage FROM AweUserSettings WHERE Ope = ?", String.class, USER))
      .isEqualTo("forged-token");
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
