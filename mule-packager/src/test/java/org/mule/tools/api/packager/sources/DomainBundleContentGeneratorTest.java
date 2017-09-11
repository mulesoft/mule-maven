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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.tools.api.packager.ProjectInformation;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class DomainBundleContentGeneratorTest {

  private DomainBundleContentGenerator generator;

  @Rule
  public TemporaryFolder projectBaseFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder buidlDirectory = new TemporaryFolder();
  private ProjectInformation projectInformation;

  @Before
  public void setUp() throws IOException {
    projectInformation = mock(ProjectInformation.class);
    projectBaseFolder.create();
    buidlDirectory.create();
    when(projectInformation.getProjectBaseFolder()).thenReturn(projectBaseFolder.getRoot().toPath());
    when(projectInformation.getBuildDirectory()).thenReturn(buidlDirectory.getRoot().toPath());
    generator = new DomainBundleContentGenerator(projectInformation);
  }

  @Test
  public void createContentTest() throws IOException {
    DomainBundleContentGenerator generatorSpy = spy(generator);
    doNothing().when(generatorSpy).createMavenDescriptors();
    generatorSpy.createContent();
    verify(generatorSpy, times(1)).createMavenDescriptors();
  }
}
