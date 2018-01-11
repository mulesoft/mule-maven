/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.validation.resolver;

import org.junit.Before;
import org.junit.Test;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;
import org.mule.tools.api.exception.ProjectBuildingException;
import org.mule.tools.api.exception.ValidationException;
import org.mule.tools.api.util.Project;
import org.mule.tools.api.util.ProjectBuilder;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static util.ResolverTestHelper.*;

public class MulePluginResolverTest {

  private MulePluginResolver resolver;
  private ProjectBuilder projectBuilderMock;
  private ArtifactCoordinates dependency;
  private Project projectMock;

  @Before
  public void before() throws ProjectBuildingException {
    projectBuilderMock = mock(ProjectBuilder.class);
    projectMock = mock(Project.class);
    resolver = new MulePluginResolver(projectBuilderMock, projectMock);
    dependency = createDependencyWithClassifierAndScope(MULE_PLUGIN_CLASSIFIER, COMPILE_SCOPE);
    Map<ArtifactCoordinates, List<ArtifactCoordinates>> projectStructure = createMainResolvableProjectDependencyTree(dependency);
    setUpProjectBuilderMock(projectStructure, projectBuilderMock);
    when(projectMock.getDependencies()).thenReturn(projectStructure.get(dependency));
  }

  @Test
  public void resolveMulePluginsTest() throws ProjectBuildingException, ValidationException {
    List<ArtifactCoordinates> actualResolvedMulePlugins = resolver.resolve();

    assertThat("Number of resolved mule plugins is not the expected", actualResolvedMulePlugins.size(), equalTo(5));
    assertThat("Not all resolved dependencies are mule plugins", actualResolvedMulePlugins.stream()
        .allMatch(dependency -> dependency.getClassifier().equals(MULE_PLUGIN_CLASSIFIER)), is(true));
  }
}
