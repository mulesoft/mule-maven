/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.packager.sources;

import static java.lang.Boolean.FALSE;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.deserialize;
import static org.mule.tools.api.classloader.ClassLoaderModelJsonSerializer.serializeToFile;
import static org.mule.tools.api.packager.sources.DefaultValuesMuleArtifactJsonGenerator.generate;
import static org.mule.tools.api.packager.structure.FolderNames.CLASSES;
import static org.mule.tools.api.packager.structure.FolderNames.META_INF;
import static org.mule.tools.api.packager.structure.FolderNames.MULE_ARTIFACT;
import static org.mule.tools.api.packager.structure.FolderNames.MULE_SRC;
import static org.mule.tools.api.packager.structure.FolderNames.TARGET;
import static org.mule.tools.api.packager.structure.FolderNames.TEST_MULE;
import static org.mule.tools.api.packager.structure.PackagerFiles.MULE_ARTIFACT_JSON;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.api.packager.ProjectInformation;
import org.mule.tools.api.packager.packaging.PackagingType;
import org.mule.tools.api.packager.structure.ProjectStructure;
import org.mule.tools.api.util.CopyFileVisitor;
import org.mule.tools.api.util.exclude.MuleExclusionMatcher;

/**
 * Generates the required content for each of the mandatory folders of a mule application package
 */
public class MuleContentGenerator extends ContentGenerator {

  private MuleArtifactContentResolver muleArtifactContentResolver;

  public MuleContentGenerator(ProjectInformation projectInformation) {
    super(projectInformation);
  }

  /**
   * It creates all the package content in the required folders
   * 
   * @throws IOException
   */
  @Override
  public void createContent() throws IOException {
    createMetaInfMuleSourceFolderContent();
    createDescriptors();
  }

  /**
   * It creates the content that contains the productive Mule source code. It leaves it in the classes folder
   *
   * @throws IOException
   */
  public void createMuleSrcFolderContent() throws IOException {
    Path originPath = PackagingType.fromString(projectInformation.getPackaging())
        .getSourceFolderLocation(projectInformation.getProjectBaseFolder());
    Path destinationPath = projectInformation.getBuildDirectory().resolve(CLASSES.value());

    copyContent(originPath, destinationPath, Optional.ofNullable(null), true, false);
  }

  /**
   * It creates the content that contains the test Mule source code. The name of the folder depends on the {@link PackagingType}
   * 
   * @throws IOException
   */
  public void createTestFolderContent() throws IOException {
    Path originPath = PackagingType.fromString(projectInformation.getPackaging())
        .getTestSourceFolderLocation(projectInformation.getProjectBaseFolder());
    Path destinationPath = projectInformation.getBuildDirectory().resolve(TEST_MULE.value()).resolve(originPath.getFileName());

    copyContent(originPath, destinationPath, Optional.ofNullable(null), false, true);
  }

  /**
   * It creates the {@link org.mule.tools.api.packager.structure.FolderNames#MULE_SRC} folder used by IDEs to import the project
   * source code
   * 
   * @throws IOException
   */
  public void createMetaInfMuleSourceFolderContent() throws IOException {
    Path originPath = projectInformation.getProjectBaseFolder();
    Path destinationPath = projectInformation.getBuildDirectory().resolve(META_INF.value()).resolve(MULE_SRC.value())
        .resolve(projectInformation.getArtifactId());

    List<Path> exclusions = new ArrayList<>();
    exclusions.add(projectInformation.getProjectBaseFolder().resolve(TARGET.value()));

    copyContent(originPath, destinationPath, Optional.of(exclusions), true, true, true, true);
  }

  /**
   * It creates classloader-model.json in META-INF/mule-artifact
   *
   * @param classLoaderModel the classloader model of the application being packaged
   */
  public void createApplicationClassLoaderModelJsonFile(ClassLoaderModel classLoaderModel) {
    File destinationFolder =
        projectInformation.getBuildDirectory().resolve(META_INF.value()).resolve(MULE_ARTIFACT.value()).toFile();
    createClassLoaderModelJsonFile(classLoaderModel, destinationFolder);
  }

  /**
   * Creates a {@link ClassLoaderModel} from the JSON representation
   *
   * @param classLoaderModelDescriptor file containing the classloader model in JSON format
   * @return a non null {@link ClassLoaderModel} matching the provided JSON content
   */
  public static ClassLoaderModel createClassLoaderModelFromJson(File classLoaderModelDescriptor) {
    return deserialize(classLoaderModelDescriptor);
  }

  private void copyContent(Path originPath, Path destinationPath, Optional<List<Path>> exclusions) throws IOException {
    copyContent(originPath, destinationPath, exclusions, true, true);
  }

  private void copyContent(Path originPath, Path destinationPath, Optional<List<Path>> exclusions, Boolean validateOrigin,
                           Boolean validateDestination)
      throws IOException {
    copyContent(originPath, destinationPath, exclusions, validateOrigin, validateDestination, FALSE, FALSE);
  }

  private void copyContent(Path originPath, Path destinationPath, Optional<List<Path>> exclusions, Boolean validateOrigin,
                           Boolean validateDestination, Boolean ignoreHiddenFiles, Boolean ignoreHiddenFolders)
      throws IOException {
    if (validateOrigin) {
      checkPathExist(originPath);
    }
    if (validateDestination) {
      checkPathExist(destinationPath);
    }


    CopyFileVisitor visitor =
        new CopyFileVisitor(originPath.toFile(), destinationPath.toFile(), ignoreHiddenFiles, ignoreHiddenFolders,
                            new MuleExclusionMatcher(projectInformation.getProjectBaseFolder()));
    exclusions.ifPresent(e -> visitor.setExclusions(e));

    Files.walkFileTree(originPath, visitor);
  }

  /**
   * It creates classloader-model.json in the destination folder
   *
   * @param classLoaderModel the classloader model of the application being packaged
   * @return the created File containing the classloader model's JSON representation
   */
  public static File createClassLoaderModelJsonFile(ClassLoaderModel classLoaderModel, File destinationFolder) {
    return serializeToFile(classLoaderModel, destinationFolder);
  }

  /**
   * It creates the descriptors files, pom.xml, pom.properties, and the mule-*.json file. The name of the the last one depends on
   * the {@link PackagingType}
   *
   * @throws IOException
   */
  public void createDescriptors() throws IOException {
    createMavenDescriptors();
    copyDescriptorFile();
  }

  private void copyDescriptorFile() throws IOException {
    Path originPath = projectInformation.getProjectBaseFolder().resolve(MULE_ARTIFACT_JSON);
    Path destinationPath = projectInformation.getBuildDirectory().resolve(META_INF.value()).resolve(MULE_ARTIFACT.value());
    String destinationFileName = originPath.getFileName().toString();
    copyFile(originPath, destinationPath, destinationFileName);
    generate(getMuleArtifactContentResolver());
  }

  public MuleArtifactContentResolver getMuleArtifactContentResolver() {
    if (muleArtifactContentResolver == null) {
      ProjectStructure projectStructure =
          new ProjectStructure(projectInformation.getProjectBaseFolder(), projectInformation.getBuildDirectory(),
                               projectInformation.isTestProject());
      muleArtifactContentResolver = new MuleArtifactContentResolver(projectStructure);
    }
    return muleArtifactContentResolver;
  }
}
