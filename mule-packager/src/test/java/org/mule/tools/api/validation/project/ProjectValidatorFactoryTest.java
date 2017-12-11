/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.api.validation.project;

import org.junit.Before;
import org.junit.Test;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.tools.api.packager.ProjectInformation;
import org.mule.tools.api.packager.packaging.PackagingType;
import org.mule.tools.api.validation.project.AbstractProjectValidator;
import org.mule.tools.api.validation.project.DomainBundleProjectValidator;
import org.mule.tools.api.validation.project.MuleProjectValidator;
import org.mule.tools.api.validation.project.ProjectValidatorFactory;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectValidatorFactoryTest {

  private boolean strictCheck;
  private ArrayList sharedLibraries;

  private AetherMavenClient aetherMavenClientMock;
  private ProjectInformation projectInformationMock;

  @Before
  public void setUp() {
    strictCheck = false;
    sharedLibraries = new ArrayList<>();

    projectInformationMock = mock(ProjectInformation.class);
    aetherMavenClientMock = mock(AetherMavenClient.class);
  }

  @Test
  public void createDomainBundleProjectValidatorTest() {
    when(projectInformationMock.getPackaging()).thenReturn(PackagingType.MULE_DOMAIN_BUNDLE.toString());

    AbstractProjectValidator actualProjectValidator =
        ProjectValidatorFactory.create(projectInformationMock, aetherMavenClientMock, sharedLibraries, strictCheck);

    assertThat("Project validator type is not the expected", actualProjectValidator,
               instanceOf(DomainBundleProjectValidator.class));
  }

  @Test
  public void createMuleProjectValidatorMuleApplicationPackagingTest() {
    when(projectInformationMock.getPackaging()).thenReturn(PackagingType.MULE_APPLICATION.toString());

    AbstractProjectValidator actualProjectValidator = ProjectValidatorFactory
        .create(projectInformationMock, aetherMavenClientMock, sharedLibraries, strictCheck);

    assertThat("Project validator type is not the expected", actualProjectValidator, instanceOf(MuleProjectValidator.class));
  }

  @Test
  public void createMuleProjectValidatorMuleDomainPackagingTest() {
    when(projectInformationMock.getPackaging()).thenReturn(PackagingType.MULE_DOMAIN.toString());

    AbstractProjectValidator actualProjectValidator = ProjectValidatorFactory
        .create(projectInformationMock, aetherMavenClientMock, sharedLibraries, strictCheck);

    assertThat("Project validator type is not the expected", actualProjectValidator, instanceOf(MuleProjectValidator.class));
  }

  @Test
  public void createMuleProjectValidatorMulePolicyPackagingTest() {
    when(projectInformationMock.getPackaging()).thenReturn(PackagingType.MULE_POLICY.toString());

    AbstractProjectValidator actualProjectValidator = ProjectValidatorFactory
        .create(projectInformationMock, aetherMavenClientMock, sharedLibraries, strictCheck);

    assertThat("Project validator type is not the expected", actualProjectValidator, instanceOf(MuleProjectValidator.class));
  }
}
