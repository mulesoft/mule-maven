/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.api.packager.sources;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.mule.tools.api.packager.ProjectInformation;

/**
 * Generates the required content for each of the mandatory folders of a mule package
 */
public abstract class ContentGenerator {

  protected final ProjectInformation projectInformation;

  public ContentGenerator(ProjectInformation projectInformation) {
    checkArgument(projectInformation.getProjectBaseFolder().toFile().exists(), "Project base folder should exist");
    checkArgument(projectInformation.getBuildDirectory().toFile().exists(), "Project build folder should exist");
    this.projectInformation = projectInformation;
  }

  /**
   * It create all the package content in the required folders
   *
   * @throws IOException
   */
  public abstract void createContent() throws IOException;

  public static void checkPathExist(Path path) {
    checkArgument(path.toFile().exists(), "The path: " + path.toString() + " should exist");
  }

  public static void copyFile(Path originPath, Path destinationPath, String destinationFileName) throws IOException {
    checkPathExist(originPath);
    checkPathExist(destinationPath);
    Files.copy(originPath, destinationPath.resolve(destinationFileName), StandardCopyOption.REPLACE_EXISTING);
  }


}
