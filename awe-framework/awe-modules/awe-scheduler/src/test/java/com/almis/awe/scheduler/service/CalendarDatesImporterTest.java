package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * «Importar…» of the calendar side panel (#993): the file staged by the uploader is read once and deleted, its dates
 * are returned for the dates grid without saving them, with the lines that cannot be read; a file out of the upload
 * area, too large or of another format is refused with a generic message
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalendarDatesImporterTest {

  @Mock
  private FileService fileService;

  @TempDir
  Path folder;

  private CalendarDatesImporter importer;

  @BeforeEach
  void setUp() {
    importer = new CalendarDatesImporter(fileService,
      (key, parameters) -> parameters.length == 0 ? key : key + Arrays.toString(parameters));
    given(fileService.isPathWithinUploadArea(any())).willReturn(true);
    given(fileService.getFullPath(any(), anyBoolean())).willReturn(folder + "/");
  }

  @Test
  void theDatesOfTheFileAreReturnedWithTheLinesThatCannotBeReadAndTheFileIsDeleted() throws Exception {
    FileData file = stage("holidays.csv", "Fecha;Nombre\n25/12/2026;Navidad\n08/12/2026;Inmaculada\nmañana\n");

    ServiceData result = importer.importDates(FileUtil.fileDataToString(file));

    assertThat(result.getType()).isEqualTo(AnswerType.WARNING);
    assertThat(result.getTitle()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_TITLE");
    assertThat(result.getMessage()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_INVALID[2, 4]");
    List<Map<String, CellData>> rows = result.getDataList().getRows();
    assertThat(rows.stream().map(row -> row.get("Dat").getStringValue()).toList())
      .containsExactly("08/12/2026", "25/12/2026");
    assertThat(rows.stream().map(row -> row.get("Nam").getStringValue()).toList())
      .containsExactly("Inmaculada", "Navidad");
    verify(fileService).deleteFile(file);
  }

  @Test
  void eachDateIsAddedAsANewRowOfTheDatesGridAndADateWithoutNameIsAHoliday() throws Exception {
    FileData file = stage("holidays.csv", "25/12/2026;Navidad\n08/12/2026\n");

    ServiceData result = importer.importDates(FileUtil.fileDataToString(file));

    // The rows of the grid are not saved: the grid adds them as new rows (no id, the grid gives them one), in order,
    // with the columns of the grid: the date as the grid shows it (dd/MM/yyyy), its weekday (the locale of the task
    // catalog, from Sunday 1 to Saturday 7: 08/12/2026 is a Tuesday, 25/12/2026 a Friday) and its name
    List<String> actions = result.getClientActionList().stream()
      .map(action -> action.getType() + ":" + action.getTarget() + ":" + action.getParameters().get("row"))
      .toList();
    assertThat(actions).containsExactly(
      "add-row:GrdDatLst:{Dat=08/12/2026, DatWek=SCREEN_TEXT_TASK_CATALOG_WEEKDAY_3, "
        + "Nam=SCHEDULER_CALENDAR_IMPORT_DEFAULT_NAME}",
      "add-row:GrdDatLst:{Dat=25/12/2026, DatWek=SCREEN_TEXT_TASK_CATALOG_WEEKDAY_6, Nam=Navidad}");
    assertThat(result.getDataList().getRows().get(0).get("Nam").getStringValue())
      .isEqualTo("SCHEDULER_CALENDAR_IMPORT_DEFAULT_NAME");
  }

  @Test
  void aFileOutOfTheUploadAreaIsRefusedAndNeverDeleted() throws Exception {
    FileData file = stage("holidays.csv", "25/12/2026");
    given(fileService.isPathWithinUploadArea(any())).willReturn(false);

    String staged = FileUtil.fileDataToString(file);
    AWException refusal = assertThrows(AWException.class, () -> importer.importDates(staged));

    assertThat(refusal.getTitle()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_ERROR_TITLE");
    assertThat(refusal.getMessage()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_INVALID_FILE");
    verify(fileService, never()).deleteFile(any(FileData.class));
  }

  @Test
  void aFileLargerThanTheBoundIsRefusedAndDeleted() throws Exception {
    FileData file = stage("holidays.csv", "25/12/2026").setFileSize(CalendarDatesImporter.MOST_BYTES + 1);

    String staged = FileUtil.fileDataToString(file);
    AWException refusal = assertThrows(AWException.class, () -> importer.importDates(staged));

    assertThat(refusal.getMessage()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_TOO_LARGE");
    verify(fileService).deleteFile(file);
  }

  @Test
  void aFileOfAnotherFormatIsRefused() throws Exception {
    FileData file = stage("holidays.xlsx", "25/12/2026");

    String staged = FileUtil.fileDataToString(file);
    AWException refusal = assertThrows(AWException.class, () -> importer.importDates(staged));

    assertThat(refusal.getMessage()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_UNSUPPORTED");
  }

  @Test
  void aFileWithAByteOrderMarkIsReadInItsEncodingAndAFileThatIsNotUtf8IsReadAsWindows1252() throws Exception {
    byte[] utf16 = ("﻿25/12/2026;Navidad día").getBytes(StandardCharsets.UTF_16LE);
    byte[] windows = "25/12/2026;Navidad día".getBytes(Charset.forName("windows-1252"));

    for (byte[] bytes : List.of(utf16, windows)) {
      FileData file = stage("holidays.csv", bytes);

      ServiceData result = importer.importDates(FileUtil.fileDataToString(file));

      assertThat(result.getType()).isEqualTo(AnswerType.OK);
      assertThat(result.getDataList().getRows().get(0).get("Nam").getStringValue()).isEqualTo("Navidad día");
    }
  }

  @Test
  void aStagedFileWhosePathLeavesItsFolderIsRefusedAndNeverReadNorDeleted() throws Exception {
    given(fileService.getFullPath(any(), anyBoolean())).willReturn(folder.resolve("area") + "/");
    Files.createDirectories(folder.resolve("area"));
    Files.writeString(folder.resolve("outside.csv"), "25/12/2026");
    FileData file = new FileData("../outside.csv", 10L, "text/plain").setBasePath("upload/");

    String staged = FileUtil.fileDataToString(file);
    AWException refusal = assertThrows(AWException.class, () -> importer.importDates(staged));

    assertThat(refusal.getMessage()).isEqualTo("SCHEDULER_CALENDAR_IMPORT_INVALID_FILE");
    verify(fileService, never()).deleteFile(any(FileData.class));
  }

  private FileData stage(String name, String content) throws Exception {
    return stage(name, content.getBytes(StandardCharsets.UTF_8));
  }

  private FileData stage(String name, byte[] bytes) throws Exception {
    Files.write(folder.resolve(name), bytes);
    return new FileData(name, (long) bytes.length, "text/plain").setBasePath("upload/");
  }
}
