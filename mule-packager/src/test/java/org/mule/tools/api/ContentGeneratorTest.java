/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.tools.api.packager.structure.FolderNames.CLASSES;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.api.packager.PackagerTestUtils;
import org.mule.tools.api.packager.ProjectInformation;
import org.mule.tools.api.packager.packaging.PackagingType;
import org.mule.tools.api.packager.sources.MuleContentGenerator;

public class ContentGeneratorTest {

  protected static final String GROUP_ID = "org.mule.munit";
  protected static final String ARTIFACT_ID = "fake-id";
  protected static final String VERSION = "1.0.0-SNAPSHOT";

  private static final String POM_FILE_NAME = "pom.xml";
  private static final String FAKE_FILE_NAME = "fakeFile.xml";
  private static final String MULE_ARTIFACT_DESCRIPTOR_FILE_NAME = "mule-artifact.json";
  private static final String TYPE = "jar";
  private static final String CLASSIFIER = "classifier";
  private static final String CLASSLOADER_MODEL_JSON_FILE_NAME = "classloader-model.json";

  @Rule
  public TemporaryFolder projectBaseFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder projectTargetFolder = new TemporaryFolder();

  private PackagingType packagingType = PackagingType.MULE_APPLICATION;

  private MuleContentGenerator contentGenerator;

  @Before
  public void setUp() {
    ProjectInformation info = new ProjectInformation.Builder()
        .withGroupId(GROUP_ID)
        .withArtifactId(ARTIFACT_ID)
        .withVersion(VERSION)
        .withPackaging(packagingType.toString())
        .withProjectBaseFolder(projectBaseFolder.getRoot().toPath())
        .withBuildDirectory(projectTargetFolder.getRoot().toPath()).build();
    contentGenerator = new MuleContentGenerator(info);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failCreationProjectBaseFolderNonExistent() {
    ProjectInformation info = new ProjectInformation.Builder()
        .withGroupId(GROUP_ID)
        .withArtifactId(ARTIFACT_ID)
        .withVersion(VERSION)
        .withPackaging(packagingType.toString())
        .withProjectBaseFolder(Paths.get("/fake/project/base/folder"))
        .withBuildDirectory(projectTargetFolder.getRoot().toPath()).build();
    new MuleContentGenerator(info);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failCreationProjectTargetFolderNonExistent() {
    ProjectInformation info = new ProjectInformation.Builder()
        .withGroupId(GROUP_ID)
        .withArtifactId(ARTIFACT_ID)
        .withVersion(VERSION)
        .withPackaging(packagingType.toString())
        .withProjectBaseFolder(projectBaseFolder.getRoot().toPath())
        .withBuildDirectory(Paths.get("/fake/project/base/folder")).build();
    new MuleContentGenerator(info);
  }

  @Test(expected = IllegalArgumentException.class)
  public void createSrcFolderContentNonExistingSourceFolder() throws IOException {
    String destinationFolderName = packagingType.getSourceFolderName();

    Path destinationFolderPath = projectTargetFolder.getRoot().toPath().resolve(destinationFolderName);
    PackagerTestUtils.createEmptyFolder(destinationFolderPath);

    contentGenerator.createMuleSrcFolderContent();
  }

  @Test
  public void createSrcFolderContent() throws IOException {
    String sourceFolderName = packagingType.getSourceFolderName();
    String destinationFolderName = CLASSES.value();


    Path sourceFolderPath = projectBaseFolder.getRoot().toPath().resolve(PackagerTestUtils.SRC)
        .resolve(PackagerTestUtils.MAIN).resolve(sourceFolderName);

    PackagerTestUtils.createFolder(sourceFolderPath, FAKE_FILE_NAME, true);

    Path destinationFolderPath = projectTargetFolder.getRoot().toPath().resolve(destinationFolderName);
    PackagerTestUtils.createEmptyFolder(destinationFolderPath);

    contentGenerator.createMuleSrcFolderContent();

    PackagerTestUtils.assertFileExists(destinationFolderPath.resolve(FAKE_FILE_NAME));
  }

  @Test
  public void createTestFolderContentNonExistingSourceFolder() throws IOException {
    String destinationFolderName = packagingType.getTestFolderName();

    Path destinationFolderPath = projectTargetFolder.getRoot().toPath().resolve(
                                                                                PackagerTestUtils.TEST_MULE)
        .resolve(destinationFolderName);
    PackagerTestUtils.createEmptyFolder(destinationFolderPath);

    contentGenerator.createTestFolderContent();
    PackagerTestUtils.assertFileDoesNotExists(destinationFolderPath.resolve(FAKE_FILE_NAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createTestFolderContentNonExistingDestinationFolder() throws IOException {
    String sourceFolderName = packagingType.getTestFolderName();

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath().resolve(PackagerTestUtils.SRC).resolve(
                                                                                                        PackagerTestUtils.TEST)
        .resolve(sourceFolderName);
    PackagerTestUtils.createFolder(sourceFolderPath, FAKE_FILE_NAME, true);

    contentGenerator.createTestFolderContent();
  }

  @Test
  public void createTestFolderContent() throws IOException {
    String sourceFolderName = packagingType.getTestFolderName();
    String destinationFolderName = packagingType.getTestFolderName();

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath().resolve(PackagerTestUtils.SRC).resolve(
                                                                                                        PackagerTestUtils.TEST)
        .resolve(sourceFolderName);
    PackagerTestUtils.createFolder(sourceFolderPath, FAKE_FILE_NAME, true);

    Path destinationFolderPath = projectTargetFolder.getRoot().toPath().resolve(
                                                                                PackagerTestUtils.TEST_MULE)
        .resolve(destinationFolderName);
    PackagerTestUtils.createEmptyFolder(destinationFolderPath);

    contentGenerator.createTestFolderContent();

    PackagerTestUtils.assertFileExists(destinationFolderPath.resolve(FAKE_FILE_NAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createMetaInfMuleSourceFolderContentNonExistingDestinationFolder() throws IOException {
    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, FAKE_FILE_NAME, true);

    contentGenerator.createMetaInfMuleSourceFolderContent();
  }

  @Test
  public void createMetaInfMuleSourceFolderContent() throws IOException {
    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, FAKE_FILE_NAME, true);

    Path destinationFolderPath = projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                                                    PackagerTestUtils.MULE_SRC)
        .resolve(ARTIFACT_ID);
    PackagerTestUtils.createEmptyFolder(destinationFolderPath);

    contentGenerator.createMetaInfMuleSourceFolderContent();
    PackagerTestUtils.assertFileExists(destinationFolderPath.resolve(FAKE_FILE_NAME));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createDescriptorsNoOriginalPom() throws IOException {
    String descriptorFileName = MULE_ARTIFACT_DESCRIPTOR_FILE_NAME;

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, descriptorFileName, true);

    contentGenerator.createDescriptors();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createDescriptorsNoPomDestinationFolder() throws IOException {

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, POM_FILE_NAME, true);

    contentGenerator.createDescriptors();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createDescriptorsNoOriginalDescriptor() throws IOException {
    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, POM_FILE_NAME, true);

    Path pomPropertiesDestinationPath =
        projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                           PackagerTestUtils.MAVEN)
            .resolve(GROUP_ID).resolve(ARTIFACT_ID);
    PackagerTestUtils.createEmptyFolder(pomPropertiesDestinationPath);

    contentGenerator.createDescriptors();
  }

  @Test(expected = IllegalArgumentException.class)
  public void createDescriptorsNoDescriptorDestinationFolder() throws IOException {
    String descriptorFileName = MULE_ARTIFACT_DESCRIPTOR_FILE_NAME;

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, POM_FILE_NAME, true);
    PackagerTestUtils.createFolder(sourceFolderPath, descriptorFileName, true);

    Path pomPropertiesDestinationPath =
        projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                           PackagerTestUtils.MAVEN)
            .resolve(GROUP_ID).resolve(ARTIFACT_ID);
    PackagerTestUtils.createEmptyFolder(pomPropertiesDestinationPath);

    contentGenerator.createDescriptors();
  }

  @Test
  public void createDescriptors() throws IOException {
    String descriptorFileName = MULE_ARTIFACT_DESCRIPTOR_FILE_NAME;

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, POM_FILE_NAME, true);
    PackagerTestUtils.createFolder(sourceFolderPath, descriptorFileName, true);

    Path pomPropertiesDestinationPath =
        projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                           PackagerTestUtils.MAVEN)
            .resolve(GROUP_ID).resolve(ARTIFACT_ID);
    PackagerTestUtils.createEmptyFolder(pomPropertiesDestinationPath);

    Path descriptorDestinationPath = projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                                                        PackagerTestUtils.MULE_ARTIFACT);
    PackagerTestUtils.createEmptyFolder(descriptorDestinationPath);

    contentGenerator.createDescriptors();

    PackagerTestUtils.assertFileExists(pomPropertiesDestinationPath.resolve(POM_FILE_NAME));
    PackagerTestUtils.assertFileExists(pomPropertiesDestinationPath.resolve(PackagerTestUtils.POM_PROPERTIES));
    PackagerTestUtils.assertFileExists(descriptorDestinationPath.resolve(descriptorFileName));
  }

  @Test
  public void createDescriptorsPolicy() throws IOException {
    ProjectInformation info = new ProjectInformation.Builder()
        .withGroupId(GROUP_ID)
        .withArtifactId(ARTIFACT_ID)
        .withVersion(VERSION)
        .withPackaging(PackagingType.MULE_POLICY.toString())
        .withProjectBaseFolder(projectBaseFolder.getRoot().toPath())
        .withBuildDirectory(projectTargetFolder.getRoot().toPath()).build();
    contentGenerator = new MuleContentGenerator(info);

    String descriptorFileName = MULE_ARTIFACT_DESCRIPTOR_FILE_NAME;

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, POM_FILE_NAME, true);
    PackagerTestUtils.createFolder(sourceFolderPath, descriptorFileName, true);

    Path pomPropertiesDestinationPath =
        projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                           PackagerTestUtils.MAVEN)
            .resolve(GROUP_ID).resolve(ARTIFACT_ID);
    PackagerTestUtils.createEmptyFolder(pomPropertiesDestinationPath);

    Path descriptorDestinationPath = projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                                                        PackagerTestUtils.MULE_ARTIFACT);
    PackagerTestUtils.createEmptyFolder(descriptorDestinationPath);

    contentGenerator.createDescriptors();

    PackagerTestUtils.assertFileExists(pomPropertiesDestinationPath.resolve(POM_FILE_NAME));
    PackagerTestUtils.assertFileExists(pomPropertiesDestinationPath.resolve(PackagerTestUtils.POM_PROPERTIES));
    PackagerTestUtils.assertFileExists(descriptorDestinationPath.resolve(descriptorFileName));
  }

  @Test
  public void createDescriptorsMuleDomain() throws IOException {
    ProjectInformation info = new ProjectInformation.Builder()
        .withGroupId(GROUP_ID)
        .withArtifactId(ARTIFACT_ID)
        .withVersion(VERSION)
        .withPackaging(PackagingType.MULE_DOMAIN.toString())
        .withProjectBaseFolder(projectBaseFolder.getRoot().toPath())
        .withBuildDirectory(projectTargetFolder.getRoot().toPath()).build();
    contentGenerator = new MuleContentGenerator(info);

    String descriptorFileName = MULE_ARTIFACT_DESCRIPTOR_FILE_NAME;

    Path sourceFolderPath = projectBaseFolder.getRoot().toPath();
    PackagerTestUtils.createFolder(sourceFolderPath, POM_FILE_NAME, true);
    PackagerTestUtils.createFolder(sourceFolderPath, descriptorFileName, true);

    Path pomPropertiesDestinationPath =
        projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                           PackagerTestUtils.MAVEN)
            .resolve(GROUP_ID).resolve(ARTIFACT_ID);
    PackagerTestUtils.createEmptyFolder(pomPropertiesDestinationPath);

    Path descriptorDestinationPath = projectTargetFolder.getRoot().toPath().resolve(PackagerTestUtils.META_INF).resolve(
                                                                                                                        PackagerTestUtils.MULE_ARTIFACT);
    PackagerTestUtils.createEmptyFolder(descriptorDestinationPath);

    contentGenerator.createDescriptors();

    PackagerTestUtils.assertFileExists(pomPropertiesDestinationPath.resolve(POM_FILE_NAME));
    PackagerTestUtils.assertFileExists(pomPropertiesDestinationPath.resolve(PackagerTestUtils.POM_PROPERTIES));
    PackagerTestUtils.assertFileExists(descriptorDestinationPath.resolve(descriptorFileName));
  }

  @Test
  public void createContent() throws IOException {
    MuleContentGenerator contentGeneratorMock = mock(MuleContentGenerator.class);

    doNothing().when(contentGeneratorMock).createMuleSrcFolderContent();
    doNothing().when(contentGeneratorMock).createMetaInfMuleSourceFolderContent();
    doNothing().when(contentGeneratorMock).createDescriptors();

    doCallRealMethod().when(contentGeneratorMock).createContent();
    contentGeneratorMock.createContent();

    verify(contentGeneratorMock, times(1)).createContent();
    verify(contentGeneratorMock, times(1)).createMetaInfMuleSourceFolderContent();
    verify(contentGeneratorMock, times(1)).createDescriptors();
  }

  @Test
  public void classLoaderModelSerializationTest() throws URISyntaxException {
    ArtifactCoordinates artifactCoordinates = new ArtifactCoordinates(GROUP_ID, ARTIFACT_ID, VERSION, TYPE, CLASSIFIER);
    ClassLoaderModel expectedClassLoaderModel = new ClassLoaderModel(VERSION, artifactCoordinates);
    List<Artifact> dependencies = getDependencies();
    expectedClassLoaderModel.setDependencies(dependencies);
    File classloaderModelJsonFile =
        MuleContentGenerator.createClassLoaderModelJsonFile(expectedClassLoaderModel, projectTargetFolder.getRoot());
    assertThat("Classloader model json file name is incorrect",
               classloaderModelJsonFile.getName().endsWith(CLASSLOADER_MODEL_JSON_FILE_NAME), is(true));
    ClassLoaderModel actualClassloaderModel = MuleContentGenerator.createClassLoaderModelFromJson(classloaderModelJsonFile);
    assertThat("Actual classloader model is not equal to the expected", actualClassloaderModel,
               equalTo(expectedClassLoaderModel));
  }

  private List<Artifact> getDependencies() throws URISyntaxException {
    List<Artifact> artifacts = new ArrayList<>();
    for (int i = 0; i < 10; ++i) {
      artifacts.add(createArtifact(i));
    }
    return artifacts;
  }

  private Artifact createArtifact(int i) throws URISyntaxException {
    ArtifactCoordinates coordinates = new ArtifactCoordinates(VERSION, GROUP_ID + i, ARTIFACT_ID + i, TYPE, CLASSIFIER);
    URI uri = new URI("file:/repository/path/" + i);
    return new Artifact(coordinates, uri);
  }
}
