package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.scheduler.service.SchedulerService;
import com.almis.awe.service.FileService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * «Importar…» of the calendar side panel of the scheduler configuration screen (#993) through the action controller,
 * as the screen launches it: the file staged by the uploader is read with the real file service, its dates are
 * counted in the answer with the lines that cannot be read, nothing is saved and the staged file is deleted. A file
 * sent to the upload endpoint, as the uploader of the side panel sends it, passes the guard of the upload area and its
 * dates are read.
 */
@Tag("integration")
class SchedulerCalendarImportTest extends AbstractSchedulerMaintainTest {

  @Autowired
  private FileService fileService;
  @Autowired
  private SchedulerService schedulerService;
  @Autowired
  private BaseConfigProperties baseConfigProperties;

  @Test
  void theDatesOfAStagedFileAreReadWithoutSavingThemAndTheFileIsDeleted() throws Exception {
    FileData staged = fileService.uploadFile(new MockMultipartFile("file", "holidays.csv", "text/csv",
      "Fecha;Nombre\n25/12/2026;Navidad\nmañana\n2026-12-08;Inmaculada\n".getBytes(StandardCharsets.UTF_8)), null);
    Path stagedPath = Path.of(fileService.getFullPath(staged, false) + staged.getFileName());

    JsonNode actions = perform("maintain", "ImportConfigurationCalendarDates",
      params().put("CalDatImp", FileUtil.fileDataToString(staged)));

    assertThat(messages(actions)).containsExactly("warning: 2 dates read from the file. These lines could not be "
      + "read: 3.");
    assertThat(Files.exists(stagedPath)).isFalse();
  }

  @Test
  void aFileSentToTheUploadEndpointIsImportedAndDeleted() throws Exception {
    byte[] content = "25/12/2026;Navidad\n2026-12-08;Inmaculada\n".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile("file", "upload-endpoint-holidays.csv", "text/csv", content);
    // The uploader of the side panel has no destination
    mockMvc.perform(multipart("/file/upload").file(file).with(csrf())
        .param("address", "{}")
        .param("destination", "")
        .param(baseConfigProperties.getComponent().getUploadFileId(), "calendar-import-test")
        .header("Authorization", "calendar-import-test"))
      .andExpect(status().isOk());
    // The value the endpoint gives to the uploader (it broadcasts it): the staged file in the upload folder of its size
    FileData staged = new FileData(file.getOriginalFilename(), (long) content.length, "text/csv", "")
      .setBasePath(baseConfigProperties.getComponent().getUploadFilePath());
    Path stagedPath = Path.of(fileService.getFullPath(staged, true) + staged.getFileName());
    assertThat(Files.exists(stagedPath)).isTrue();

    List<Map<String, CellData>> rows = schedulerService
      .importConfigurationCalendarDates(FileUtil.fileDataToString(staged)).getDataList().getRows();

    assertThat(rows.stream().map(row -> row.get("Dat").getStringValue()).toList())
      .containsExactly("08/12/2026", "25/12/2026");
    assertThat(rows.stream().map(row -> row.get("Nam").getStringValue()).toList())
      .containsExactly("Inmaculada", "Navidad");
    assertThat(Files.exists(stagedPath)).isFalse();
  }

  private static List<String> messages(JsonNode actions) {
    List<String> messages = new ArrayList<>();
    for (JsonNode action : actions) {
      if ("message".equals(action.get("type").asString())) {
        messages.add(action.get("parameters").get("type").asString() + ": "
          + action.get("parameters").get("message").asString());
      }
    }
    return messages;
  }
}
