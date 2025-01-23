/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.client.standalone.controller;

import org.apache.commons.io.file.Counters;
import org.junit.jupiter.api.Test;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mule.tools.client.standalone.exception.MuleControllerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class ControllerTest {

  @TempDir
  public File temporaryFolder;

  @Test
  public void testController() throws IOException {
    temporaryFolder.toPath().resolve("conf").toFile().mkdirs();
    temporaryFolder.toPath().resolve("lib").resolve("user").toFile().mkdirs();
    temporaryFolder.toPath().resolve("conf").resolve("wrapper.conf").toFile().createNewFile();
    temporaryFolder.toPath().resolve("mule-app").toFile().createNewFile();
    temporaryFolder.toPath().resolve("mule-invalid-app").toFile().createNewFile();
    temporaryFolder.toPath().resolve("lib.jar").toFile().createNewFile();
    BufferedWriter writer =
        new BufferedWriter(new FileWriter(temporaryFolder.toPath().resolve("conf").resolve("wrapper.conf").toFile()));
    writer.write("wrapper.java.additional.19=-Danypoint.platform.proxy_port=80");
    writer.close();
    AbstractOSController abstractOSController = new WindowsController(temporaryFolder.getAbsolutePath(), 20000);
    Controller controller = new Controller(abstractOSController, temporaryFolder.getAbsolutePath());
    controller.addConfProperty("2");
    controller.deploy(temporaryFolder.toPath().resolve("mule-app").toString());
    controller.addLibrary(temporaryFolder.toPath().resolve("lib.jar").toFile());
    controller.deployDomain(temporaryFolder.toPath().resolve("mule-app").toString());
    controller.deployDomain(temporaryFolder.toPath().resolve("conf").toFile().getAbsolutePath().toString());

    File muleInvalidAppFile = temporaryFolder.toPath().resolve("mule-invalid-app").toFile();
    muleInvalidAppFile.setReadable(false);
    muleInvalidAppFile.setWritable(false);
    assertThrows(MuleControllerException.class, () -> {
      controller.deployDomain(muleInvalidAppFile.getAbsolutePath());
    });
  }

  private static class XController extends Controller {

    public XController(AbstractOSController osSpecificController, String muleHome) {
      super(osSpecificController, muleHome);
      this.libsDir = mock(File.class);
    }

    void resetController() {
      reset(this.libsDir);
    }
  }

  private final AbstractOSController osController = mock(AbstractOSController.class);
  private final XController controller = new XController(osController, UUID.randomUUID().toString());

  @BeforeEach
  void setUp() {
    reset(osController);
    controller.resetController();
  }

  @Test
  void getMuleBinTest() {
    String value = UUID.randomUUID().toString();

    when(osController.getMuleBin()).thenReturn(value);

    assertThat(controller.getMuleBin()).isEqualTo(value);
  }

  @Test
  void startTest() {
    // VALUES
    String value = UUID.randomUUID().toString();

    //CONFIG
    ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
    doNothing().when(osController).start(args.capture());

    //EXEC
    controller.start(value);

    //VALIDATION
    verify(osController, times(1)).start(any());
    assertThat(args.getValue()).hasSize(1).containsExactly(value);
  }

  @Test
  void restartTest() {
    String value = UUID.randomUUID().toString();

    ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
    doNothing().when(osController).restart(args.capture());

    controller.restart(value);

    verify(osController, times(1)).restart(any());
    assertThat(args.getValue()).hasSize(1).containsExactly(value);
  }

  @Test
  void statusTest() {
    // VALUES
    String value = UUID.randomUUID().toString();

    //CONFIG
    ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
    doNothing().when(osController).start(args.capture());
    when(osController.status(any())).thenReturn(0);

    controller.start(value);
    controller.status(value);

    //VALIDATION
    verify(osController, times(1)).status(any());
    assertThat(args.getValue()).hasSize(1).containsExactly(value);
  }

  @Test
  void getProcessIdTest() {
    // VALUES
    String value = UUID.randomUUID().toString();

    //CONFIG
    ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
    doNothing().when(osController).start(args.capture());
    when(osController.getProcessId()).thenReturn(0);

    controller.start(value);
    controller.getProcessId();

    //VALIDATION
    verify(osController, times(1)).getProcessId();
    assertThat(args.getValue()).hasSize(1).containsExactly(value);
  }

  @ParameterizedTest
  @MethodSource("addLibraryTestValues")
  void addLibraryTest(File jar, String message, boolean canWrite) {
    when(controller.libsDir.canWrite()).thenReturn(canWrite);

    if (!canWrite) {
      assertThatThrownBy(() -> controller.addLibrary(jar))
          .isInstanceOf(MuleControllerException.class)
          .hasMessageContaining(message);
    } else {
      try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
        fileUtils.when(() -> FileUtils.copyFileToDirectory(any(), any())).then(invocationOnMock -> null);
        controller.addLibrary(jar);

        fileUtils.verify(() -> FileUtils.copyFileToDirectory(any(), any()));

        fileUtils.when(() -> FileUtils.copyFileToDirectory(any(), any())).thenThrow(new IOException());
        assertThatThrownBy(() -> controller.addLibrary(jar))
            .isInstanceOf(MuleControllerException.class);
      }
    }
  }

  static Stream<Arguments> addLibraryTestValues() {
    File notFound = mock(File.class);
    when(notFound.exists()).thenReturn(false);

    File notExt = mock(File.class);
    when(notExt.exists()).thenReturn(true);
    when(notExt.getAbsolutePath()).thenReturn("anyName.txt");

    File readOnly = mock(File.class);
    when(readOnly.exists()).thenReturn(true);
    when(readOnly.getAbsolutePath()).thenReturn("library.jar");
    when(readOnly.canRead()).thenReturn(false);

    File jarFile = mock(File.class);
    when(jarFile.exists()).thenReturn(true);
    when(jarFile.getAbsolutePath()).thenReturn("library.jar");
    when(jarFile.canRead()).thenReturn(true);

    return Stream.of(
                     Arguments.of(notFound, "Jar file does not exist", false),
                     Arguments.of(notExt, "don't have .jar extension.", false),
                     Arguments.of(readOnly, "Cannot read jar file", false),
                     Arguments.of(jarFile, "Cannot write on lib dir", false),
                     Arguments.of(jarFile, null, true));
  }

  @Test
  void stopAnddeleteAnchorsTest() {
    temporaryFolder.toPath().resolve("conf").toFile();
    File arg2Dir = new File(temporaryFolder, "arg2");
    arg2Dir.mkdirs();

    File appsDir = new File(temporaryFolder, "apps");
    appsDir.mkdirs();
    File anchorFile = new File(temporaryFolder, "anchor.txt");
    XController controller = new XController(osController, temporaryFolder.getAbsolutePath());

    when(osController.stop(any(String[].class))).thenReturn(0);

    int result = controller.stop("arg1Dir", "arg2Dir");

    assertEquals(0, result);
    assertFalse(anchorFile.exists(), "Anchor file should have been deleted by deleteAnchors");
  }



}
