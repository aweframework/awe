package com.almis.awe.tools.service;

import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.service.data.builder.DataListBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqlExtractorServiceTest {

  private static final String USER_HOME = "user.home";

  @TempDir
  Path userHome;

  private String previousUserHome;
  private DataListBuilder builder;
  private SqlExtractorService service;

  @BeforeEach
  void setUp() {
    previousUserHome = System.getProperty(USER_HOME);
    System.setProperty(USER_HOME, userHome.toString());
    builder = mock(DataListBuilder.class, RETURNS_SELF);
    service = new SqlExtractorService(builder, mock(DataSource.class));
  }

  @AfterEach
  void tearDown() {
    System.setProperty(USER_HOME, previousUserHome);
  }

  @Test
  void getSelectFileReturnsNoFilesWhenTheSelectsFolderCannotBeListed() throws Exception {
    // A regular file where the selects folder should be: it exists, but File#listFiles returns null
    Files.createFile(userHome.resolve("aweFiles"));
    DataList dataList = new DataList();
    when(builder.build()).thenReturn(dataList);

    ServiceData serviceData = service.getSelectFile();

    verify(builder).addColumn(eq("label"), eq(List.of()), anyString());
    verify(builder).addColumn(eq("value"), eq(List.of()), anyString());
    assertThat(serviceData.getDataList()).isSameAs(dataList);
  }

  @Test
  void getSelectFileListsTheSavedSelects() throws Exception {
    Path selects = Files.createDirectory(userHome.resolve("aweFiles"));
    Files.writeString(selects.resolve("users.sql"), "select * from users");
    when(builder.build()).thenReturn(new DataList());

    service.getSelectFile();

    verify(builder).addColumn(eq("label"), eq(List.of("users.sql")), any());
  }
}
