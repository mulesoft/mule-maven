/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.packager;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectFoldersGeneratorFactoryTest {

  @Test
  public void createDomainBundleProjectFoldersGeneratorTest() {
    ProjectInformation projectInformation = mock(ProjectInformation.class);
    when(projectInformation.getGroupId()).thenReturn("group.id");
    when(projectInformation.getArtifactId()).thenReturn("artifact-id");
    when(projectInformation.getPackaging()).thenReturn("mule-application");
    assertThat("The project folder generator type is not the expected", ProjectFoldersGeneratorFactory.create(projectInformation),
               instanceOf(MuleProjectFoldersGenerator.class));
  }
}
