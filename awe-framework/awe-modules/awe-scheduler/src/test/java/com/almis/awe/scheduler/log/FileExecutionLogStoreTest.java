package com.almis.awe.scheduler.log;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.service.EncodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FileExecutionLogStore}: on-disk execution logs, read by task and execution id through the
 * scheduler {@code get-execution-log} action, so no file path ever reaches the client.
 */
class FileExecutionLogStoreTest {

  @TempDir
  Path logDirectory;

  @Test
  void locatorNodeEmitsTheExecutionKeyAndNoFilePath() throws Exception {
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    ObjectNode node = store.locatorNode(new ExecutionKey(12, 34));

    assertEquals("12-34", node.get("value").asString());
    assertFalse(node.toString().contains(logDirectory.toString()));
    assertFalse(node.toString().contains(EncodeService.encodeSymmetric(logDirectory.resolve("execution_12-34.log").toString())));
    assertEquals("no-btn", node.get("style").asString());
    assertEquals("SCHEDULER_SHOW_EXECUTION_LOG", node.get("title").asString());
    assertEquals("file-text-o text-info", node.get("icon").asString());
    assertEquals("", node.get("label").asString());
  }

  @Test
  void applyViewerSelectionPointsTheViewerAtTheSchedulerActionByTaskAndExecutionId() {
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());
    ServiceData serviceData = new ServiceData();

    store.applyViewerSelection("5-9", new ExecutionKey(5, 9), serviceData);

    List<ClientAction> actions = serviceData.getClientActionList();
    assertEquals(5, actions.size());
    assertEquals("reset", actions.get(0).getType());
    assertEquals("executionLogViewer", actions.get(0).getTarget());
    assertEquals("update-controller", actions.get(1).getType());
    assertEquals("executionLogViewer", actions.get(1).getTarget());
    assertEquals("serverAction", parameter(actions.get(1), "attribute"));
    assertEquals("get-execution-log", parameter(actions.get(1), "value"));
    assertEquals("select", actions.get(2).getType());
    assertEquals("execution-log-task-id", actions.get(2).getTarget());
    assertArrayEquals(new Object[]{5}, (Object[]) actions.get(2).getParameters().get("values"));
    assertEquals("select", actions.get(3).getType());
    assertEquals("execution-log-execution-id", actions.get(3).getTarget());
    assertArrayEquals(new Object[]{9}, (Object[]) actions.get(3).getParameters().get("values"));
    assertEquals("filter", actions.get(4).getType());
    assertEquals("executionLogViewer", actions.get(4).getTarget());
    assertTrue(actions.stream().noneMatch(action -> "path".equals(action.getTarget())));
  }

  @Test
  void readReturnsLinesFromOffsetMatchingTheCurrentFileServiceLoop() throws Exception {
    Files.write(logDirectory.resolve("execution_5-9.log"), List.of("line0", "line1", "line2", "line3"));
    FileExecutionLogStore store = new FileExecutionLogStore(logDirectory.toString());

    ExecutionLogPage page = store.read(new ExecutionKey(5, 9), 2, null);

    assertEquals(List.of("line2", "line3"), page.lines());
    assertFalse(page.replace());
    assertEquals(0L, page.omittedLines());
  }

  @Test
  void readFromZeroOffsetReturnsEveryLine() throws Exception {
    Files.write(logDirectory.resolve("execution_5-9.log"), List.of("a", "b"));
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

  private static Object parameter(ClientAction action, String name) {
    return action.getParameters().get(name);
  }

  @Test
  void readErrorDoesNotEchoThePath(@TempDir Path directory) throws Exception {
    // A directory with the name of the log file: reading it fails
    Files.createDirectory(directory.resolve("execution_1-2.log"));
    FileExecutionLogStore store = new FileExecutionLogStore(directory.toString());

    AWException exception = assertThrows(AWException.class, () -> store.read(new ExecutionKey(1, 2), 0, null));

    assertEquals("Error reading the log of task 1 and execution 2", exception.getMessage());
    assertFalse(exception.getMessage().contains(directory.toString()));
  }
}
