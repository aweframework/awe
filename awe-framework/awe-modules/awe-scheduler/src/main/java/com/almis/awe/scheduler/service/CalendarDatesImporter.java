package com.almis.awe.scheduler.service;

import com.almis.awe.builder.client.grid.AddRowActionBuilder;
import com.almis.awe.builder.enumerates.RowPosition;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.FileData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.file.FileUtil;
import com.almis.awe.scheduler.util.CalendarDatesImport;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.FileService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * «Importar…» of the calendar side panel (#993): read the dates of an .ics or a .csv file staged by the uploader of the
 * screen (the /file/upload stage-then-claim model) and return them for the dates grid (Dat dd/MM/yyyy, Nam) without
 * saving them: they are saved with the calendar. Each date comes as an add-row action of the dates grid of the panel
 * ({@value #DATES_GRID}) with the columns of the grid (Dat, its weekday DatWek as the saved dates show it, Nam), at the
 * end of the grid (after its selected row, when one is), in order, a new row with no id (the grid gives it one) that the grid sends as an insert when the
 * calendar is saved; a date without a name is a holiday (the grid requires a name). The staged file is read once, at most {@value #MOST_BYTES} bytes, in
 * the encoding its byte order mark tells (UTF-8 or UTF-16), as UTF-8 when it has none, or as windows-1252 when it is
 * not valid UTF-8, and deleted; a file out of the upload area, or whose path leaves its folder, is refused and never
 * touched. The messages are generic and the lines that
 * cannot be read are told by their number, never the content of the file
 */
@Slf4j
public class CalendarDatesImporter {

  /**
   * Most bytes of a file
   */
  static final long MOST_BYTES = 1024L * 1024L;
  /**
   * Most line numbers told in the message
   */
  private static final int MOST_TOLD_LINES = 20;
  /**
   * Dates grid of the calendar side panel
   */
  static final String DATES_GRID = "GrdDatLst";
  private static final String IMPORT = "SCHEDULER_CALENDAR_IMPORT_";
  private static final String ERROR_TITLE = IMPORT + "ERROR_TITLE";
  private static final String TOO_LARGE = "TOO_LARGE";
  private static final String INVALID_FILE = "INVALID_FILE";
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

  private final FileService fileService;
  private final TaskWhenSummary.Texts texts;

  /**
   * Constructor
   *
   * @param fileService Files staged by the uploader
   * @param texts       Locales with their parameters
   */
  public CalendarDatesImporter(FileService fileService, TaskWhenSummary.Texts texts) {
    this.fileService = fileService;
    this.texts = texts;
  }

  /**
   * Read the dates of a staged file
   *
   * @param fileToken Value of the uploader: the staged file
   * @return The dates, in order and each one once (Dat, Nam), OK, or WARNING when some lines cannot be read or there is
   * no date
   * @throws AWException The file is not a staged file, cannot be read, is too large or is neither .ics nor .csv
   */
  public ServiceData importDates(String fileToken) throws AWException {
    FileData file = stagedFile(fileToken);
    Path path = stagedPath(file);
    CalendarDatesImport.Result result;
    try {
      result = CalendarDatesImport.parse(file.getFileName(), read(file, path));
    } finally {
      delete(file);
    }
    if (result.unsupported()) {
      throw refusal("UNSUPPORTED");
    } else if (result.tooLarge()) {
      throw refusal(TOO_LARGE);
    }
    DataList dataList = new DataList();
    ServiceData serviceData = new ServiceData();
    int id = 1;
    String holiday = texts.text(IMPORT + "DEFAULT_NAME");
    for (CalendarDatesImport.Day day : result.dates()) {
      String date = DATE.format(day.date());
      String weekday = texts.text(CalendarDatesImport.weekdayKey(day.date()));
      String name = day.name() == null || day.name().isBlank() ? holiday : day.name();
      Map<String, CellData> row = new HashMap<>();
      row.put("id", new CellData(id++));
      row.put("Dat", new CellData(date));
      row.put("DatWek", new CellData(weekday));
      row.put("Nam", new CellData(name));
      dataList.addRow(row);
      Map<String, Object> gridRow = new LinkedHashMap<>();
      gridRow.put("Dat", date);
      gridRow.put("DatWek", weekday);
      gridRow.put("Nam", name);
      serviceData.addClientAction(new AddRowActionBuilder(RowPosition.BOTTOM, DATES_GRID, gridRow).setSilent(true)
        .build());
    }
    dataList.setRecords(dataList.getRows().size());
    String count = String.valueOf(result.dates().size());
    serviceData.setDataList(dataList).setTitle(texts.text(IMPORT + "TITLE"));
    if (!result.invalidLines().isEmpty()) {
      return serviceData.setType(AnswerType.WARNING).setMessage(texts.text(IMPORT + "INVALID", count,
        lines(result.invalidLines())));
    } else if (result.dates().isEmpty()) {
      return serviceData.setType(AnswerType.WARNING).setMessage(texts.text(IMPORT + "EMPTY"));
    }
    return serviceData.setType(AnswerType.OK).setMessage(texts.text(IMPORT + "OK", count));
  }

  /**
   * The staged file of the uploader, only when it is in the upload area
   */
  private FileData stagedFile(String fileToken) throws AWException {
    FileData file;
    try {
      file = fileToken == null || fileToken.isBlank() ? null : FileUtil.stringToFileData(fileToken);
    } catch (AWException | RuntimeException exc) {
      log.warn("[CALENDAR IMPORT] The file to import is not a staged file");
      file = null;
    }
    if (file == null || file.getFileName() == null || !fileService.isPathWithinUploadArea(file)) {
      throw refusal(INVALID_FILE);
    }
    return file;
  }

  /**
   * Path of the staged file, refused (and never read nor deleted) when it is not inside the upload area once normalized
   */
  private Path stagedPath(FileData file) throws AWException {
    Path area = Path.of(fileService.getFullPath(new FileData().setBasePath(file.getBasePath()), false))
      .toAbsolutePath().normalize();
    Path path = Path.of(fileService.getFullPath(file, false) + file.getFileName()).toAbsolutePath().normalize();
    if (!path.startsWith(area) || path.equals(area)) {
      log.warn("[CALENDAR IMPORT] The file to import is out of the upload area");
      throw refusal(INVALID_FILE);
    }
    return path;
  }

  /**
   * Content of the staged file, refused when it is larger than the bound
   */
  private String read(FileData file, Path path) throws AWException {
    if (file.getFileSize() != null && file.getFileSize() > MOST_BYTES) {
      throw refusal(TOO_LARGE);
    }
    byte[] bytes;
    try (InputStream input = Files.newInputStream(path)) {
      bytes = input.readNBytes((int) MOST_BYTES + 1);
    } catch (IOException exc) {
      log.warn("[CALENDAR IMPORT] The staged file cannot be read", exc);
      throw refusal(INVALID_FILE);
    }
    if (bytes.length > MOST_BYTES) {
      throw refusal(TOO_LARGE);
    }
    return decode(bytes);
  }

  /**
   * Text of the bytes: in the encoding of its byte order mark (UTF-8, UTF-16 big or little endian), as UTF-8 when it
   * has none, or as windows-1252 when they are not valid UTF-8
   */
  private static String decode(byte[] bytes) {
    if (startsWith(bytes, 0xEF, 0xBB, 0xBF)) {
      return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    } else if (startsWith(bytes, 0xFE, 0xFF)) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
    } else if (startsWith(bytes, 0xFF, 0xFE)) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
    }
    try {
      return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException exc) {
      return new String(bytes, WINDOWS_1252);
    }
  }

  private static boolean startsWith(byte[] bytes, int... mark) {
    if (bytes.length < mark.length) {
      return false;
    }
    for (int index = 0; index < mark.length; index++) {
      if ((bytes[index] & 0xFF) != mark[index]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Delete the staged file once read: it is never kept
   */
  private void delete(FileData file) {
    try {
      fileService.deleteFile(file);
    } catch (AWException | RuntimeException exc) {
      log.warn("[CALENDAR IMPORT] The staged file could not be deleted");
    }
  }

  /**
   * Line numbers of the message: the first ones, and «…» when there are more
   */
  private static String lines(List<Integer> lines) {
    String told = lines.stream().limit(MOST_TOLD_LINES).map(String::valueOf).collect(Collectors.joining(", "));
    return lines.size() > MOST_TOLD_LINES ? told + ", …" : told;
  }

  private AWException refusal(String reason) {
    return new AWException(texts.text(ERROR_TITLE), texts.text(IMPORT + reason));
  }
}
