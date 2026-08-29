package com.almis.awe.scheduler.log;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.EncodeService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden/parity tests for {@link FileExecutionLogStore}, a pure extraction of the on-disk
 * execution log logic previously embedded in {@code TaskDAO}.
 */
class FileExecutionLogStoreTest {

  @TempDir
  Path logDirectory;

  @Test
  void locatorNodeEmitsTheEncodedAbsolutePathWithTheExistingNodeShape() throws Exception {
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());
    Path expectedPath = logDirectory.resolve("execution_12-34.log");

    ObjectNode node = store.locatorNode(new ExecutionKey(12, 34));

    assertEquals(EncodeService.encodeSymmetric(expectedPath.toString()), node.get("value").asText());
    assertEquals("no-btn", node.get("style").asText());
    assertEquals("SCHEDULER_SHOW_EXECUTION_LOG", node.get("title").asText());
    assertEquals("file-text-o text-info", node.get("icon").asText());
    assertEquals("", node.get("label").asText());
  }

  @Test
  void locatorNodeUsesTheRequestedExecutionInThePath() throws Exception {
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());
    Path expectedPath = logDirectory.resolve("execution_1-2.log");

    ObjectNode node = store.locatorNode(new ExecutionKey(1, 2));

    assertEquals(EncodeService.encodeSymmetric(expectedPath.toString()), node.get("value").asText());
  }

  @Test
  void applyViewerSelectionEmitsResetSelectAndFilterInOrder() throws Exception {
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());
    ServiceData serviceData = new ServiceData();

    store.applyViewerSelection("encoded-path", new ExecutionKey(1, 1), serviceData);

    assertEquals(3, serviceData.getClientActionList().size());
    assertEquals("reset", serviceData.getClientActionList().get(0).getType());
    assertEquals("executionLogViewer", serviceData.getClientActionList().get(0).getTarget());
    assertEquals("select", serviceData.getClientActionList().get(1).getType());
    assertEquals("path", serviceData.getClientActionList().get(1).getTarget());
    assertEquals("filter", serviceData.getClientActionList().get(2).getType());
    assertEquals("executionLogViewer", serviceData.getClientActionList().get(2).getTarget());
  }

  @Test
  void readReturnsLinesFromOffsetMatchingTheCurrentFileServiceLoop() throws Exception {
    Files.write(logDirectory.resolve("execution_5-9.log"), List.of("line0", "line1", "line2", "line3"), StandardCharsets.UTF_8);
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    ExecutionLogPage page = store.read(new ExecutionKey(5, 9), 2, null);

    assertEquals(List.of("line2", "line3"), page.lines());
    assertFalse(page.replace());
    assertEquals(0L, page.omittedLines());
  }

  @Test
  void readFromZeroOffsetReturnsEveryLine() throws Exception {
    Files.write(logDirectory.resolve("execution_5-9.log"), List.of("a", "b"), StandardCharsets.UTF_8);
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    ExecutionLogPage page = store.read(new ExecutionKey(5, 9), 0, null);

    assertEquals(List.of("a", "b"), page.lines());
  }

  @Test
  void readMissingFileReturnsEmptyPage() throws Exception {
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    ExecutionLogPage page = store.read(new ExecutionKey(99, 1), 0, null);

    assertEquals(ExecutionLogPage.empty(), page);
  }

  @Test
  void purgeDeletesOnlyTheTargetedFiles() throws Exception {
    Path keep = Files.createFile(logDirectory.resolve("execution_1-1.log"));
    Path delete = Files.createFile(logDirectory.resolve("execution_1-2.log"));
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    store.purge(1, List.of(2));

    assertTrue(Files.exists(keep));
    assertFalse(Files.exists(delete));
  }

  @Test
  void purgeOrphansKeepsValidFilesAndDeletesOrphans() throws Exception {
    Path valid = Files.createFile(logDirectory.resolve("execution_1-1.log"));
    Path orphan = Files.createFile(logDirectory.resolve("execution_1-2.log"));
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    store.purgeOrphans(Set.of(new ExecutionKey(1, 1)));

    assertTrue(Files.exists(valid));
    assertFalse(Files.exists(orphan));
  }
}
