package com.almis.awe.tools.service;

import com.almis.awe.config.BaseConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileManagerServiceTest {

  @Mock
  BaseConfigProperties.Filemanager filemanager;
  @InjectMocks
  private FileManagerService fileManagerService;
  @Mock
  private BaseConfigProperties baseConfigProperties;
  @TempDir
  Path baseDir;

  @Test
  void afterPropertiesSet() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn(baseDir.toString());
    fileManagerService.afterPropertiesSet();
    verify(filemanager, times(2)).getDateFormat();
  }

  @Test
  void afterPropertiesSetBadDirectory() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn("");
    assertThrows(IllegalArgumentException.class, () -> fileManagerService.afterPropertiesSet());
    verify(filemanager, times(0)).getDateFormat();
  }

  @Test
  void afterPropertiesSetBadDateFormat() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn(baseDir.toString());
    when(filemanager.getDateFormat()).thenReturn("");
    fileManagerService.afterPropertiesSet();
    verify(filemanager, times(3)).getDateFormat();
  }

  @Test
  void afterPropertiesSetGoodDateFormat() {
    when(baseConfigProperties.getFilemanager()).thenReturn(filemanager);
    when(filemanager.getBasePath()).thenReturn(baseDir.toString());
    when(filemanager.getDateFormat()).thenReturn("dd/MM/yyyy");
    fileManagerService.afterPropertiesSet();
    verify(filemanager, times(2)).getDateFormat();
  }

  @Test
  void listReturnsFilesAndFoldersOfPath() throws IOException {
    writeFile("docs/a.txt", "hello");
    Files.createDirectories(baseDir.resolve("docs/sub"));

    JsonNode response = serviceOnBaseDir().fileOperation(action("list").put("path", "docs"));

    List<JsonNode> elements = new ArrayList<>();
    response.get("result").forEach(elements::add);
    assertThat(elements).extracting(element -> element.get("name").asString())
      .containsExactlyInAnyOrder("a.txt", "sub");
    JsonNode file = elements.stream().filter(element -> "a.txt".equals(element.get("name").asString())).findFirst().orElseThrow();
    assertThat(file.get("type").asString()).isEqualTo("file");
    assertThat(file.get("size").asLong()).isEqualTo(5L);
    assertThat(file.get("date").asString()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    JsonNode folder = elements.stream().filter(element -> "sub".equals(element.get("name").asString())).findFirst().orElseThrow();
    assertThat(folder.get("type").asString()).isEqualTo("dir");
    if (isPosix()) {
      assertThat(file.get("rights").asString()).matches("[r-][w-][x-][r-][w-][x-][r-][w-][x-]");
    }
  }

  @Test
  void listMissingPathReturnsError() {
    JsonNode response = serviceOnBaseDir().fileOperation(action("list").put("path", "missing"));

    assertError(response, "NoSuchFileException");
  }

  @Test
  void unknownActionReturnsError() {
    JsonNode response = serviceOnBaseDir().fileOperation(action("unknown"));

    assertError(response, "IllegalArgumentException");
  }

  @Test
  void renameMovesItemToNewName() throws IOException {
    writeFile("a.txt", "content");

    JsonNode response = serviceOnBaseDir().fileOperation(action("rename").put("item", "a.txt").put("newItemPath", "b.txt"));

    assertSuccess(response);
    assertThat(baseDir.resolve("a.txt")).doesNotExist();
    assertThat(baseDir.resolve("b.txt")).hasContent("content");
  }

  @Test
  void renameMissingItemReturnsError() {
    JsonNode response = serviceOnBaseDir().fileOperation(action("rename").put("item", "missing.txt").put("newItemPath", "b.txt"));

    assertError(response, "NoSuchFileException");
  }

  @Test
  void moveMovesEveryItemIntoNewPath() throws IOException {
    writeFile("a.txt", "a");
    writeFile("b.txt", "b");
    Files.createDirectories(baseDir.resolve("target"));
    ObjectNode params = action("move").put("newPath", "target");
    params.set("items", items("a.txt", "b.txt"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertSuccess(response);
    assertThat(baseDir.resolve("a.txt")).doesNotExist();
    assertThat(baseDir.resolve("b.txt")).doesNotExist();
    assertThat(baseDir.resolve("target/a.txt")).hasContent("a");
    assertThat(baseDir.resolve("target/b.txt")).hasContent("b");
  }

  @Test
  void copySingleItemUsesSingleFilename() throws IOException {
    writeFile("a.txt", "a");
    Files.createDirectories(baseDir.resolve("target"));
    ObjectNode params = action("copy").put("newPath", "target").put("singleFilename", "copy.txt");
    params.set("items", items("a.txt"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertSuccess(response);
    assertThat(baseDir.resolve("a.txt")).hasContent("a");
    assertThat(baseDir.resolve("target/copy.txt")).hasContent("a");
  }

  @Test
  void copyMultipleItemsKeepsFileNames() throws IOException {
    writeFile("a.txt", "a");
    writeFile("b.txt", "b");
    Files.createDirectories(baseDir.resolve("target"));
    ObjectNode params = action("copy").put("newPath", "target");
    params.set("items", items("a.txt", "b.txt"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertSuccess(response);
    assertThat(baseDir.resolve("a.txt")).hasContent("a");
    assertThat(baseDir.resolve("b.txt")).hasContent("b");
    assertThat(baseDir.resolve("target/a.txt")).hasContent("a");
    assertThat(baseDir.resolve("target/b.txt")).hasContent("b");
  }

  @Test
  void copyToMissingFolderReturnsError() throws IOException {
    writeFile("a.txt", "a");
    ObjectNode params = action("copy").put("newPath", "missing");
    params.set("items", items("a.txt"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertError(response, "NoSuchFileException");
  }

  @Test
  void removeDeletesFilesAndFolderTrees() throws IOException {
    writeFile("a.txt", "a");
    writeFile("folder/nested/b.txt", "b");
    ObjectNode params = action("remove");
    params.set("items", items("a.txt", "folder"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertSuccess(response);
    assertThat(baseDir.resolve("a.txt")).doesNotExist();
    assertThat(baseDir.resolve("folder")).doesNotExist();
  }

  @Test
  void removeMissingItemReturnsError() {
    ObjectNode params = action("remove");
    params.set("items", items("missing.txt"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertError(response, "NoSuchFileException");
  }

  @Test
  void editWritesContentAndGetContentReadsIt() {
    FileManagerService service = serviceOnBaseDir();

    JsonNode editResponse = service.fileOperation(action("edit").put("item", "notes.txt").put("content", "Some notes"));
    JsonNode contentResponse = service.fileOperation(action("getContent").put("item", "notes.txt"));

    assertSuccess(editResponse);
    assertThat(baseDir.resolve("notes.txt")).hasContent("Some notes");
    assertThat(contentResponse.get("result").asString()).isEqualTo("Some notes");
  }

  @Test
  void getContentOfMissingFileReturnsError() {
    JsonNode response = serviceOnBaseDir().fileOperation(action("getContent").put("item", "missing.txt"));

    assertError(response, "FileNotFoundException");
  }

  @Test
  void createFolderCreatesItOnlyOnce() {
    FileManagerService service = serviceOnBaseDir();

    JsonNode firstResponse = service.fileOperation(action("createFolder").put("newPath", "newFolder"));
    JsonNode secondResponse = service.fileOperation(action("createFolder").put("newPath", "newFolder"));

    assertSuccess(firstResponse);
    assertThat(baseDir.resolve("newFolder")).isDirectory();
    assertError(secondResponse, "Already exist");
  }

  @Test
  void compressZipsItemsIntoDestination() throws IOException {
    writeFile("a.txt", "a");
    writeFile("b.txt", "b");
    ObjectNode params = action("compress").put("compressedFilename", "archive.zip").put("destination", "");
    params.set("items", items("a.txt", "b.txt"));

    JsonNode response = serviceOnBaseDir().fileOperation(params);

    assertSuccess(response);
    assertThat(zipEntryNames(baseDir.resolve("archive.zip"))).contains("a.txt", "b.txt");
  }

  @Test
  void extractUnzipsItemIntoDestination() throws IOException {
    writeFile("a.txt", "a");
    FileManagerService service = serviceOnBaseDir();
    ObjectNode compressParams = action("compress").put("compressedFilename", "archive.zip").put("destination", "");
    compressParams.set("items", items("a.txt"));
    assertSuccess(service.fileOperation(compressParams));

    JsonNode response = service.fileOperation(action("extract").put("item", "archive.zip").put("destination", "extracted"));

    assertSuccess(response);
    assertThat(baseDir.resolve("extracted/a.txt")).hasContent("a");
  }

  @Test
  void downloadFileResolvesPathInsideBasePath() throws IOException {
    writeFile("docs/a.txt", "a");

    File file = serviceOnBaseDir().downloadFile("docs/a.txt", "false");

    assertThat(file).hasContent("a");
  }

  @Test
  void downloadAsZipFileZipsItemsIntoTempPath() throws IOException {
    writeFile("a.txt", "a");
    writeFile("b.txt", "b");
    Path tempDir = Files.createDirectories(baseDir.resolve("tmp"));

    File zipFile = serviceOnBaseDir(tempDir).downloadAsZipFile(new String[] { "bundle.zip" }, new String[] { "a.txt", "b.txt" });

    assertThat(zipFile.toPath()).isEqualTo(tempDir.resolve("bundle.zip"));
    assertThat(zipEntryNames(zipFile.toPath())).contains("a.txt", "b.txt");
  }

  @Test
  void uploadFileWritesFilesIntoDestination() throws IOException {
    Files.createDirectories(baseDir.resolve("uploads"));
    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("upload.txt");
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream("uploaded".getBytes(StandardCharsets.UTF_8)));

    JsonNode response = serviceOnBaseDir().uploadFile("uploads", List.of(file));

    assertSuccess(response);
    assertThat(baseDir.resolve("uploads/upload.txt")).hasContent("uploaded");
  }

  @Test
  void uploadFileWithoutFilesReturnsError() {
    JsonNode response = serviceOnBaseDir().uploadFile("uploads", Collections.emptyList());

    assertError(response, "file size = 0");
  }

  private FileManagerService serviceOnBaseDir() {
    return serviceOnBaseDir(baseDir);
  }

  private FileManagerService serviceOnBaseDir(Path tempDir) {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getFilemanager().setBasePath(baseDir.toString());
    properties.getFilemanager().setDateFormat("yyyy-MM-dd HH:mm:ss");
    properties.getPaths().setTemp(tempDir.toString());
    FileManagerService service = new FileManagerService(properties);
    service.afterPropertiesSet();
    return service;
  }

  private ObjectNode action(String action) {
    return JsonNodeFactory.instance.objectNode().put("action", action);
  }

  private ArrayNode items(String... items) {
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    for (String item : items) {
      array.add(item);
    }
    return array;
  }

  private void writeFile(String relativePath, String content) throws IOException {
    Path path = baseDir.resolve(relativePath);
    Files.createDirectories(path.getParent());
    Files.writeString(path, content);
  }

  private List<String> zipEntryNames(Path zipPath) throws IOException {
    try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
      return zipFile.stream().map(ZipEntry::getName).toList();
    }
  }

  private boolean isPosix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  private void assertSuccess(JsonNode response) {
    assertThat(response.get("result").get("success").asBoolean()).as(response.toString()).isTrue();
  }

  private void assertError(JsonNode response, String expectedMessage) {
    assertThat(response.get("result").get("success").asBoolean()).isFalse();
    assertThat(response.get("result").get("error").asString()).contains(expectedMessage);
  }
}
